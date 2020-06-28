package me.jfenn.metronome.activities

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.*
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.*
import android.preference.PreferenceManager
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.material.button.MaterialButton
import me.jfenn.androidutils.autoSystemUiColors
import me.jfenn.androidutils.bind
import me.jfenn.androidutils.dpToPx
import me.jfenn.metronome.BuildConfig
import me.jfenn.metronome.Metronome
import me.jfenn.metronome.R
import me.jfenn.metronome.billing.Billing
import me.jfenn.metronome.services.MetronomeService
import me.jfenn.metronome.services.MetronomeService.LocalBinder
import me.jfenn.metronome.services.MetronomeService.TickListener
import me.jfenn.metronome.utils.PREF_BOOKMARKS
import me.jfenn.metronome.utils.PreferenceDelegate
import me.jfenn.metronome.utils.getThemedColor
import me.jfenn.metronome.views.EmphasisSwitch
import me.jfenn.metronome.views.MetronomeView
import me.jfenn.metronome.views.SeekBar
import me.jfenn.metronome.views.SeekBar.OnProgressChangeListener
import me.jfenn.metronome.views.TicksView
import me.jfenn.metronome.views.TicksView.OnTickChangedListener
import java.lang.ref.WeakReference
import java.util.*

class MainActivity : AppCompatActivity(), OnTickChangedListener, ServiceConnection, TickListener, EmphasisSwitch.OnCheckedChangeListener, OnProgressChangeListener {

    private var service: MetronomeService? = null

    private val iconView: View? by bind(R.id.icon)
    private val metronomeView: MetronomeView? by bind(R.id.metronome)
    private val playView: ImageView? by bind(R.id.play)
    private val emphasisLayout: LinearLayout? by bind(R.id.emphasis)
    private val bookmarkLayout: LinearLayout? by bind(R.id.bookmarks)
    private val bpmView: TextView? by bind(R.id.bpm)
    private val bpmTitleView: TextView? by bind(R.id.bpm_title)
    private val bookmarkView: MaterialButton? by bind(R.id.button_bookmark)
    private val touchView: MaterialButton? by bind(R.id.button_touch)
    private val lessView: ImageView? by bind(R.id.bpm_decrease)
    private val moreView: ImageView? by bind(R.id.bpm_increase)
    private val muchLessView: ImageView? by bind(R.id.bpm_mega_decrease)
    private val muchMoreView: ImageView? by bind(R.id.bpm_mega_increase)
    private val addEmphasisView: ImageView? by bind(R.id.emphasis_add)
    private val removeEmphasisView: ImageView? by bind(R.id.emphasis_remove)
    private val ticksView: TicksView? by bind(R.id.ticks)
    private val seekBar: SeekBar? by bind(R.id.seekbar)

    private val textColorAccent by lazy { getThemedColor(R.attr.textColorAccent) }
    private val textColorPrimary by lazy { getThemedColor(android.R.attr.textColorPrimary) }

    private val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    private var bookmarks: MutableList<Int> by PreferenceDelegate(PREF_BOOKMARKS, mutableListOf(80, 120, 180))

    private var isPlaying: Boolean = false
    private var prevTouchInterval: Long = 0
    private var prevTouchTime: Long = 0
    private val metronome: Metronome get() = applicationContext as Metronome

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.autoSystemUiColors()
        metronome.onCreateActivity()

        // max tempo = 300 BPM
        seekBar?.setMaxProgress(300)
        bindToService()

        updateBookmarks(true)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playView?.setOnClickListener { _ ->
            service?.apply {
                if (isPlaying)
                    pause()
                else play()
            }
        }

        bookmarkView?.setOnClickListener { _ ->
            service?.bpm?.let { bpm ->
                if (bookmarks.contains(bpm))
                    removeBookmark(bpm)
                else {
                    metronome.onPremium(this@MainActivity)
                    addBookmark(bpm)
                }
            }
        }

        touchView?.setOnClickListener { _ ->
            if (prevTouchTime > 0) {
                val interval = System.currentTimeMillis() - prevTouchTime
                if (interval > 200) {
                    prevTouchInterval = if (interval < 20000) {
                        if (prevTouchInterval == -1L) interval else (prevTouchInterval + interval) / 2
                    } else -1
                }

                seekBar?.setProgress((60000 / prevTouchInterval).toInt())
            }
            prevTouchTime = System.currentTimeMillis()
        }

        addEmphasisView?.setOnClickListener  { _ ->
            service?.let {
                if (it.emphasisList.size < 20) {
                    val emphasisList = it.emphasisList
                    emphasisList.add(false)
                    it.emphasisList = emphasisList

                    emphasisLayout?.addView(getEmphasisSwitch(false))
                }
            }
        }

        removeEmphasisView?.setOnClickListener { _ ->
            service?.let {
                if (it.emphasisList.size > 2) {
                    // update service list / prefs
                    val emphasisList: MutableList<Boolean> = it.emphasisList
                    val position = emphasisList.size - 1
                    emphasisList.removeAt(position)
                    it.emphasisList = emphasisList

                    // remove item from view
                    emphasisLayout?.removeViewAt(position)
                }
            }
        }

        moreView?.setOnClickListener { _ ->
            service?.bpm?.plus(1)?.coerceIn(BPM_RANGE)?.let {
                seekBar?.setProgress(it)
            }
        }

        muchMoreView?.setOnClickListener { _ ->
            service?.bpm?.plus(10)?.coerceIn(BPM_RANGE)?.let {
                seekBar?.setProgress(it)
            }
        }

        lessView?.setOnClickListener { _ ->
            service?.bpm?.minus(1)?.coerceIn(BPM_RANGE)?.let {
                seekBar?.setProgress(it)
            }
        }

        muchLessView?.setOnClickListener { _ ->
            service?.bpm?.minus(10)?.coerceIn(BPM_RANGE)?.let {
                seekBar?.setProgress(it)
            }
        }

        seekBar?.setOnProgressChangeListener(this)
        ticksView?.setListener(this)
        savedInstanceState?.run {
            iconView?.visibility = View.GONE
        } ?: run {
            SplashThread(this).start()
        }
    }

    private fun bindToService() {
        service?.let {
            ticksView?.setTick(it.tick)
            seekBar?.setProgress(it.bpm)
            bindTempo()
            bindPlayPause()
            emphasisLayout?.apply {
                removeAllViews()
                for (isEmphasis in it.emphasisList) {
                    addView(getEmphasisSwitch(isEmphasis))
                }
            }
        }
    }

    private fun bindTempo() = service?.let {
        metronomeView?.setInterval(it.interval)
        bpmView?.text = String.format(Locale.getDefault(), getString(R.string.bpm), it.bpm.toString())
        bpmTitleView?.text = when {
            it.bpm <= 24 -> "Larghissimo"
            it.bpm <= 45 -> "Grave"
            it.bpm <= 60 -> "Largo"
            it.bpm <= 66 -> "Larghetto"
            it.bpm <= 76 -> "Adagio"
            it.bpm <= 100 -> "Andante"
            it.bpm <= 112 -> "Moderato"
            it.bpm <= 120 -> "Allegro moderato"
            it.bpm <= 156 -> "Allegro"
            it.bpm <= 176 -> "Vivace"
            it.bpm <= 200 -> "Presto"
            else -> "Prestissimo"
        }
    }

    private fun bindPlayPause() = service?.let { service ->
        if (service.isPlaying != isPlaying) {
            AnimatedVectorDrawableCompat.create(
                    this,
                    if (service.isPlaying) R.drawable.ic_play_to_pause else R.drawable.ic_pause_to_play
            )?.also {
                playView?.setImageDrawable(it)
                it.start()
            } ?: run {
                playView?.setImageResource(
                        if (service.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                )
            }

            isPlaying = service.isPlaying
        }
    }

    private fun addBookmark(bpm: Int) = bookmarks.let {
        if (!it.contains(bpm)) {
            it.add(bpm)
            it.sort()
            bookmarks = it

            updateBookmarks(true)
        }
    }

    private fun removeBookmark(bpm: Int) = bookmarks.let {
        if (it.contains(bpm)) {
            it.remove(bpm)
            bookmarks = it

            updateBookmarks(true)
        }
    }

    private fun updateBookmarks(contentChanged: Boolean) = bookmarks.let { bookmarks ->
        if (contentChanged) {
            service?.let {
                // call bpm listener (to update bookmark icon)
                onBpmChanged(it.bpm)
            }

            // set launcher shortcuts
            if (Build.VERSION.SDK_INT >= 25) {
                val shortcutManager = getSystemService(Context.SHORTCUT_SERVICE) as? ShortcutManager
                shortcutManager?.dynamicShortcuts = ArrayList<ShortcutInfo>().apply {
                    for (bpm in bookmarks) {
                        add(ShortcutInfo.Builder(this@MainActivity, bpm.toString())
                                .setShortLabel(getString(R.string.bpm, bpm.toString()))
                                .setIcon(Icon.createWithResource(this@MainActivity, R.drawable.ic_metronome))
                                .setIntent(getBookmarkIntent(bpm))
                                .build())
                    }
                }
            }

            // recreate bookmark views
            for (i in bookmarks.indices) {
                if (!isBookmark(bookmarks[i])) {
                    val bpm = bookmarks[i]
                    val isSelected = bpm == service?.bpm
                    val view = LayoutInflater.from(this).inflate(R.layout.item_bookmark, bookmarkLayout, false)

                    view.tag = bpm
                    view.setOnClickListener { clickedView: View ->
                        (clickedView.tag as? Int)?.let { service?.bpm = it }
                    }

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
                        view.setOnLongClickListener { clickedView: View ->
                            clickedView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            (clickedView.tag as? Int)?.let {
                                AlertDialog.Builder(this@MainActivity)
                                        .setTitle(R.string.title_add_shortcut)
                                        .setMessage(R.string.msg_add_shortcut)
                                        .setPositiveButton(android.R.string.ok) { dialogInterface: DialogInterface, i12: Int ->
                                            val intent = Intent("com.android.launcher.action.INSTALL_SHORTCUT")
                                            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, getBookmarkIntent(it))
                                            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.bpm, it.toString()))
                                            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(applicationContext, R.mipmap.ic_launcher))
                                            intent.putExtra("duplicate", false)
                                            sendBroadcast(intent)
                                            startActivity(Intent(Intent.ACTION_MAIN)
                                                    .addCategory(Intent.CATEGORY_HOME)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                            dialogInterface.dismiss()
                                        }
                                        .setNegativeButton(android.R.string.cancel) { dialogInterface: DialogInterface, i1: Int -> dialogInterface.dismiss() }
                                        .show()
                            }

                            false
                        }
                    }

                    bookmarkLayout?.addView(view, i)

                    view.findViewById<TextView>(R.id.title).apply {
                        text = getString(R.string.bpm, bpm.toString())
                        setTextColor(if (isSelected) textColorAccent else textColorPrimary)
                    }
                }
            }

            // remove inactive/invalid bookmarks
            for (i in 0 until (bookmarkLayout?.childCount ?: 0)) {
                if (!isBookmark(bookmarkLayout?.getChildAt(i))) {
                    try {
                        bookmarkLayout?.removeViewAt(i)
                    } catch (e : NullPointerException) {
                        if (BuildConfig.DEBUG)
                            e.printStackTrace()
                    }
                }
            }
        } else service?.let {
            for (i in 0 until (bookmarkLayout?.childCount ?: 0)) {
                val view = bookmarkLayout?.getChildAt(i)
                val bpm = (view?.tag as? Int) ?: continue

                val isSelected = it.bpm == bpm
                val titleView = view.findViewById<TextView>(R.id.title)
                ValueAnimator.ofObject(
                        ArgbEvaluator(),
                        titleView.currentTextColor,
                        if (isSelected) textColorAccent else textColorPrimary
                ).apply {
                    duration = 250
                    addUpdateListener { valueAnimator: ValueAnimator ->
                        val color = valueAnimator.animatedValue as Int
                        titleView.setTextColor(color)
                    }
                    start()
                }
            }
        }
    }

    private fun isBookmark(bpm: Int): Boolean {
        for (i in 0 until (bookmarkLayout?.childCount ?: 0)) {
            val tag = (bookmarkLayout?.getChildAt(i)?.tag as? Int) ?: continue
            if (tag == bpm)
                return true
        }
        return false
    }

    private fun isBookmark(view: View?): Boolean {
        return (view?.tag as? Int)?.let { bookmarks.contains(it) } ?: false
    }

    private fun getBookmarkIntent(bpm: Int): Intent {
        return Intent(this, DummyShortcutActivity::class.java)
                .setAction(MetronomeService.ACTION_START)
                .putExtra(MetronomeService.EXTRA_BPM, bpm)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    override fun onTickChanged(tick: Int) {
        service?.tick = tick
    }

    private fun getEmphasisSwitch(isChecked: Boolean): EmphasisSwitch {
        val emphasisSwitch = EmphasisSwitch(this)
        emphasisSwitch.setChecked(isChecked)
        emphasisSwitch.setOnCheckedChangeListener(this)
        emphasisSwitch.layoutParams = LinearLayout.LayoutParams(dpToPx(40f), dpToPx(40f))
        return emphasisSwitch
    }

    override fun onStart() {
        val intent = Intent(this, MetronomeService::class.java)
        startService(intent)
        bindService(intent, this, Context.BIND_AUTO_CREATE)
        super.onStart()
    }

    override fun onStop() {
        service?.let {
            service = null
            unbindService(this)
        }

        super.onStop()
    }

    override fun onDestroy() {
        metronome.onDestroyActivity()
        super.onDestroy()
    }

    override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
        val binder = iBinder as LocalBinder
        service = binder.service
        service?.setTickListener(this)

        bindToService()
    }

    override fun onServiceDisconnected(componentName: ComponentName) {
        service = null
    }

    override fun onStartTicks() {
        bindPlayPause()
    }

    override fun onTick(isEmphasis: Boolean, index: Int) {
        metronomeView?.onTick(isEmphasis)
        emphasisLayout?.apply {
            for (i in 0 until childCount) {
                (getChildAt(i) as? EmphasisSwitch)?.setAccented(i == index)
            }
        }
    }

    override fun onBpmChanged(bpm: Int) {
        service?.let {
            bindTempo()
            bookmarkView?.setIconResource(
                    if (bookmarks.contains(bpm)) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border
            )

            updateBookmarks(false)
            if (seekBar?.getProgress() != bpm) {
                seekBar?.setOnProgressChangeListener(null)
                seekBar?.setProgress(bpm)
                seekBar?.setOnProgressChangeListener(this)
            }
        }
    }

    override fun onStopTicks() {
        bindPlayPause()
        for (i in 0 until (emphasisLayout?.childCount ?: 0)) {
            (emphasisLayout?.getChildAt(i) as? EmphasisSwitch)?.setAccented(false)
        }
    }

    override fun onCheckedChanged(emphasisSwitch: EmphasisSwitch?, b: Boolean) { service?.let {
        val emphasisList: MutableList<Boolean> = ArrayList()
        for (i in 0 until (emphasisLayout?.childCount ?: 0)) {
            emphasisList.add((emphasisLayout?.getChildAt(i) as? EmphasisSwitch)?.isChecked() ?: false)
        }
        it.emphasisList = emphasisList
    }}

    override fun onProgressChange(progress: Int) {
        if (progress > 0) service?.apply { bpm = progress }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Billing.REQUEST_PURCHASE) data?.let {
            metronome.onPremiumBought(resultCode, it)
        }
    }

    private inner class SplashThread(activity: MainActivity) : Thread() {

        private val activityReference: WeakReference<MainActivity> = WeakReference(activity)

        override fun run() {
            try {
                sleep(3000)
            } catch (e: InterruptedException) {
                return
            }

            Handler(Looper.getMainLooper()).post {
                activityReference.get()?.iconView?.visibility = View.GONE
            }
        }

    }

    companion object {
        private val BPM_RANGE = (1..300)
    }
}