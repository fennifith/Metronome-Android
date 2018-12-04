package james.metronome.views;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.aesthetic.Aesthetic;

import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import james.metronome.R;
import james.metronome.data.TickData;

public class TicksView extends LinearLayout {

    public static final TickData[] ticks = new TickData[]{
            new TickData(R.string.title_beep, R.raw.beep),
            new TickData(R.string.title_click, R.raw.click),
            new TickData(R.string.title_ding, R.raw.ding),
            new TickData(R.string.title_wood, R.raw.wood),
            new TickData(R.string.title_vibrate)
    };
    private OnTickChangedListener listener;

    private boolean isExpanded;
    private int tick;

    private Integer colorAccent;
    private Integer textColorPrimary;
    private Integer textColorPrimaryInverse;
    private int aboutViewColor;

    private Disposable colorAccentSubscription;
    private Disposable textColorPrimarySubscription;
    private Disposable textColorPrimaryInverseSubscription;

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
                            else
                                ((ImageView) view.findViewById(R.id.image)).setImageResource(R.drawable.ic_expand);
                        }
                    } else {
                        isExpanded = true;
                        for (int i = 0; i < getChildCount(); i++) {
                            final View view = getChildAt(i);
                            view.setVisibility(View.VISIBLE);
                            ((ImageView) view.findViewById(R.id.image)).setImageResource(R.drawable.ic_note);
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
        colorAccentSubscription = Aesthetic.Companion.get()
                .colorAccent()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(@NonNull Integer integer) throws Exception {
                        colorAccent = integer;
                        for (int i = 0; i < getChildCount(); i++) {
                            View v = getChildAt(i);
                            v.findViewById(R.id.background).setBackgroundColor(integer);
                            if (!isExpanded || tick != i)
                                ((ImageView) v.findViewById(R.id.image)).setColorFilter(integer);
                        }
                    }
                });

        textColorPrimarySubscription = Aesthetic.Companion.get()
                .textColorPrimary()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(@NonNull Integer integer) throws Exception {
                        textColorPrimary = integer;
                        DrawableCompat.setTint(getBackground(), integer);
                        for (int i = 0; i < getChildCount(); i++) {
                            View v = getChildAt(i);
                            if (!isExpanded || tick != i)
                                ((TextView) v.findViewById(R.id.name)).setTextColor(integer);
                        }
                    }
                });

        textColorPrimaryInverseSubscription = Aesthetic.Companion.get()
                .textColorPrimaryInverse()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(@NonNull Integer integer) throws Exception {
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
        colorAccentSubscription.dispose();
        textColorPrimarySubscription.dispose();
        textColorPrimaryInverseSubscription.dispose();
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
