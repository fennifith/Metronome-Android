package james.metronome.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ThumbnailUtils
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import james.metronome.R
import me.jfenn.androidutils.ImageUtils
import kotlin.math.min

class AppIconView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private var background: Bitmap? = null
    private var middleground: Bitmap? = null
    private var foreground: Bitmap? = null

    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
    }

    private var currentBitmapSize = 0
    private var scale = 0f
    private var pendulum = 0f

    init {
        ValueAnimator.ofFloat(0f, 1f).apply {
            interpolator = OvershootInterpolator()
            duration = 2000
            startDelay = 500
            addUpdateListener { animator ->
                scale = animator.animatedValue as Float
            }
            start()
        }

        ValueAnimator.ofFloat(0f, -48f).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = 1500
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                pendulum = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val size = min(width, height)

        if (currentBitmapSize != size || background == null || middleground == null || foreground == null) {
            currentBitmapSize = size
            background = ThumbnailUtils.extractThumbnail(ImageUtils.drawableToBitmap(ContextCompat.getDrawable(context, R.mipmap.ic_splash_fg)), size, size)
            middleground = ThumbnailUtils.extractThumbnail(ImageUtils.drawableToBitmap(ContextCompat.getDrawable(context, R.mipmap.ic_splash_mg)), size, size)
            foreground = ThumbnailUtils.extractThumbnail(ImageUtils.drawableToBitmap(ContextCompat.getDrawable(context, R.mipmap.ic_splash_bg)), size, size)
        }

        val fgBitmap = foreground ?: return
        val mgBitmap = middleground ?: return
        val bgBitmap = background ?: return

        var matrix = Matrix()
        matrix.postTranslate(-fgBitmap.width / 2f, -fgBitmap.height / 2f)
        matrix.postScale(scale, scale)
        matrix.postTranslate(0f, 0f)
        matrix.postTranslate(fgBitmap.width / 2f, fgBitmap.height / 2f)
        canvas.drawBitmap(fgBitmap, matrix, paint)

        matrix = Matrix()
        matrix.postTranslate(-mgBitmap.width / 2f, (-mgBitmap.height / 1.25f))
        matrix.postRotate(pendulum)
        matrix.postTranslate(mgBitmap.width / 2f, (mgBitmap.height / 1.25f))
        matrix.postTranslate(-mgBitmap.width / 2f, -mgBitmap.height / 2f)
        matrix.postScale(scale, scale)
        matrix.postTranslate(mgBitmap.width / 2f, mgBitmap.height / 2f)
        canvas.drawBitmap(mgBitmap, matrix, paint)

        matrix = Matrix()
        matrix.postTranslate(-bgBitmap.width / 2f, -bgBitmap.height / 2f)
        matrix.postScale(scale, scale)
        matrix.postTranslate(0f, 0f)
        matrix.postTranslate(bgBitmap.width / 2f, bgBitmap.height / 2f)
        canvas.drawBitmap(bgBitmap, matrix, paint)
    }
}