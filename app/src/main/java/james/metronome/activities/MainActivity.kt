package james.metronome.activities

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
import com.google.android.material.button.MaterialButton
import james.metronome.BuildConfig
import james.metronome.Metronome
import james.metronome.R
import james.metronome.billing.Billing
import james.metronome.services.MetronomeService
import james.metronome.services.MetronomeService.LocalBinder
import james.metronome.services.MetronomeService.TickListener
import james.metronome.utils.bind
import james.metronome.utils.edit
import james.metronome.utils.getThemedColor
import james.metronome.views.EmphasisSwitch
import james.metronome.views.MetronomeView
import james.metronome.views.SeekBar
import james.metronome.views.SeekBar.OnProgressChangeListener
import james.metronome.views.TicksView
import james.metronome.views.TicksView.OnTickChangedListener
import me.jfenn.androidutils.DimenUtils
import java.lang.ref.WeakReference
import java.util.*

class MainActivity : AppCompatActivity(), OnTickChangedListener, ServiceConnection, TickListener, EmphasisSwitch.OnCheckedChangeListener, OnProgressChangeListener {

    private var service: MetronomeService? = null

    private val metronomeView: MetronomeView? by bind(R.id.metronome)
    private val playView: ImageView? by bind(R.id.play)
    private val emphasisLayout: LinearLayout? by bind(R.id.emphasis)
    private val bookmarkLayout: LinearLayout? by bind(R.id.bookmarks)
    private val bpmView: TextView? by bind(R.id.bpm)
    private val bookmarkView: MaterialButton? by bind(R.id.button_bookmark)
    private val touchView: MaterialButton? by bind(R.id.button_touch)
    private val lessView: ImageView? by bind(R.id.bpm_decrease)
    private val moreView: ImageView? by bind(R.id.bpm_increase)
    private val addEmphasisView: ImageView? by bind(R.id.emphasis_add)
    private val removeEmphasisView: ImageView? by bind(R.id.emphasis_remove)
    private val ticksView: TicksView? by bind(R.id.ticks)
    private val seekBar: SeekBar? by bind(R.id.seekbar)

    private val textColorAccent by lazy { getThemedColor(R.attr.textColorAccent) }
    private val textColorPrimary by lazy { getThemedColor(R.attr.textColorPrimary) }

    private val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    private val bookmarks: MutableList<Int> by lazy {
        ArrayList<Int>().apply {
            val bookmarksLength = prefs.getInt(PREF_BOOKMARKS_LENGTH, 0)
            for (i in 0 until bookmarksLength) {
                add(prefs.getInt(PREF_BOOKMARK + i, -1))
            }
        }
    }

    private var prevTouchInterval: Long = 0
    private var prevTouchTime: Long = 0
    private val metronome: Metronome get() = applicationContext as Metronome

    private fun bindToService() {
        service?.let {
            ticksView?.setTick(it.tick)
            metronomeView?.setInterval(it.interval)
            seekBar?.setProgress(it.bpm)
            bpmView?.text = String.format(Locale.getDefault(), getString(R.string.bpm), it.bpm.toString())
            playView?.setImageResource(if (service!!.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            emphasisLayout?.apply {
                removeAllViews()
                for (isEmphasis in it.emphasisList) {
                    addView(getEmphasisSwitch(isEmphasis, true))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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

                    emphasisLayout?.addView(getEmphasisSwitch(false, true))
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

        lessView?.setOnClickListener { _ ->
            service?.bpm?.minus(1)?.coerceIn(BPM_RANGE)?.let {
                seekBar?.setProgress(it)
            }
        }

        seekBar?.setOnProgressChangeListener(this)
        ticksView?.setListener(this)
        SplashThread(this).start()
    }

    private fun addBookmark(bpm: Int) = service?.let {
        if (!bookmarks.contains(bpm)) {
            bookmarks.add(bpm)
            saveBookmarks()
            it.bpm = bpm
        }
    }

    private fun removeBookmark(bpm: Int) = service?.let {
        bookmarks.remove(bpm)
        saveBookmarks()

        if (it.bpm == bpm)
            it.bpm = bpm
    }

    private fun saveBookmarks() {
        prefs.edit {
            for (i in bookmarks.indices) {
                putInt(PREF_BOOKMARK + i, bookmarks[i])
            }
            putInt(PREF_BOOKMARKS_LENGTH, bookmarks.size)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            bookmarks.sort()

            val shortcutManager = getSystemService(Context.SHORTCUT_SERVICE) as? ShortcutManager
            shortcutManager?.dynamicShortcuts = ArrayList<ShortcutInfo>().apply {
                for (bpm in bookmarks) {
                    add(ShortcutInfo.Builder(this@MainActivity, bpm.toString())
                            .setShortLabel(getString(R.string.bpm, bpm.toString()))
                            .setIcon(Icon.createWithResource(this@MainActivity, R.drawable.ic_note))
                            .setIntent(getBookmarkIntent(bpm))
                            .build())
                }
            }
        }

        updateBookmarks(true)
    }

    private fun updateBookmarks(contentChanged: Boolean) {
        if (contentChanged) {
            bookmarks.sort()
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

                val isSelected = service?.bpm == bpm
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

    private fun getEmphasisSwitch(isChecked: Boolean, subscribe: Boolean): EmphasisSwitch {
        val emphasisSwitch = EmphasisSwitch(this)
        emphasisSwitch.setChecked(isChecked)
        emphasisSwitch.setOnCheckedChangeListener(this)
        emphasisSwitch.layoutParams = LinearLayout.LayoutParams(DimenUtils.dpToPx(40f), DimenUtils.dpToPx(40f))
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
        playView?.setImageResource(R.drawable.ic_pause)
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
            metronomeView?.setInterval(service!!.interval)
            bpmView?.text = String.format(Locale.getDefault(), getString(R.string.bpm), bpm.toString())
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
        playView!!.setImageResource(R.drawable.ic_play)
        for (i in 0 until emphasisLayout!!.childCount) {
            (emphasisLayout!!.getChildAt(i) as EmphasisSwitch).setAccented(false)
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
                activityReference.get()?.findViewById<View>(R.id.icon)?.visibility = View.GONE
            }
        }

    }

    companion object {
        private const val PREF_BOOKMARKS_LENGTH = "bookmarksLength"
        private const val PREF_BOOKMARK = "bookmark"
        private val BPM_RANGE = (1..300)
    }
}