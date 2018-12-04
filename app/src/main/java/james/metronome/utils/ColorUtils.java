package james.metronome.utils;

import android.graphics.Color;

import androidx.annotation.ColorInt;

public class ColorUtils {

    @ColorInt
    public static int getDarkColor(@ColorInt int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }

    @ColorInt
    public static int getMixedColor(@ColorInt int color1, @ColorInt int color2) {
        return Color.rgb(
                (Color.red(color1) + Color.red(color1) + Color.red(color2)) / 3,
                (Color.green(color1) + Color.green(color1) + Color.green(color2)) / 3,
                (Color.blue(color1) + Color.blue(color1) + Color.blue(color2)) / 3
        );
    }

}
