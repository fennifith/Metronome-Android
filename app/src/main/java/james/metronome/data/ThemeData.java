package james.metronome.data;

import android.content.Context;

import com.afollestad.aesthetic.Aesthetic;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import james.metronome.R;
import me.jfenn.androidutils.ColorUtils;

public class ThemeData {

    private int nameRes;
    private int colorPrimaryRes;
    private int colorAccentRes;
    private int colorBackgroundRes;

    public ThemeData(@StringRes int nameRes, @ColorRes int colorPrimaryRes, @ColorRes int colorAccentRes, @ColorRes int colorBackgroundRes) {
        this.nameRes = nameRes;
        this.colorPrimaryRes = colorPrimaryRes;
        this.colorAccentRes = colorAccentRes;
        this.colorBackgroundRes = colorBackgroundRes;
    }

    public String getName(Context context) {
        return context.getString(nameRes);
    }

    public void apply(Context context) {
        int backgroundColor = ContextCompat.getColor(context, colorBackgroundRes);
        boolean isBackgroundDark = ColorUtils.isColorDark(backgroundColor);

        Aesthetic.Companion.get()
                .colorPrimary(ContextCompat.getColor(context, colorPrimaryRes))
                .colorAccent(ContextCompat.getColor(context, colorAccentRes))
                .colorWindowBackground(backgroundColor)
                .textColorPrimary(ContextCompat.getColor(context, isBackgroundDark ? R.color.textColorPrimary : R.color.textColorPrimaryInverse))
                .textColorPrimaryInverse(ContextCompat.getColor(context, isBackgroundDark ? R.color.textColorPrimaryInverse : R.color.textColorPrimary))
                .textColorSecondary(ContextCompat.getColor(context, isBackgroundDark ? R.color.textColorSecondary : R.color.textColorSecondaryInverse))
                .textColorSecondaryInverse(ContextCompat.getColor(context, isBackgroundDark ? R.color.textColorSecondaryInverse : R.color.textColorSecondary))
                .colorStatusBarAuto()
                .colorNavigationBarAuto()
                .apply();
    }

}
