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

import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;

import com.afollestad.aesthetic.Aesthetic;

import io.reactivex.disposables.Disposable;
import james.metronome.R;
import james.metronome.data.ThemeData;

public class ThemesView extends LinearLayout {

    public static final ThemeData[] themes = new ThemeData[]{
            //new ThemeData(R.string.title_theme_light, R.color.colorPrimary, R.color.colorAccent, R.color.colorBackground),
            //new ThemeData(R.string.title_theme_wood, R.color.colorPrimaryWood, R.color.colorAccentWood, R.color.colorPrimaryWood),
            //new ThemeData(R.string.title_theme_dark, R.color.colorPrimaryDark, R.color.colorAccentDark, R.color.colorBackgroundDark),
            //new ThemeData(R.string.title_theme_amoled, R.color.colorPrimaryAmoled, R.color.colorAccentAmoled, R.color.colorPrimaryAmoled)
    };

    private OnThemeChangedListener listener;

    private boolean isExpanded;
    private int theme;

    private Integer colorAccent;
    private Integer textColorPrimary;
    private Integer textColorPrimaryInverse;

    private Disposable colorAccentSubscription;
    private Disposable textColorPrimarySubscription;
    private Disposable textColorPrimaryInverseSubscription;

    public ThemesView(Context context) {
        this(context, null);
    }

    public ThemesView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThemesView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < themes.length; i++) {
            View v = inflater.inflate(R.layout.item_theme, this, false);
            v.setTag(i);
            v.setOnClickListener(v1 -> {
                final int position = (int) v1.getTag();
                if (isExpanded) {
                    if (theme != position && listener != null) {
                        theme = position;
                        themes[theme].apply(getContext());
                        listener.onThemeChanged(position);
                    }

                    isExpanded = false;
                    for (int i1 = 0; i1 < getChildCount(); i1++) {
                        final View view = getChildAt(i1);

                        if (view.findViewById(R.id.background).getAlpha() == 1) {
                            ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), textColorPrimaryInverse, textColorPrimary);
                            animator.addUpdateListener(animation -> {
                                view.findViewById(R.id.background).setAlpha(1 - animation.getAnimatedFraction());
                                ((TextView) view.findViewById(R.id.name)).setTextColor((int) animation.getAnimatedValue());
                            });
                            animator.start();

                            ValueAnimator animator2 = ValueAnimator.ofObject(new ArgbEvaluator(), textColorPrimaryInverse, colorAccent);
                            animator2.addUpdateListener(animation -> ((ImageView) view.findViewById(R.id.image)).setColorFilter((int) animation.getAnimatedValue()));
                            animator2.start();
                        }

                        if (theme != i1)
                            view.setVisibility(View.GONE);
                        else
                            ((ImageView) view.findViewById(R.id.image)).setImageResource(R.drawable.ic_expand);
                    }
                } else {
                    isExpanded = true;
                    for (int i1 = 0; i1 < getChildCount(); i1++) {
                        final View view = getChildAt(i1);
                        view.setVisibility(View.VISIBLE);
                        ((ImageView) view.findViewById(R.id.image)).setImageResource(R.drawable.ic_theme);
                        if (theme == i1) {
                            ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), textColorPrimary, textColorPrimaryInverse);
                            animator.addUpdateListener(animation -> {
                                view.findViewById(R.id.background).setAlpha(animation.getAnimatedFraction());
                                ((TextView) view.findViewById(R.id.name)).setTextColor((int) animation.getAnimatedValue());
                            });
                            animator.start();

                            ValueAnimator animator2 = ValueAnimator.ofObject(new ArgbEvaluator(), colorAccent, textColorPrimaryInverse);
                            animator2.addUpdateListener(animation -> ((ImageView) view.findViewById(R.id.image)).setColorFilter((int) animation.getAnimatedValue()));
                            animator2.start();
                        }
                    }
                }
            });

            ((TextView) v.findViewById(R.id.name)).setText(themes[i].getName(getContext()));
            if (i != theme)
                v.setVisibility(View.GONE);

            addView(v);
        }
    }

    public void subscribe() {
        colorAccentSubscription = Aesthetic.Companion.get()
                .colorAccent()
                .subscribe(integer -> {
                    colorAccent = integer;
                    for (int i = 0; i < getChildCount(); i++) {
                        View v = getChildAt(i);
                        v.findViewById(R.id.background).setBackgroundColor(integer);
                        if (!isExpanded || theme != i)
                            ((ImageView) v.findViewById(R.id.image)).setColorFilter(integer);
                    }
                });

        textColorPrimarySubscription = Aesthetic.Companion.get()
                .textColorPrimary()
                .subscribe(integer -> {
                    textColorPrimary = integer;
                    DrawableCompat.setTint(getBackground(), integer);
                    for (int i = 0; i < getChildCount(); i++) {
                        View v = getChildAt(i);
                        if (!isExpanded || theme != i)
                            ((TextView) v.findViewById(R.id.name)).setTextColor(integer);
                    }
                });

        textColorPrimaryInverseSubscription = Aesthetic.Companion.get()
                .textColorPrimaryInverse()
                .subscribe(integer -> {
                    textColorPrimaryInverse = integer;
                    for (int i = 0; i < getChildCount(); i++) {
                        View v = getChildAt(i);
                        if (isExpanded && theme == i) {
                            ((TextView) v.findViewById(R.id.name)).setTextColor(integer);
                            ((ImageView) v.findViewById(R.id.image)).setColorFilter(integer);
                        }
                    }
                });
    }

    public void unsubscribe() {
        colorAccentSubscription.dispose();
        textColorPrimarySubscription.dispose();
        textColorPrimaryInverseSubscription.dispose();
    }

    public void setListener(OnThemeChangedListener listener) {
        this.listener = listener;
    }

    public void setTheme(int theme) {
        this.theme = theme;
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setVisibility(i == theme ? View.VISIBLE : View.GONE);
        }
    }

    public interface OnThemeChangedListener {
        void onThemeChanged(int theme);
    }

}
