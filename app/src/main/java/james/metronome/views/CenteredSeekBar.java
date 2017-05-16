package james.metronome.views;

import android.content.Context;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

public class CenteredSeekBar extends AppCompatSeekBar {

    public CenteredSeekBar(Context context) {
        this(context, null);
    }

    public CenteredSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CenteredSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();
                layoutParams.bottomMargin -= (getHeight() / 2);
                setLayoutParams(layoutParams);

                getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }
}
