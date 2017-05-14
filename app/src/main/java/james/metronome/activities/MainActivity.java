package james.metronome.activities;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

import james.metronome.R;
import james.metronome.data.TickData;
import james.metronome.utils.WhileHeldListener;
import james.metronome.views.MetronomeView;

public class MainActivity extends AppCompatActivity implements Runnable {

    public static final String PREF_TICK = "tick";
    public static final String PREF_INTERVAL = "interval";

    public static final TickData[] ticks = new TickData[]{
            new TickData(R.string.title_beep, R.raw.beep),
            new TickData(R.string.title_click, R.raw.click),
            new TickData(R.string.title_ding, R.raw.ding),
            new TickData(R.string.title_wood, R.raw.wood)
    };

    private SoundPool soundPool;
    private Handler handler;
    private int soundId = -1;
    private boolean isPlaying;
    private boolean isExpanded;

    private SharedPreferences prefs;
    private int tick;
    private int bpm;
    private long interval;

    private MetronomeView metronomeView;
    private ImageView playView;
    private TextView bpmView;
    private LinearLayout ticksLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        metronomeView = (MetronomeView) findViewById(R.id.metronome);
        playView = (ImageView) findViewById(R.id.play);
        bpmView = (TextView) findViewById(R.id.bpm);
        View lessView = findViewById(R.id.less);
        View moreView = findViewById(R.id.more);
        ticksLayout = (LinearLayout) findViewById(R.id.ticks);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(1)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .build();
        } else soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

        tick = prefs.getInt(PREF_TICK, 0);
        soundId = ticks[tick].getSoundId(this, soundPool);

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

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < ticks.length; i++) {
            View v = inflater.inflate(R.layout.item_tick, ticksLayout, false);
            v.setTag(i);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (int) v.getTag();
                    if (isExpanded) {
                        if (tick != position) {
                            tick = position;
                            soundId = ticks[tick].getSoundId(MainActivity.this, soundPool);
                            prefs.edit().putInt(PREF_TICK, tick).apply();

                            if (!isPlaying)
                                soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);
                        }

                        isExpanded = false;
                        for (int i = 0; i < ticksLayout.getChildCount(); i++) {
                            final View view = ticksLayout.getChildAt(i);

                            if (view.findViewById(R.id.background).getAlpha() == 1) {
                                ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), Color.WHITE, Color.BLACK);
                                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animation) {
                                        view.findViewById(R.id.background).setAlpha(1 - animation.getAnimatedFraction());
                                        ((TextView) view.findViewById(R.id.name)).setTextColor((int) animation.getAnimatedValue());
                                    }
                                });
                                animator.start();

                                ValueAnimator animator2 = ValueAnimator.ofObject(new ArgbEvaluator(), Color.WHITE, ContextCompat.getColor(MainActivity.this, R.color.colorAccent));
                                animator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animation) {
                                        ((ImageView) view.findViewById(R.id.image)).setColorFilter((int) animation.getAnimatedValue());
                                    }
                                });
                                animator2.start();
                            }

                            if (tick != i)
                                view.setVisibility(View.GONE);
                        }
                    } else {
                        isExpanded = true;
                        for (int i = 0; i < ticksLayout.getChildCount(); i++) {
                            final View view = ticksLayout.getChildAt(i);
                            view.setVisibility(View.VISIBLE);
                            if (tick == i) {
                                ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), Color.BLACK, Color.WHITE);
                                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animation) {
                                        view.findViewById(R.id.background).setAlpha(animation.getAnimatedFraction());
                                        ((TextView) view.findViewById(R.id.name)).setTextColor((int) animation.getAnimatedValue());
                                    }
                                });
                                animator.start();

                                ValueAnimator animator2 = ValueAnimator.ofObject(new ArgbEvaluator(), ContextCompat.getColor(MainActivity.this, R.color.colorAccent), Color.WHITE);
                                animator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animation) {
                                        ((ImageView) view.findViewById(R.id.image)).setColorFilter((int) animation.getAnimatedValue());
                                    }
                                });
                                animator2.start();
                            }
                        }
                    }
                }
            });

            ((TextView) v.findViewById(R.id.name)).setText(ticks[i].getName(this));
            if (i != tick)
                v.setVisibility(View.GONE);

            ticksLayout.addView(v);
        }
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
    protected void onStart() {
        super.onStart();
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
