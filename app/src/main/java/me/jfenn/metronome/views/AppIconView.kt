package me.jfenn.metronome.views

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
import me.jfenn.androidutils.toBitmap
import me.jfenn.metronome.R
import kotlin.math.min

class AppIconView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private var background: Bitmap? = null
    private var middleground: Bitmap? = null
    private var foreground: Bitmap? = null
    private val bitmapMatrix = Matrix()

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
            background = ThumbnailUtils.extractThumbnail(ContextCompat.getDrawable(context, R.mipmap.ic_splash_fg)?.toBitmap(), size, size)
            middleground = ThumbnailUtils.extractThumbnail(ContextCompat.getDrawable(context, R.mipmap.ic_splash_mg)?.toBitmap(), size, size)
            foreground = ThumbnailUtils.extractThumbnail(ContextCompat.getDrawable(context, R.mipmap.ic_splash_bg)?.toBitmap(), size, size)
        }

        val fgBitmap = foreground ?: return
        val mgBitmap = middleground ?: return
        val bgBitmap = background ?: return

        bitmapMatrix.reset()
        bitmapMatrix.postTranslate(-fgBitmap.width / 2f, -fgBitmap.height / 2f)
        bitmapMatrix.postScale(scale, scale)
        bitmapMatrix.postTranslate(0f, 0f)
        bitmapMatrix.postTranslate(fgBitmap.width / 2f, fgBitmap.height / 2f)
        canvas.drawBitmap(fgBitmap, bitmapMatrix, paint)

        bitmapMatrix.reset()
        bitmapMatrix.postTranslate(-mgBitmap.width / 2f, (-mgBitmap.height / 1.25f))
        bitmapMatrix.postRotate(pendulum)
        bitmapMatrix.postTranslate(mgBitmap.width / 2f, (mgBitmap.height / 1.25f))
        bitmapMatrix.postTranslate(-mgBitmap.width / 2f, -mgBitmap.height / 2f)
        bitmapMatrix.postScale(scale, scale)
        bitmapMatrix.postTranslate(mgBitmap.width / 2f, mgBitmap.height / 2f)
        canvas.drawBitmap(mgBitmap, bitmapMatrix, paint)

        bitmapMatrix.reset()
        bitmapMatrix.postTranslate(-bgBitmap.width / 2f, -bgBitmap.height / 2f)
        bitmapMatrix.postScale(scale, scale)
        bitmapMatrix.postTranslate(0f, 0f)
        bitmapMatrix.postTranslate(bgBitmap.width / 2f, bgBitmap.height / 2f)
        canvas.drawBitmap(bgBitmap, bitmapMatrix, paint)
    }
}