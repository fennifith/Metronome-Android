package james.metronome.data;

import android.content.Context;
import android.media.SoundPool;
import android.support.annotation.RawRes;
import android.support.annotation.StringRes;

public class TickData {

    private int name;
    private int soundRes;

    public TickData(@StringRes int name, @RawRes int soundRes) {
        this.name = name;
        this.soundRes = soundRes;
    }

    public String getName(Context context) {
        return context.getString(name);
    }

    @RawRes
    public int getSoundRes() {
        return soundRes;
    }

    public int getSoundId(Context context, SoundPool pool) {
        return pool.load(context, getSoundRes(), 1);
    }

}
