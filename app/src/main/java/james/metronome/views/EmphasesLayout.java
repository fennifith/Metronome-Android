package james.metronome.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import james.metronome.utils.ConversionUtils;

public class EmphasesLayout extends LinearLayout {

    public EmphasesLayout(Context context) {
        super(context, null);
    }

    public EmphasesLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public EmphasesLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = ConversionUtils.getPixelsFromDp(40) * getChildCount(), height = getMeasuredHeight();
        setMeasuredDimension(width, height);

        HorizontalScrollView.LayoutParams layoutParams = (HorizontalScrollView.LayoutParams) getLayoutParams();
        if (layoutParams != null && getParent() != null && getParent() instanceof View) {
            if (((View) getParent()).getMeasuredWidth() > width)
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
            else layoutParams.gravity = GravityCompat.START;
            setLayoutParams(layoutParams);
        }
    }
}
