package james.metronome.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.afollestad.aesthetic.Aesthetic;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class SeekBar extends View implements View.OnTouchListener {

    private Paint paint;

    private OnProgressChangeListener listener;
    private int progress;
    private int maxProgress = 100;

    private Disposable subscription;

    public SeekBar(Context context) {
        this(context, null);
    }

    public SeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint = new Paint();
        paint.setAntiAlias(true);

        setOnTouchListener(this);
        setClickable(true);

        subscribe();
    }

    public void subscribe() {
        subscription = Aesthetic.get()
                .textColorPrimary()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        paint.setColor(integer);
                        invalidate();
                    }
                });
    }

    public void unsubscribe() {
        subscription.dispose();
    }

    public void setProgress(int progress) {
        this.progress = progress;
        invalidate();

        if (listener != null)
            listener.onProgressChange(progress);
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
            case MotionEvent.ACTION_MOVE:
                setProgress((int) (maxProgress * (x / view.getMeasuredWidth())));
        }
        return false;
    }

    public interface OnProgressChangeListener {
        void onProgressChange(int progress);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setAlpha(50);
        canvas.drawRect(0, 0, canvas.getWidth(), 2, paint);
        paint.setAlpha(255);
        canvas.drawRect(0, 0, (int) (canvas.getWidth() * ((float) progress / maxProgress)), 2, paint);
    }
}
