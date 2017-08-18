package james.metronome.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.media.ThumbnailUtils;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.afollestad.aesthetic.Aesthetic;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import james.metronome.R;
import james.metronome.utils.ColorUtils;
import james.metronome.utils.ImageUtils;

public class AppIconView extends View {

    private Bitmap fgBitmap;
    private Bitmap mgBitmap;
    private Bitmap bmgBitmap;
    private Bitmap bgBitmap;
    private Paint fgPaint;
    private Paint mgPaint;
    private Paint bmgPaint;
    private Paint bgPaint;
    private int size;
    private float rotation;
    private float bgScale;

    private ValueAnimator animator;

    private Disposable colorAccentSubscription;

    public AppIconView(Context context) {
        this(context, null);
    }

    public AppIconView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppIconView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        fgPaint = new Paint();
        fgPaint.setAntiAlias(true);

        mgPaint = new Paint();
        mgPaint.setAntiAlias(true);

        bmgPaint = new Paint();
        bmgPaint.setAntiAlias(true);

        bgPaint = new Paint();
        bgPaint.setAntiAlias(true);

        animator = ValueAnimator.ofFloat(0, 0.8f);
        animator.setInterpolator(new OvershootInterpolator());
        animator.setDuration(2000);
        animator.setStartDelay(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                bgScale = (float) animator.getAnimatedValue();
            }
        });
        animator.start();

        this.animator = ValueAnimator.ofFloat(0, -48);
        this.animator.setInterpolator(new AccelerateDecelerateInterpolator());
        this.animator.setDuration(1500);
        this.animator.setRepeatMode(ValueAnimator.REVERSE);
        this.animator.setRepeatCount(ValueAnimator.INFINITE);
        this.animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                rotation = (float) animator.getAnimatedValue();
                invalidate();
            }
        });
        this.animator.start();

        subscribe();
    }

    public void subscribe() {
        colorAccentSubscription = Aesthetic.get()
                .colorAccent()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer color) throws Exception {
                        fgPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
                        int darkColor = ColorUtils.getDarkColor(color);
                        bgPaint.setColorFilter(new PorterDuffColorFilter(darkColor, PorterDuff.Mode.SRC_IN));
                        bmgPaint.setColorFilter(new PorterDuffColorFilter(ColorUtils.getMixedColor(darkColor, Color.BLACK), PorterDuff.Mode.SRC_IN));
                    }
                });
    }

    public void unsubscribe() {
        colorAccentSubscription.dispose();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int size = Math.min(canvas.getWidth(), canvas.getHeight());
        if (this.size != size || fgBitmap == null || mgBitmap == null || bmgBitmap == null || bgBitmap == null) {
            this.size = size;
            fgBitmap = ThumbnailUtils.extractThumbnail(ImageUtils.drawableToBitmap(ContextCompat.getDrawable(getContext(), R.mipmap.ic_splash_fg)), size, size);
            mgBitmap = ThumbnailUtils.extractThumbnail(ImageUtils.drawableToBitmap(ContextCompat.getDrawable(getContext(), R.mipmap.ic_splash_mg)), size, size);
            bmgBitmap = ThumbnailUtils.extractThumbnail(ImageUtils.drawableToBitmap(ContextCompat.getDrawable(getContext(), R.mipmap.ic_splash_bmg)), size, size);
            bgBitmap = ThumbnailUtils.extractThumbnail(ImageUtils.drawableToBitmap(ContextCompat.getDrawable(getContext(), R.mipmap.ic_splash_bg)), size, size);
        }

        Matrix matrix = new Matrix();
        matrix.postTranslate(-bgBitmap.getWidth() / 2, -bgBitmap.getHeight() / 2);
        matrix.postScale(bgScale, bgScale);
        matrix.postTranslate(0, 0);
        matrix.postTranslate(bgBitmap.getWidth() / 2, bgBitmap.getHeight() / 2);
        canvas.drawBitmap(bgBitmap, matrix, bgPaint);

        matrix = new Matrix();
        matrix.postTranslate(-bmgBitmap.getWidth() / 2, -bmgBitmap.getHeight() / 2);
        matrix.postScale(bgScale, bgScale);
        matrix.postTranslate(0, 0);
        matrix.postTranslate(bmgBitmap.getWidth() / 2, bmgBitmap.getHeight() / 2);
        canvas.drawBitmap(bmgBitmap, matrix, bmgPaint);

        matrix = new Matrix();
        matrix.postTranslate(-mgBitmap.getWidth() / 2, (int) (-mgBitmap.getHeight() / 1.25));
        matrix.postRotate(rotation);
        matrix.postScale(bgScale, bgScale);
        matrix.postTranslate(mgBitmap.getWidth() / 2, (int) (mgBitmap.getHeight() / 1.5));
        canvas.drawBitmap(mgBitmap, matrix, mgPaint);

        matrix = new Matrix();
        matrix.postTranslate(-fgBitmap.getWidth() / 2, -fgBitmap.getHeight() / 2);
        matrix.postScale(bgScale, bgScale);
        matrix.postTranslate(0, 0);
        matrix.postTranslate(fgBitmap.getWidth() / 2, fgBitmap.getHeight() / 2);
        canvas.drawBitmap(fgBitmap, matrix, fgPaint);
    }
}
