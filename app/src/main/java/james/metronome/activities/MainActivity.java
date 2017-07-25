package james.metronome.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.AestheticActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import james.metronome.R;
import james.metronome.services.MetronomeService;
import james.metronome.utils.WhileHeldListener;
import james.metronome.views.EmphasisSwitch;
import james.metronome.views.MetronomeView;
import james.metronome.views.ThemesView;
import james.metronome.views.TicksView;

public class MainActivity extends AestheticActivity implements TicksView.OnTickChangedListener, ServiceConnection, MetronomeService.TickListener, EmphasisSwitch.OnCheckedChangeListener {

    private boolean isBound;
    private MetronomeService service;

    private MetronomeView metronomeView;
    private ImageView playView;
    private LinearLayout emphasisLayout;
    private TextView bpmView;
    private ImageView aboutView;
    private ImageView lessView;
    private ImageView moreView;
    private ImageView addEmphasisView;
    private ImageView removeEmphasisView;
    private TicksView ticksView;
    private SeekBar seekBar;

    private Disposable colorBackgroundSubscription;
    private Disposable textColorPrimarySubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Aesthetic.isFirstTime())
            ThemesView.themes[0].apply(this);

        metronomeView = findViewById(R.id.metronome);
        playView = findViewById(R.id.play);
        emphasisLayout = findViewById(R.id.emphasis);
        addEmphasisView = findViewById(R.id.add);
        removeEmphasisView = findViewById(R.id.remove);
        bpmView = findViewById(R.id.bpm);
        lessView = findViewById(R.id.less);
        moreView = findViewById(R.id.more);
        ticksView = findViewById(R.id.ticks);
        aboutView = findViewById(R.id.about);
        seekBar = findViewById(R.id.seekBar);

        seekBar.setPadding(0, 0, 0, 0);

        if (isBound()) {
            ticksView.setTick(service.getTick());
            metronomeView.setInterval(service.getInterval());
            seekBar.setProgress(service.getBpm());
            bpmView.setText(String.format(Locale.getDefault(), getString(R.string.bpm), String.valueOf(service.getBpm())));
            playView.setImageResource(service.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
            emphasisLayout.removeAllViews();
            for (boolean isEmphasis : service.getEmphasisList()) {
                emphasisLayout.addView(getEmphasisSwitch(isEmphasis, false));
            }
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        playView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound()) {
                    if (service.isPlaying())
                        service.pause();
                    else service.play();
                }
            }
        });

        aboutView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
        });

        addEmphasisView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isBound()) {
                    if (service.getEmphasisList().size() < 6) {
                        emphasisLayout.addView(getEmphasisSwitch(false, true));

                        List<Boolean> emphasisList = service.getEmphasisList();
                        emphasisList.add(false);
                        service.setEmphasisList(emphasisList);
                    }
                }
            }
        });

        removeEmphasisView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isBound()) {
                    if (service.getEmphasisList().size() > 2) {
                        List<Boolean> emphasisList = service.getEmphasisList();
                        int position = emphasisList.size() - 1;
                        emphasisList.remove(position);
                        service.setEmphasisList(emphasisList);

                        emphasisLayout.removeViewAt(position);
                    }
                }
            }
        });

        moreView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound() && service.getBpm() < 300)
                    seekBar.setProgress(service.getBpm() + 1);
            }
        });

        moreView.setOnTouchListener(new WhileHeldListener() {
            @Override
            public void onHeld() {
                if (isBound() && service.getBpm() < 300)
                    seekBar.setProgress(service.getBpm() + 1);
            }
        });

        lessView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound() && service.getBpm() > 1)
                    seekBar.setProgress(service.getBpm() - 1);
            }
        });

        lessView.setOnTouchListener(new WhileHeldListener() {
            @Override
            public void onHeld() {
                if (isBound() && service.getBpm() > 1)
                    seekBar.setProgress(service.getBpm() - 1);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress > 0)
                    setBpm(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        ticksView.setListener(this);
        subscribe();
    }

    private void setBpm(int bpm) {
        if (isBound()) {
            service.setBpm(bpm);
            metronomeView.setInterval(service.getInterval());
            bpmView.setText(String.format(Locale.getDefault(), getString(R.string.bpm), String.valueOf(bpm)));
        }
    }

    private boolean isBound() {
        return isBound && service != null;
    }

    @Override
    public void onTickChanged(int tick) {
        if (isBound())
            service.setTick(tick);
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

        if (emphasisLayout != null) {
            for (int i = 0; i < emphasisLayout.getChildCount(); i++) {
                ((EmphasisSwitch) emphasisLayout.getChildAt(i)).subscribe();
            }
        }

        colorBackgroundSubscription = Aesthetic.get()
                .colorWindowBackground()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(@NonNull Integer integer) throws Exception {
                        findViewById(R.id.topBar).setBackgroundColor(integer);
                        findViewById(R.id.bottomBar).setBackgroundColor(integer);
                    }
                });

        textColorPrimarySubscription = Aesthetic.get()
                .textColorPrimary()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(@NonNull Integer integer) throws Exception {
                        DrawableCompat.setTint(seekBar.getProgressDrawable(), integer);
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

        if (emphasisLayout != null) {
            for (int i = 0; i < emphasisLayout.getChildCount(); i++) {
                ((EmphasisSwitch) emphasisLayout.getChildAt(i)).unsubscribe();
            }
        }

        colorBackgroundSubscription.dispose();
        textColorPrimarySubscription.dispose();
    }

    private EmphasisSwitch getEmphasisSwitch(boolean isChecked, boolean subscribe) {
        EmphasisSwitch emphasisSwitch = new EmphasisSwitch(this);
        emphasisSwitch.setChecked(isChecked);
        emphasisSwitch.setOnCheckedChangeListener(this);

        if (subscribe)
            emphasisSwitch.subscribe();

        return emphasisSwitch;
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
    protected void onStart() {
        Intent intent = new Intent(this, MetronomeService.class);
        startService(intent);
        bindService(intent, this, Context.BIND_AUTO_CREATE);

        super.onStart();
    }

    @Override
    protected void onStop() {
        if (isBound) {
            unbindService(this);
            isBound = false;
        }
        super.onStop();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MetronomeService.LocalBinder binder = (MetronomeService.LocalBinder) iBinder;
        service = binder.getService();
        service.setTickListener(this);
        isBound = true;

        if (ticksView != null)
            ticksView.setTick(service.getTick());

        if (metronomeView != null)
            metronomeView.setInterval(service.getInterval());

        if (seekBar != null)
            seekBar.setProgress(service.getBpm());

        if (bpmView != null)
            bpmView.setText(String.format(Locale.getDefault(), getString(R.string.bpm), String.valueOf(service.getBpm())));

        if (playView != null)
            playView.setImageResource(service.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);

        if (emphasisLayout != null) {
            emphasisLayout.removeAllViews();
            for (boolean isEmphasis : service.getEmphasisList()) {
                emphasisLayout.addView(getEmphasisSwitch(isEmphasis, true));
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        isBound = false;
    }

    @Override
    public void onStartTicks() {
        playView.setImageResource(R.drawable.ic_pause);
    }

    @Override
    public void onTick(int index) {
        metronomeView.onTick();

        for (int i = 0; i < emphasisLayout.getChildCount(); i++) {
            ((EmphasisSwitch) emphasisLayout.getChildAt(i)).setOutlined(i == index);
        }
    }

    @Override
    public void onStopTicks() {
        playView.setImageResource(R.drawable.ic_play);

        for (int i = 0; i < emphasisLayout.getChildCount(); i++) {
            ((EmphasisSwitch) emphasisLayout.getChildAt(i)).setOutlined(false);
        }
    }

    @Override
    public void onCheckedChanged(EmphasisSwitch emphasisSwitch, boolean b) {
        if (isBound()) {
            List<Boolean> emphasisList = new ArrayList<>();
            for (int i = 0; i < emphasisLayout.getChildCount(); i++) {
                emphasisList.add(((EmphasisSwitch) emphasisLayout.getChildAt(i)).isChecked());
            }

            service.setEmphasisList(emphasisList);
        }
    }
}
