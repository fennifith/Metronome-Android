package james.metronome.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ThumbnailUtils;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import james.metronome.R;
import james.metronome.utils.ImageUtils;

public class AppIconView extends View {

    private Bitmap fgBitmap;
    private Bitmap mgBitmap;
    private Bitmap bgBitmap;
    private Paint paint;
    private int size;
    private float rotation;
    private float fgScale, bgScale;

    public AppIconView(Context context) {
        this(context, null);
    }

    public AppIconView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppIconView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint = new Paint();
        paint.setAntiAlias(true);

        ValueAnimator animator = ValueAnimator.ofFloat(0, 0.8f);
        animator.setInterpolator(new OvershootInterpolator());
        animator.setDuration(2000);
        animator.setStartDelay(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                fgScale = (float) animator.getAnimatedValue();
            }
        });
        animator.start();

        animator = ValueAnimator.ofFloat(0, 0.8f);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(2000);
        animator.setStartDelay(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                bgScale = (float) animator.getAnimatedValue();
            }
        });
        animator.start();

        animator = ValueAnimator.ofFloat(0, -48);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(1500);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                rotation = (float) animator.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }

    private Bitmap getRoundBitmap(@DrawableRes int drawable, int size) {
        Bitmap bitmap = ImageUtils.drawableToBitmap(ContextCompat.getDrawable(getContext(), drawable));
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, size, size);

        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), bitmap);

        roundedBitmapDrawable.setCornerRadius(size / 2);
        roundedBitmapDrawable.setAntiAlias(true);

        return ImageUtils.drawableToBitmap(roundedBitmapDrawable);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int size = Math.min(canvas.getWidth(), canvas.getHeight());
        if (this.size != size || fgBitmap == null || bgBitmap == null) {
            this.size = size;
            fgBitmap = getRoundBitmap(R.mipmap.ic_splash_fg, size);
            mgBitmap = ThumbnailUtils.extractThumbnail(ImageUtils.drawableToBitmap(ContextCompat.getDrawable(getContext(), R.mipmap.ic_splash_mg)), size, size);
            bgBitmap = getRoundBitmap(R.mipmap.ic_splash_bg, size);
        }

        Matrix matrix = new Matrix();
        matrix.postTranslate(-fgBitmap.getWidth() / 2, -fgBitmap.getHeight() / 2);
        matrix.postScale(bgScale, bgScale);
        matrix.postTranslate(0, 0);
        matrix.postTranslate(fgBitmap.getWidth() / 2, fgBitmap.getHeight() / 2);
        canvas.drawBitmap(bgBitmap, matrix, paint);

        matrix = new Matrix();
        matrix.postTranslate(-bgBitmap.getWidth() / 2, (int) (-bgBitmap.getHeight() / 1.25));
        matrix.postRotate(rotation);
        matrix.postScale(fgScale, fgScale);
        matrix.postTranslate(bgBitmap.getWidth() / 2, (int) (bgBitmap.getHeight() / 1.5));
        canvas.drawBitmap(mgBitmap, matrix, paint);

        matrix = new Matrix();
        matrix.postTranslate(-fgBitmap.getWidth() / 2, -fgBitmap.getHeight() / 2);
        matrix.postScale(bgScale, bgScale);
        matrix.postTranslate(0, 0);
        matrix.postTranslate(fgBitmap.getWidth() / 2, fgBitmap.getHeight() / 2);
        canvas.drawBitmap(fgBitmap, matrix, paint);
    }
}
