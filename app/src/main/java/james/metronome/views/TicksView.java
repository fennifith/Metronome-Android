package james.metronome.views;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.aesthetic.Aesthetic;

import james.metronome.R;
import james.metronome.data.TickData;
import rx.Subscription;
import rx.functions.Action1;

public class TicksView extends LinearLayout {

    public static final TickData[] ticks = new TickData[]{
            new TickData(R.string.title_beep, R.raw.beep),
            new TickData(R.string.title_click, R.raw.click),
            new TickData(R.string.title_ding, R.raw.ding),
            new TickData(R.string.title_wood, R.raw.wood)
    };
    private OnTickChangedListener listener;

    private boolean isExpanded;
    private int tick;

    private Integer colorAccent;
    private Integer textColorPrimary;
    private Integer textColorPrimaryInverse;
    private int aboutViewColor;

    private Subscription colorAccentSubscription;
    private Subscription textColorPrimarySubscription;
    private Subscription textColorPrimaryInverseSubscription;

    public TicksView(Context context) {
        this(context, null);
    }

    public TicksView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TicksView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < ticks.length; i++) {
            View v = inflater.inflate(R.layout.item_tick, this, false);
            v.setTag(i);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int position = (int) v.getTag();
                    if (isExpanded) {
                        if (tick != position && listener != null) {
                            tick = position;
                            listener.onTickChanged(tick);
                        }

                        isExpanded = false;
                        for (int i = 0; i < getChildCount(); i++) {
                            final View view = getChildAt(i);

                            if (view.findViewById(R.id.background).getAlpha() == 1) {
                                ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), textColorPrimaryInverse, textColorPrimary);
                                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animation) {
                                        int color = (int) animation.getAnimatedValue();

                                        view.findViewById(R.id.background).setAlpha(1 - animation.getAnimatedFraction());
                                        ((TextView) view.findViewById(R.id.name)).setTextColor(color);
                                        if (aboutViewColor != textColorPrimary && listener != null) {
                                            listener.onAboutViewColorChanged(color);
                                            aboutViewColor = color;
                                        }
                                    }
                                });
                                animator.start();

                                ValueAnimator animator2 = ValueAnimator.ofObject(new ArgbEvaluator(), textColorPrimaryInverse, colorAccent);
                                animator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animation) {
                                        ((ImageView) view.findViewById(R.id.image)).setColorFilter((int) animation.getAnimatedValue());
                                    }
                                });
                                animator2.start();
                            }

                            if (tick != i)
                                view.setVisibility(View.GONE);
                        }
                    } else {
                        isExpanded = true;
                        for (int i = 0; i < getChildCount(); i++) {
                            final View view = getChildAt(i);
                            view.setVisibility(View.VISIBLE);
                            if (tick == i) {
                                ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), textColorPrimary, textColorPrimaryInverse);
                                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animation) {
                                        int color = (int) animation.getAnimatedValue();

                                        view.findViewById(R.id.background).setAlpha(animation.getAnimatedFraction());
                                        ((TextView) view.findViewById(R.id.name)).setTextColor(color);
                                        if (position == 0 && listener != null) {
                                            listener.onAboutViewColorChanged(color);
                                            aboutViewColor = color;
                                        }
                                    }
                                });
                                animator.start();

                                ValueAnimator animator2 = ValueAnimator.ofObject(new ArgbEvaluator(), colorAccent, textColorPrimaryInverse);
                                animator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animation) {
                                        ((ImageView) view.findViewById(R.id.image)).setColorFilter((int) animation.getAnimatedValue());
                                    }
                                });
                                animator2.start();
                            }
                        }
                    }
                }
            });

            ((TextView) v.findViewById(R.id.name)).setText(ticks[i].getName(getContext()));
            if (i != tick)
                v.setVisibility(View.GONE);

            addView(v);
        }
    }

    public void subscribe() {
        colorAccentSubscription = Aesthetic.get()
                .colorAccent()
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        colorAccent = integer;
                        for (int i = 0; i < getChildCount(); i++) {
                            View v = getChildAt(i);
                            v.findViewById(R.id.background).setBackgroundColor(integer);
                            if (!isExpanded || tick != i)
                                ((ImageView) v.findViewById(R.id.image)).setColorFilter(integer);
                        }
                    }
                });

        textColorPrimarySubscription = Aesthetic.get()
                .textColorPrimary()
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        textColorPrimary = integer;
                        for (int i = 0; i < getChildCount(); i++) {
                            View v = getChildAt(i);
                            if (!isExpanded || tick != i)
                                ((TextView) v.findViewById(R.id.name)).setTextColor(integer);
                        }
                    }
                });

        textColorPrimaryInverseSubscription = Aesthetic.get()
                .textColorPrimaryInverse()
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        textColorPrimaryInverse = integer;
                        for (int i = 0; i < getChildCount(); i++) {
                            View v = getChildAt(i);
                            if (isExpanded && tick == i) {
                                ((TextView) v.findViewById(R.id.name)).setTextColor(integer);
                                ((ImageView) v.findViewById(R.id.image)).setColorFilter(integer);
                            }
                        }
                    }
                });
    }

    public void unsubscribe() {
        colorAccentSubscription.unsubscribe();
        textColorPrimarySubscription.unsubscribe();
        textColorPrimaryInverseSubscription.unsubscribe();
    }

    public void setTick(int tick) {
        this.tick = tick;
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setVisibility(i == tick ? View.VISIBLE : View.GONE);
        }
    }

    public void setListener(OnTickChangedListener listener) {
        this.listener = listener;
    }

    public interface OnTickChangedListener {
        void onTickChanged(int tick);
        void onAboutViewColorChanged(int color);
    }

}
