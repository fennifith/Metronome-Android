package james.metronome.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.afollestad.aesthetic.Aesthetic;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import james.metronome.utils.ConversionUtils;

public class SeekBar extends View implements View.OnTouchListener {

    private Paint paint;
    private Paint secondaryPaint;
    private Paint accentPaint;

    private OnProgressChangeListener listener;
    private int progress;
    private int maxProgress = 100;
    private float touchDiff;

    private Disposable textColorPrimarySubscription;
    private Disposable textColorSecondarySubscription;
    private Disposable colorAccentSubscription;

    public SeekBar(Context context) {
        this(context, null);
    }

    public SeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);

        secondaryPaint = new Paint();
        secondaryPaint.setColor(Color.BLACK);
        secondaryPaint.setAntiAlias(true);

        accentPaint = new Paint();
        accentPaint.setColor(Color.BLACK);
        accentPaint.setAntiAlias(true);

        setOnTouchListener(this);
        setClickable(true);

        subscribe();
    }

    public void subscribe() {
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
                        secondaryPaint.setColor(integer);
                        invalidate();
                    }
                });

        colorAccentSubscription = Aesthetic.get()
                .colorAccent()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        accentPaint.setColor(integer);
                        invalidate();
                    }
                });
    }

    public void unsubscribe() {
        textColorPrimarySubscription.dispose();
        textColorSecondarySubscription.dispose();
        colorAccentSubscription.dispose();
    }

    public void setProgress(int progress) {
        this.progress = progress;
        invalidate();

        if (listener != null)
            listener.onProgressChange(progress);
    }

    public int getProgress() {
        return progress;
    }

    public void setMaxProgress(int maxProgress) {
        this.maxProgress = maxProgress;
        invalidate();
    }

    public void setOnProgressChangeListener(OnProgressChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        float x = event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDiff = x - (((float) progress / maxProgress) * view.getMeasuredWidth());
            case MotionEvent.ACTION_MOVE:
                int progress = (int) (maxProgress * ((x - touchDiff) / view.getMeasuredWidth()));
                if (progress < 0)
                    progress = 0;
                else if (progress > maxProgress)
                    progress = maxProgress;

                setProgress(progress);
        }
        return false;
    }

    public interface OnProgressChangeListener {
        void onProgressChange(int progress);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        secondaryPaint.setAlpha(255);
        canvas.drawRect(0, 0, canvas.getWidth(), 2, secondaryPaint);

        int currentWidth = (int) (canvas.getWidth() * ((float) progress / maxProgress));
        for (int i = 0; i < maxProgress; i += 10) {
            int width = (int) (canvas.getWidth() * ((float) i / maxProgress));
            secondaryPaint.setAlpha(Math.max(255 - (int) ((float) Math.abs(width - currentWidth) * 1000 / canvas.getWidth()), 0));
            canvas.drawRect(width - 1, 0, width + 1, ConversionUtils.getPixelsFromDp(i % 20 == 0 ? 6 : 4), secondaryPaint);
        }

        canvas.drawRect(0, 0, currentWidth, 2, accentPaint);
        canvas.drawRect(currentWidth - 1, 0, currentWidth + 1, ConversionUtils.getPixelsFromDp(10), accentPaint);
    }
}
