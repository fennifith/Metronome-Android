package james.metronome.data;

import android.content.Context;
import android.media.SoundPool;
import android.support.annotation.RawRes;

public class TickData {

    private String name;
    private int soundRes;

    public TickData(String name, @RawRes int soundRes) {
        this.name = name;
        this.soundRes = soundRes;
    }

    public String getName() {
        return name;
    }

    @RawRes
    public int getSoundRes() {
        return soundRes;
    }

    public int getSoundId(Context context, SoundPool pool) {
        return pool.load(context, getSoundRes(), 1);
    }

}
