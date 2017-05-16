package james.metronome.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.AestheticActivity;

import java.util.Locale;

import james.metronome.R;
import james.metronome.utils.WhileHeldListener;
import james.metronome.views.MetronomeView;
import james.metronome.views.ThemesView;
import james.metronome.views.TicksView;
import rx.Subscription;
import rx.functions.Action1;

public class MainActivity extends AestheticActivity implements Runnable, TicksView.OnTickChangedListener {

    public static final String PREF_TICK = "tick";
    public static final String PREF_INTERVAL = "interval";

    private SoundPool soundPool;
    private Handler handler;
    private int soundId = -1;
    private boolean isPlaying;

    private SharedPreferences prefs;
    private int bpm;
    private long interval;

    private MetronomeView metronomeView;
    private ImageView playView;
    private TextView bpmView;
    private ImageView aboutView;
    private ImageView lessView;
    private ImageView moreView;
    private TicksView ticksView;

    private Subscription colorBackgroundSubscription;
    private Subscription textColorPrimarySubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (Aesthetic.isFirstTime())
            ThemesView.themes[0].apply(this);

        metronomeView = (MetronomeView) findViewById(R.id.metronome);
        playView = (ImageView) findViewById(R.id.play);
        bpmView = (TextView) findViewById(R.id.bpm);
        lessView = (ImageView) findViewById(R.id.less);
        moreView = (ImageView) findViewById(R.id.more);
        ticksView = (TicksView) findViewById(R.id.ticks);
        aboutView = (ImageView) findViewById(R.id.about);

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
        ticksView.setTick(tick);

        interval = prefs.getLong(PREF_INTERVAL, 500);
        bpm = toBpm(interval);
        metronomeView.setInterval(interval);
        bpmView.setText(String.format(Locale.getDefault(), getString(R.string.bpm), String.valueOf(bpm)));

        handler = new Handler();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        playView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlaying)
                    pause();
                else play();
            }
        });

        aboutView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
        });

        moreView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bpm < 300)
                    setBpm(++bpm);
            }
        });

        moreView.setOnTouchListener(new WhileHeldListener() {
            @Override
            public void onHeld() {
                if (bpm < 300)
                    setBpm(++bpm);
            }
        });

        lessView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bpm > 1)
                    setBpm(--bpm);
            }
        });

        lessView.setOnTouchListener(new WhileHeldListener() {
            @Override
            public void onHeld() {
                if (bpm > 1)
                    setBpm(--bpm);
            }
        });

        ticksView.setListener(this);
        subscribe();
    }

    private static int toBpm(long interval) {
        return (int) (60000 / interval);
    }

    private static long toInterval(int bpm) {
        return (long) 60000 / bpm;
    }

    private void play() {
        handler.post(this);
        playView.setImageResource(R.drawable.ic_pause);
        isPlaying = true;
    }

    private void pause() {
        handler.removeCallbacks(this);
        playView.setImageResource(R.drawable.ic_play);
        isPlaying = false;
    }

    private void setBpm(int bpm) {
        interval = toInterval(bpm);
        metronomeView.setInterval(interval);
        bpmView.setText(String.format(Locale.getDefault(), getString(R.string.bpm), String.valueOf(bpm)));

        prefs.edit().putLong(PREF_INTERVAL, interval).apply();
    }

    @Override
    public void onTickChanged(int tick) {
        soundId = TicksView.ticks[tick].getSoundId(MainActivity.this, soundPool);
        prefs.edit().putInt(MainActivity.PREF_TICK, tick).apply();

        if (!isPlaying)
            soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);
    }

    @Override
    public void onAboutViewColorChanged(int color) {
        aboutView.setColorFilter(color);
    }

    public void subscribe() {
        if (metronomeView != null && ticksView != null) {
            metronomeView.subscribe();
            ticksView.subscribe();
        }

        colorBackgroundSubscription = Aesthetic.get()
                .colorWindowBackground()
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        findViewById(R.id.topBar).setBackgroundColor(integer);
                        findViewById(R.id.bottomBar).setBackgroundColor(integer);
                    }
                });

        textColorPrimarySubscription = Aesthetic.get()
                .textColorPrimary()
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        playView.setColorFilter(integer);
                        moreView.setColorFilter(integer);
                        lessView.setColorFilter(integer);
                        aboutView.setColorFilter(integer);
                    }
                });
    }

    public void unsubscribe() {
        if (metronomeView != null && ticksView != null) {
            metronomeView.unsubscribe();
            ticksView.unsubscribe();
        }

        colorBackgroundSubscription.unsubscribe();
        textColorPrimarySubscription.unsubscribe();
    }

    @Override
    protected void onResume() {
        super.onResume();
        subscribe();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unsubscribe();
    }

    @Override
    protected void onStop() {
        pause();
        super.onStop();
    }

    @Override
    public void run() {
        if (isPlaying) {
            soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);
            handler.postDelayed(this, interval);
            metronomeView.onTick();
        }
    }
}
