package james.metronome.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.afollestad.aesthetic.Aesthetic;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import james.metronome.utils.ConversionUtils;

public class EmphasisSwitch extends View implements View.OnClickListener {

    private Paint paint;
    private Paint outlinePaint;
    private Paint accentPaint;
    private Paint accentOutlinePaint;

    private Disposable colorAccentSubscription;
    private Disposable textColorPrimarySubscription;
    private Disposable textColorSecondarySubscription;

    private float checked;
    private boolean isChecked;
    private boolean isAccented;
    private OnCheckedChangeListener listener;

    private final int margin = ConversionUtils.getPixelsFromDp(8);

    public EmphasisSwitch(Context context) {
        this(context, null);
    }

    public EmphasisSwitch(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmphasisSwitch(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnClickListener(this);

        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        outlinePaint = new Paint();
        outlinePaint.setColor(Color.BLACK);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(2);
        outlinePaint.setAntiAlias(true);

        accentPaint = new Paint();
        accentPaint.setColor(Color.BLACK);
        accentPaint.setStyle(Paint.Style.FILL);
        accentPaint.setAntiAlias(true);

        accentOutlinePaint = new Paint();
        accentOutlinePaint.setColor(Color.BLACK);
        accentOutlinePaint.setStyle(Paint.Style.STROKE);
        accentOutlinePaint.setStrokeWidth(2);
        accentOutlinePaint.setAntiAlias(true);
    }

    public void setAccented(boolean isOutlined) {
        this.isAccented = isOutlined;
        invalidate();
    }

    public void setChecked(boolean isChecked) {
        if (isChecked != this.isChecked) {
            this.isChecked = isChecked;

            ValueAnimator animator = ValueAnimator.ofFloat(isChecked ? 0 : 1, isChecked ? 1 : 0);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    checked = (float) valueAnimator.getAnimatedValue();
                    invalidate();
                }
            });
            animator.start();
        }
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    public void subscribe() {
        colorAccentSubscription = Aesthetic.get()
                .colorAccent()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(@NonNull Integer integer) throws Exception {
                        accentPaint.setColor(integer);
                        accentOutlinePaint.setColor(integer);
                        invalidate();
                    }
                });

        textColorPrimarySubscription = Aesthetic.get()
                .textColorPrimary()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        paint.setColor(integer);
                        invalidate();
                    }
                });

        textColorSecondarySubscription = Aesthetic.get()
                .textColorSecondary()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        outlinePaint.setColor(integer);
                        invalidate();
                    }
                });
    }

    public void unsubscribe() {
        colorAccentSubscription.dispose();
        textColorPrimarySubscription.dispose();
        textColorSecondarySubscription.dispose();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int size = ConversionUtils.getPixelsFromDp(12);
        canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2, size, isAccented ? accentOutlinePaint : outlinePaint);
        canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2, checked * size, isAccented ? accentPaint : paint);
    }

    @Override
    public void onClick(View view) {
        setChecked(!isChecked);
        if (listener != null)
            listener.onCheckedChanged(this, isChecked);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(size, size);
    }

    public interface OnCheckedChangeListener {
        void onCheckedChanged(EmphasisSwitch emphasisSwitch, boolean b);
    }
}
