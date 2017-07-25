package james.metronome.utils;

import android.content.res.Resources;

public class ConversionUtils {

    public static int getPixelsFromDp(float dp) {
        return (int) (Resources.getSystem().getDisplayMetrics().density * dp);
    }

    public static float getDpFromPixels(int pixels) {
        return pixels / Resources.getSystem().getDisplayMetrics().density;
    }

}
