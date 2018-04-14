package james.metronome.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import james.metronome.R;
import james.metronome.views.TicksView;

public class MetronomeService extends Service implements Runnable {

    public static final String ACTION_START = "james.metronome.ACTION_START";
    public static final String ACTION_PAUSE = "james.metronome.ACTION_PAUSE";
    public static final String EXTRA_BPM = "james.metronome.EXTRA_BPM";

    public static final String PREF_TICK = "tick";
    public static final String PREF_INTERVAL = "interval";
    public static final String PREF_EMPHASIS_SIZE = "emphasisSize";
    public static final String PREF_EMPHASIS = "emphasis";

    private final IBinder binder = new LocalBinder();

    private SharedPreferences prefs;
    private int bpm;
    private long interval;

    private SoundPool soundPool;
    private Handler handler;
    private int soundId = -1;
    private boolean isPlaying;

    private List<Boolean> emphasisList = new ArrayList<>(Arrays.asList(new Boolean[]{true, true, true, true}));
    private int emphasisIndex;

    private Vibrator vibrator;

    private TickListener listener;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(1)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .build();
        } else soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

        int tick = prefs.getInt(PREF_TICK, 0);
        if (!TicksView.ticks[tick].isVibration())
            soundId = TicksView.ticks[tick].getSoundId(this, soundPool);

        interval = prefs.getLong(PREF_INTERVAL, 500);
        bpm = toBpm(interval);

        emphasisList = new ArrayList<>();
        int emphasisSize = prefs.getInt(PREF_EMPHASIS_SIZE, 4);
        for (int i = 0; i < emphasisSize; i++) {
            emphasisList.add(prefs.getBoolean(PREF_EMPHASIS + i, false));
        }

        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_START:
                    setBpm(intent.getIntExtra(EXTRA_BPM, bpm));
                    pause();
                    play();
                    break;
                case ACTION_PAUSE:
                    pause();
                    break;
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
        emphasisIndex = 0;

        Intent intent = new Intent(this, MetronomeService.class);
        intent.setAction(ACTION_PAUSE);

        NotificationCompat.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(new NotificationChannel("metronome", getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT));

            builder = new NotificationCompat.Builder(this, "metronome");
        } else
            builder = new NotificationCompat.Builder(this);

        startForeground(530,
                builder.setContentTitle(getString(R.string.notification_title))
                        .setContentText(getString(R.string.notification_desc))
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentIntent(PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_ONE_SHOT))
                        .setPriority(NotificationCompat.PRIORITY_LOW)
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

    public void setEmphasisList(List<Boolean> emphasisList) {
        this.emphasisList = emphasisList;
        emphasisIndex = 0;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_EMPHASIS_SIZE, emphasisList.size());
        for (int i = 0; i < emphasisList.size(); i++) {
            editor.putBoolean(PREF_EMPHASIS + i, emphasisList.get(i));
        }
        editor.apply();
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
        interval = toInterval(bpm);
        prefs.edit().putLong(PREF_INTERVAL, interval).apply();
        if (listener != null)
            listener.onBpmChanged(bpm);
    }

    public void setTick(int tick) {
        if (!TicksView.ticks[tick].isVibration()) {
            soundId = TicksView.ticks[tick].getSoundId(this, soundPool);
            if (!isPlaying)
                soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);
        } else soundId = -1;

        prefs.edit().putInt(PREF_TICK, tick).apply();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public long getInterval() {
        return interval;
    }

    public List<Boolean> getEmphasisList() {
        return emphasisList;
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
            handler.postDelayed(this, interval);

            if (emphasisIndex >= emphasisList.size())
                emphasisIndex = 0;
            boolean isEmphasis = emphasisList.get(emphasisIndex);
            if (listener != null)
                listener.onTick(isEmphasis, emphasisIndex);
            emphasisIndex++;

            if (soundId != -1)
                soundPool.play(soundId, 1, 1, 0, 0, isEmphasis ? 1.5f : 1);
            else if (Build.VERSION.SDK_INT >= 26)
                vibrator.vibrate(VibrationEffect.createOneShot(isEmphasis ? 50 : 20, VibrationEffect.DEFAULT_AMPLITUDE));
            else vibrator.vibrate(isEmphasis ? 50 : 20);
        }
    }

    public class LocalBinder extends Binder {
        public MetronomeService getService() {
            return MetronomeService.this;
        }
    }

    public interface TickListener {
        void onStartTicks();

        void onTick(boolean isEmphasis, int index);

        void onBpmChanged(int bpm);

        void onStopTicks();
    }
}
