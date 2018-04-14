package james.metronome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends WearableActivity implements Runnable {

    public static final String PREF_VIBRATION = "vibration";
    public static final String PREF_INTERVAL = "interval";

    private View container;
    private ImageView vibrationView;
    private ImageView playView;
    private TextView bpmView;
    private SeekBar seekBar;

    private PowerManager.WakeLock wakeLock;

    private SharedPreferences prefs;
    private int bpm;
    private long interval;

    private SoundPool soundPool;
    private Handler handler;
    private int soundId = -1;
    private boolean isPlaying;
    private boolean isVibration;

    private Vibrator vibrator;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "idiot.metronome.MainActivity");
            wakeLock.acquire();
        }

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

        isVibration = prefs.getBoolean(PREF_VIBRATION, true);
        if (!isVibration)
            soundId = soundPool.load(this, R.raw.click, 1);

        interval = prefs.getLong(PREF_INTERVAL, 500);
        bpm = Utils.toBpm(interval);

        handler = new Handler();

        container = findViewById(R.id.container);
        vibrationView = findViewById(R.id.vibration);
        playView = findViewById(R.id.play);
        bpmView = findViewById(R.id.bpm);
        seekBar = findViewById(R.id.seekBar);

        vibrationView.setImageResource(isVibration ? R.drawable.ic_vibration : R.drawable.ic_sound);
        playView.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        bpmView.setText(String.format(Locale.getDefault(), getString(R.string.bpm), String.valueOf(bpm)));
        seekBar.setProgress(bpm);

        findViewById(R.id.vibrationButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isVibration = !isVibration;
                prefs.edit().putBoolean(PREF_VIBRATION, isVibration).apply();

                if (!isVibration)
                    soundId = soundPool.load(MainActivity.this, R.raw.click, 1);
                else soundId = -1;

                vibrationView.setImageResource(isVibration ? R.drawable.ic_vibration : R.drawable.ic_sound);
            }
        });

        findViewById(R.id.playButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isPlaying = !isPlaying;
                if (isPlaying) {
                    handler.post(MainActivity.this);
                    playView.setImageResource(R.drawable.ic_pause);
                } else {
                    handler.removeCallbacks(MainActivity.this);
                    playView.setImageResource(R.drawable.ic_play);
                }
            }
        });

        findViewById(R.id.moreButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (seekBar.getProgress() < 300)
                    seekBar.setProgress(seekBar.getProgress() + 1);
            }
        });

        findViewById(R.id.lessButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (seekBar.getProgress() > 1)
                    seekBar.setProgress(seekBar.getProgress() - 1);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                bpm = i;
                interval = Utils.toInterval(bpm);
                bpmView.setText(String.format(Locale.getDefault(), getString(R.string.bpm), String.valueOf(bpm)));
                prefs.edit().putLong(PREF_INTERVAL, interval).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPlaying = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(this);
        if (wakeLock != null)
            wakeLock.release();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient())
            container.setBackgroundColor(Color.BLACK);
        else container.setBackground(null);
    }

    @Override
    public void run() {
        if (isPlaying) {
            handler.postDelayed(this, interval);

            if (soundId != -1)
                soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);
            else if (Build.VERSION.SDK_INT >= 26)
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            else vibrator.vibrate(50);
        }
    }
}
