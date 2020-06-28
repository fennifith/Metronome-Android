package me.jfenn.metronome.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import me.jfenn.metronome.R
import me.jfenn.metronome.utils.getThemedColor

class MetronomeView @JvmOverloads constructor(
        context: Context?,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
        color = context?.getThemedColor(android.R.attr.textColorPrimary) ?: Color.BLACK
    }

    private val accentPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
        color = context?.getThemedColor(R.attr.colorAccent) ?: Color.RED
    }

    private var interval: Long = 500
    private var distance = 0f
    private var isEmphasis = false

    fun setInterval(interval: Long) {
        this.interval = interval
    }

    fun onTick(isEmphasis: Boolean) {
        this.isEmphasis = isEmphasis
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = interval
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            distance = animation.animatedValue as Float
            invalidate()
        }
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.alpha = (255 * (1 - distance)).toInt()
        accentPaint.alpha = (255 * (1 - distance)).toInt()
        canvas.drawCircle(canvas.width / 2.toFloat(), canvas.height / 2.toFloat(), distance * Math.max(canvas.width, canvas.height) / 2, if (isEmphasis) accentPaint else paint)
        //this probably draws a circle or something idk
    }
}