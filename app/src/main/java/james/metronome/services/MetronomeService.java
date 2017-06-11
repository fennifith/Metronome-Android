package james.metronome.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import james.metronome.R;
import james.metronome.views.TicksView;

public class MetronomeService extends Service implements Runnable {

    public static final String ACTION_PAUSE = "james.metronome.ACTION_PAUSE";

    public static final String PREF_TICK = "tick";
    public static final String PREF_INTERVAL = "interval";

    private final IBinder binder = new LocalBinder();

    private SharedPreferences prefs;
    private int bpm;
    private long interval;

    private SoundPool soundPool;
    private Handler handler;
    private int soundId = -1;
    private boolean isPlaying;

    private TickListener listener;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(1)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .build();
        } else soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

        int tick = prefs.getInt(PREF_TICK, 0);
        soundId = TicksView.ticks[tick].getSoundId(this, soundPool);

        interval = prefs.getLong(PREF_INTERVAL, 500);
        bpm = toBpm(interval);

        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PAUSE:
                    pause();
            }
        }
        return START_STICKY;
    }

    private static int toBpm(long interval) {
        return (int) (60000 / interval);
    }

    private static long toInterval(int bpm) {
        return (long) 60000 / bpm;
    }

    public void play() {
        handler.post(this);
        isPlaying = true;

        Intent intent = new Intent(this, MetronomeService.class);
        intent.setAction(ACTION_PAUSE);

        startForeground(530,
                new NotificationCompat.Builder(this)
                        .setContentTitle(getString(R.string.notification_title))
                        .setContentText(getString(R.string.notification_desc))
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentIntent(PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_ONE_SHOT))
                        .build()
        );

        if (listener != null)
            listener.onStartTicks();
    }

    public void pause() {
        handler.removeCallbacks(this);
        stopForeground(true);
        isPlaying = false;

        if (listener != null)
            listener.onStopTicks();
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
        interval = toInterval(bpm);
        prefs.edit().putLong(PREF_INTERVAL, interval).apply();
    }

    public void setTick(int tick) {
        soundId = TicksView.ticks[tick].getSoundId(this, soundPool);
        prefs.edit().putInt(PREF_TICK, tick).apply();

        if (!isPlaying)
            soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public long getInterval() {
        return interval;
    }

    public int getBpm() {
        return bpm;
    }

    public int getTick() {
        return prefs.getInt(PREF_TICK, 0);
    }

    public void setTickListener(TickListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        listener = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(this);
        super.onDestroy();
    }

    @Override
    public void run() {
        if (isPlaying) {
            soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);
            handler.postDelayed(this, interval);
            if (listener != null)
                listener.onTick();
        }
    }

    public class LocalBinder extends Binder {
        public MetronomeService getService() {
            return MetronomeService.this;
        }
    }

    public interface TickListener {
        void onStartTicks();

        void onTick();

        void onStopTicks();
    }
}
