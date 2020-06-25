package me.jfenn.metronome.views

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import me.jfenn.androidutils.bind
import me.jfenn.androidutils.dpToPx
import me.jfenn.attribouter.Attribouter
import me.jfenn.metronome.BuildConfig
import me.jfenn.metronome.R
import me.jfenn.metronome.data.TickData
import me.jfenn.metronome.utils.getThemedColor

class TicksView @JvmOverloads constructor(
        context: Context?,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        val ticks = arrayOf(
                TickData(R.string.title_beep, R.raw.beep),
                TickData(R.string.title_click, R.raw.click),
                TickData(R.string.title_ding, R.raw.ding),
                TickData(R.string.title_wood, R.raw.wood),
                TickData(R.string.title_vibrate)
        )
    }

    private var listener: OnTickChangedListener? = null
    private var isExpanded = false
    private var selectedItem = 0

    private val itemsView: LinearLayout? by bind(R.id.items)
    private val expandView: ImageView? by bind(R.id.button_expand)
    private val aboutView: ImageView? by bind(R.id.button_about)

    private val foregroundColor: Int by lazy { context?.getThemedColor(R.attr.foregroundColor) ?: Color.WHITE }
    private val textColorPrimary: Int by lazy { context?.getThemedColor(R.attr.textColorPrimary) ?: Color.BLACK }
    private val textColorAccent: Int by lazy { context?.getThemedColor(R.attr.textColorAccent) ?: Color.RED }

    val itemColors: MutableMap<Int, Int> = HashMap()

    init {
        val inflater = LayoutInflater.from(getContext())
        val layout = inflater.inflate(R.layout.content_dropdown, this, false)
        addView(layout)

        for (tickIndex in ticks.indices) {
            val tickLayout = inflater.inflate(R.layout.item_tick, this, false)
            bindItem(tickLayout, tickIndex)
            itemsView?.addView(tickLayout)
        }

        aboutView?.setOnClickListener {
            context?.let {
                Attribouter.from(it).apply {
                    if (BuildConfig.GITHUB_TOKEN != null)
                        withGitHubToken(BuildConfig.GITHUB_TOKEN)
                }.show()
            }
        }
    }

    fun bindItem(rootView: View, index: Int) {
        val backgroundView: View = rootView.findViewById(R.id.background)
        val iconView: ImageView = rootView.findViewById(R.id.image)
        val nameView: TextView = rootView.findViewById(R.id.name)

        iconView.setImageResource(if (ticks[index].isVibration) R.drawable.ic_vibration else R.drawable.ic_note)
        nameView.text = context.getString(ticks[index].nameRes)

        ValueAnimator.ofObject(
                ArgbEvaluator(),
                itemColors[index] ?: textColorPrimary,
                if (index == selectedItem && isExpanded) textColorAccent else textColorPrimary
        ).apply {
            addUpdateListener {
                val color = it.animatedValue as Int
                backgroundView.alpha = (Color.red(color) / 255f) * 0.3f
                iconView.setColorFilter(color)
                nameView.setTextColor(color)

                if (index == 0) {
                    expandView?.setColorFilter(color)
                    expandView?.rotationX = 180f * (if (isExpanded) it.animatedFraction else (1 - it.animatedFraction))
                    aboutView?.setColorFilter(color)
                }
            }
            start()
        }

        rootView.setOnClickListener {
            if (isExpanded) {
                setTick(index)
                setExpanded(false)
            } else {
                setExpanded(true)
            }
        }

        rootView.visibility = if (index == selectedItem || isExpanded) VISIBLE else GONE
    }

    fun setExpanded(expanded: Boolean) {
        isExpanded = expanded
        notifyItemsChanged()

        ValueAnimator.ofFloat(0f, 1f).apply {
            start()
        }
        ValueAnimator.ofObject(
                ArgbEvaluator(),
                if (!expanded) foregroundColor else 0x00FFFFFF,
                if (expanded) foregroundColor else 0x00FFFFFF
        ).apply {
            addUpdateListener {
                val color = it.animatedValue as Int
                setBackgroundColor(color)
                ViewCompat.setElevation(this@TicksView, dpToPx(2f) * (if (expanded) it.animatedFraction else (1 - it.animatedFraction)))
            }
            start()
        }
    }

    fun setTick(tick: Int) {
        this.selectedItem = tick
        listener?.onTickChanged(tick)
        notifyItemsChanged()
    }

    private fun notifyItemsChanged() {
        for (i in 0 until (itemsView?.childCount ?: 0)) {
            itemsView?.getChildAt(i)?.let { bindItem(it, i) }
        }
    }

    fun setListener(listener: OnTickChangedListener?) {
        this.listener = listener
    }

    interface OnTickChangedListener {
        fun onTickChanged(tick: Int)
    }
}