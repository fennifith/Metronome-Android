package james.metronome.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.GravityCompat

class CenteredOverflowLayout : LinearLayout {
    constructor(context: Context?) : super(context, null) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs, 0) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        var width = 0
        for (i in 0 until childCount) {
            width += getChildAt(i).measuredWidth
        }

        setMeasuredDimension(width, measuredHeight)

        val layoutParams = layoutParams as? FrameLayout.LayoutParams ?: return
        (parent as? View)?.let {
            layoutParams.gravity = if (it.measuredWidth > width) Gravity.CENTER_HORIZONTAL else GravityCompat.START
            setLayoutParams(layoutParams)
        }
    }
}