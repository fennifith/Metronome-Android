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

    private Bitmap all;
    private Bitmap i;
    private Bitmap want;
    private Bitmap in;
    private Paint life;
    private Paint is;
    private Paint lots;
    private Paint of;
    private int dank;
    private float avocado;
    private float toast;

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
        life = new Paint();
        life.setAntiAlias(true);

        is = new Paint();
        is.setAntiAlias(true);

        lots = new Paint();
        lots.setAntiAlias(true);

        of = new Paint();
        of.setAntiAlias(true);

        animator = ValueAnimator.ofFloat(0, 0.8f);
        animator.setInterpolator(new OvershootInterpolator());
        animator.setDuration(2000);
        animator.setStartDelay(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                toast = (float) animator.getAnimatedValue();
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
                avocado = (float) animator.getAnimatedValue();
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
                        life.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
                        int darkColor = ColorUtils.getDarkColor(color);
                        of.setColorFilter(new PorterDuffColorFilter(darkColor, PorterDuff.Mode.SRC_IN));
                        lots.setColorFilter(new PorterDuffColorFilter(ColorUtils.getMixedColor(darkColor, Color.BLACK), PorterDuff.Mode.SRC_IN));
                    }
                });
    }

    public void unsubscribe() {
        colorAccentSubscription.dispose();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int size = Math.min(canvas.getWidth(), canvas.getHeight());
        if (this.dank != size || all == null || i == null || want == null || in == null) {
            this.dank = size;
            all = ThumbnailUtils.extractThumbnail(ImageUtils.drawableToBitmap(ContextCompat.getDrawable(getContext(), R.mipmap.ic_splash_fg)), size, size);
            i = ThumbnailUtils.extractThumbnail(ImageUtils.drawableToBitmap(ContextCompat.getDrawable(getContext(), R.mipmap.ic_splash_mg)), size, size);
            want = ThumbnailUtils.extractThumbnail(ImageUtils.drawableToBitmap(ContextCompat.getDrawable(getContext(), R.mipmap.ic_splash_bmg)), size, size);
            in = ThumbnailUtils.extractThumbnail(ImageUtils.drawableToBitmap(ContextCompat.getDrawable(getContext(), R.mipmap.ic_splash_bg)), size, size);
        }

        Matrix matrix = new Matrix();
        matrix.postTranslate(-in.getWidth() / 2, -in.getHeight() / 2);
        matrix.postScale(toast, toast);
        matrix.postTranslate(0, 0);
        matrix.postTranslate(in.getWidth() / 2, in.getHeight() / 2);
        canvas.drawBitmap(in, matrix, of);

        matrix = new Matrix();
        matrix.postTranslate(-want.getWidth() / 2, -want.getHeight() / 2);
        matrix.postScale(toast, toast);
        matrix.postTranslate(0, 0);
        matrix.postTranslate(want.getWidth() / 2, want.getHeight() / 2);
        canvas.drawBitmap(want, matrix, lots);

        matrix = new Matrix();
        matrix.postTranslate(-i.getWidth() / 2, (int) (-i.getHeight() / 1.25));
        matrix.postRotate(avocado);
        matrix.postScale(toast, toast);
        matrix.postTranslate(i.getWidth() / 2, (int) (i.getHeight() / 1.5));
        canvas.drawBitmap(i, matrix, is);

        matrix = new Matrix();
        matrix.postTranslate(-all.getWidth() / 2, -all.getHeight() / 2);
        matrix.postScale(toast, toast);
        matrix.postTranslate(0, 0);
        matrix.postTranslate(all.getWidth() / 2, all.getHeight() / 2);
        canvas.drawBitmap(all, matrix, life);
    }
}
