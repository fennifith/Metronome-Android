package me.jfenn.metronome.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import me.jfenn.androidutils.dpToPx
import me.jfenn.metronome.R
import me.jfenn.metronome.utils.getThemedColor
import kotlin.math.abs

class SeekBar @JvmOverloads constructor(
        context: Context?,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), OnTouchListener {

    private val lineWidth = dpToPx(2f).toFloat()

    private val secondaryPaint = Paint().apply {
        color = context?.getThemedColor(R.attr.textColorPrimary) ?: Color.BLACK
        strokeWidth = lineWidth
        isAntiAlias = true
    }

    private val accentPaint = Paint().apply {
        color = context?.getThemedColor(R.attr.colorAccent) ?: Color.RED
        strokeWidth = lineWidth
        isAntiAlias = true
    }

    private var listener: OnProgressChangeListener? = null
    private var progress = 0
    private var displayedProgress = 0f
    private var maxProgress = 100
    private var touchDiff = 0f

    init {
        setOnTouchListener(this)
        isClickable = true
    }

    fun setProgress(progress: Int, immediate: Boolean = false) {
        this.progress = progress
        if (immediate) this.displayedProgress = progress.toFloat()

        listener?.onProgressChange(progress)
        invalidate()
    }

    fun getProgress(): Int {
        return progress
    }

    fun setMaxProgress(maxProgress: Int) {
        this.maxProgress = maxProgress
        invalidate()
    }

    fun setOnProgressChangeListener(listener: OnProgressChangeListener?) {
        this.listener = listener
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val x = event.x
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDiff = x - progress.toFloat() / maxProgress * view.measuredWidth
                var progress = (maxProgress * ((x - touchDiff) / view.measuredWidth)).toInt()
                if (progress < 0) progress = 0 else if (progress > maxProgress) progress = maxProgress
                setProgress(progress, true)
            }
            MotionEvent.ACTION_MOVE -> {
                var progress = (maxProgress * ((x - touchDiff) / view.measuredWidth)).toInt()
                if (progress < 0) progress = 0 else if (progress > maxProgress) progress = maxProgress
                setProgress(progress, true)
            }
        }
        return false
    }

    interface OnProgressChangeListener {
        fun onProgressChange(progress: Int)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        secondaryPaint.alpha = 255
        canvas.drawLine(0f, lineWidth / 2, width.toFloat(), lineWidth / 2, secondaryPaint)
        val currentWidth = (width * (displayedProgress / maxProgress)).toInt()
        var i = 0
        while (i < maxProgress) {
            val width = (width * (i.toFloat() / maxProgress)).toInt()
            secondaryPaint.alpha = (255 - (abs(width - currentWidth).toFloat() * 1000 / this.width).toInt()).coerceAtLeast(0)
            canvas.drawLine(width.toFloat(), lineWidth / 2, width.toFloat(), dpToPx(if (i % 20 == 0) 14f else 8f).toFloat(), secondaryPaint)
            i += 10
        }
        canvas.drawLine(0f, lineWidth / 2, currentWidth.toFloat(), lineWidth / 2, accentPaint)
        canvas.drawLine(currentWidth.toFloat(), lineWidth / 2, currentWidth.toFloat(), dpToPx(18f).toFloat(), accentPaint)

        if (displayedProgress != progress.toFloat()) {
            displayedProgress = if (abs(displayedProgress - progress) < 0.5f)
                progress.toFloat()
            else (displayedProgress + progress) / 2f

            postInvalidate()
        }
    }
}