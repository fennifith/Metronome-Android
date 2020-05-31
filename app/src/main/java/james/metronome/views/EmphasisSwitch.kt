package james.metronome.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import james.metronome.R
import james.metronome.utils.getThemedColor
import me.jfenn.androidutils.DimenUtils
import kotlin.math.min

class EmphasisSwitch @JvmOverloads constructor(
        context: Context?,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), View.OnClickListener {

    private val circleRadius = DimenUtils.dpToPx(10f).toFloat()
    private val borderWidth = DimenUtils.dpToPx(3f).toFloat()

    private val paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val outlinePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
        isAntiAlias = true
    }

    private val accentPaint = Paint().apply {
        color = context?.getThemedColor(R.attr.textColorAccent) ?: Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val accentOutlinePaint = Paint().apply {
        color = context?.getThemedColor(R.attr.textColorAccent) ?: Color.RED
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
        isAntiAlias = true
    }

    private var checked = 0f
    private var isChecked = false
    private var isAccented = false
    private var listener: OnCheckedChangeListener? = null

    init {
        setOnClickListener(this)
    }

    fun setAccented(isOutlined: Boolean) {
        isAccented = isOutlined
        invalidate()
    }

    fun setChecked(isChecked: Boolean) {
        if (isChecked != this.isChecked) {
            this.isChecked = isChecked
            ValueAnimator.ofFloat(if (isChecked) 0f else 1f, if (isChecked) 1f else 0f).apply {
                interpolator = DecelerateInterpolator()
                addUpdateListener { valueAnimator ->
                    checked = valueAnimator.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
    }

    fun isChecked(): Boolean {
        return isChecked
    }

    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        this.listener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val fill = if (isAccented) paint else accentPaint
        fill.alpha = (checked * 0.3f * 255).toInt()
        canvas.drawCircle(width / 2f, height / 2f, circleRadius, fill)

        val outline = if (isAccented) outlinePaint else accentOutlinePaint
        canvas.drawCircle(width / 2f, height / 2f, circleRadius, outline)
    }

    override fun onClick(view: View) {
        setChecked(!isChecked)
        listener?.onCheckedChanged(this, isChecked)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = min(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)
    }

    interface OnCheckedChangeListener {
        fun onCheckedChanged(emphasisSwitch: EmphasisSwitch?, b: Boolean)
    }

}