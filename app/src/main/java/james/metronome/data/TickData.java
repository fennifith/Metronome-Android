package james.metronome.data;

import android.content.Context;
import android.media.SoundPool;
import android.support.annotation.RawRes;
import android.support.annotation.StringRes;

public class TickData {

    private int nameRes;
    private int soundRes;

    public TickData(@StringRes int nameRes, @RawRes int soundRes) {
        this.nameRes = nameRes;
        this.soundRes = soundRes;
    }

    public String getName(Context context) {
        return context.getString(nameRes);
    }

    @RawRes
    public int getSoundRes() {
        return soundRes;
    }

    public int getSoundId(Context context, SoundPool pool) {
        return pool.load(context, getSoundRes(), 1);
    }

}
