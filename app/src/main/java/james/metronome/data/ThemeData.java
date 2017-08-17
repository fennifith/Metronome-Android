package james.metronome.data;

import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;

import com.afollestad.aesthetic.Aesthetic;

import james.metronome.R;
import james.metronome.utils.ColorUtils;

public class ThemeData {

    public int nameRes;
    public int colorPrimaryRes;
    public int colorAccentRes;
    public int colorBackgroundRes;

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

        Aesthetic.get()
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
