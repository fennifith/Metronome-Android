package james.metronome.views

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import james.metronome.BuildConfig
import james.metronome.R
import james.metronome.data.TickData
import james.metronome.utils.getThemedColor
import me.jfenn.androidutils.DimenUtils
import me.jfenn.attribouter.Attribouter

class TicksView @JvmOverloads constructor(
        context: Context?,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

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

    private val foregroundColor: Int by lazy { context?.getThemedColor(R.attr.foregroundColor) ?: Color.WHITE }
    private val textColorPrimary: Int by lazy { context?.getThemedColor(R.attr.textColorPrimary) ?: Color.BLACK }
    private val textColorAccent: Int by lazy { context?.getThemedColor(R.attr.textColorAccent) ?: Color.RED }

    val itemColors: MutableMap<Int, Int> = HashMap()

    init {
        val inflater = LayoutInflater.from(getContext())
        for (tickIndex in ticks.indices) {
            val tickLayout = inflater.inflate(R.layout.item_tick, this, false)
            bindItem(tickLayout, tickIndex)
            addView(tickLayout)
        }
    }

    fun bindItem(rootView: View, index: Int) {
        val backgroundView: View = rootView.findViewById(R.id.background)
        val iconView: ImageView = rootView.findViewById(R.id.image)
        val nameView: TextView = rootView.findViewById(R.id.name)
        val expandView: ImageView = rootView.findViewById(R.id.expand)
        val aboutView: ImageView = rootView.findViewById(R.id.about)

        iconView.setImageResource(if (ticks[index].isVibration) R.drawable.ic_vibration else R.drawable.ic_note)
        nameView.text = ticks[index].getName(context)

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
                expandView.setColorFilter(color)
                expandView.rotationX = 180f * (if (isExpanded) it.animatedFraction else (1 - it.animatedFraction))
                aboutView.setColorFilter(color)
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    if ((index != 0 && isExpanded) || (index == selectedItem && !isExpanded)) {
                        expandView.visibility = GONE
                        aboutView.visibility = GONE
                    }
                }

                override fun onAnimationEnd(animation: Animator?) {
                    expandView.visibility = if (index == 0 || !isExpanded) VISIBLE else GONE
                    aboutView.visibility = if (index == 0 || !isExpanded) VISIBLE else GONE
                }

                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}
            })
            duration = 500
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

        if (index == 0) {
            expandView.visibility = VISIBLE
            aboutView.visibility = VISIBLE
        }

        aboutView.setOnClickListener { Attribouter.from(context).withGitHubToken(BuildConfig.GITHUB_TOKEN).show() }
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
                ViewCompat.setElevation(this@TicksView, DimenUtils.dpToPx(2f) * (if (expanded) it.animatedFraction else (1 - it.animatedFraction)))
            }
            start()
        }
    }

    fun setTick(tick: Int) {
        this.selectedItem = tick
        notifyItemsChanged()
    }

    private fun notifyItemsChanged() {
        for (i in 0 until childCount) {
            bindItem(getChildAt(i), i)
        }
    }

    fun setListener(listener: OnTickChangedListener?) {
        this.listener = listener
    }

    interface OnTickChangedListener {
        fun onTickChanged(tick: Int)
    }
}