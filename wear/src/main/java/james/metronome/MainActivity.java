package james.metronome;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends WearableActivity implements ServiceConnection, MetronomeService.TickListener {

    private BoxInsetLayout container;
    private ImageView vibrationView;
    private ImageView playView;
    private TextView bpmView;
    private SeekBar seekBar;

    private MetronomeService service;
    private boolean isBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(james.metronome.wear.R.layout.activity_main);
        setAmbientEnabled();

        container = findViewById(james.metronome.wear.R.id.container);
        vibrationView = findViewById(james.metronome.wear.R.id.vibration);
        playView = findViewById(james.metronome.wear.R.id.play);
        bpmView = findViewById(james.metronome.wear.R.id.bpm);
        seekBar = findViewById(james.metronome.wear.R.id.seekBar);

        if (isBound()) {
            vibrationView.setImageResource(service.isVibration() ? james.metronome.wear.R.drawable.ic_vibration : james.metronome.wear.R.drawable.ic_sound);
            playView.setImageResource(service.isPlaying() ? james.metronome.wear.R.drawable.ic_pause : james.metronome.wear.R.drawable.ic_play);
            bpmView.setText(String.format(Locale.getDefault(), getString(james.metronome.wear.R.string.bpm), String.valueOf(service.getBpm())));
            seekBar.setProgress(service.getBpm());
        }

        vibrationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                service.setVibration(!service.isVibration());
                vibrationView.setImageResource(service.isVibration() ? james.metronome.wear.R.drawable.ic_vibration : james.metronome.wear.R.drawable.ic_sound);
            }
        });

        playView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isBound()) {
                    if (service.isPlaying())
                        service.pause();
                    else service.play();
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setBpm(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void setBpm(int bpm) {
        if (isBound()) {
            service.setBpm(bpm);
            bpmView.setText(String.format(Locale.getDefault(), getString(james.metronome.wear.R.string.bpm), String.valueOf(bpm)));
        }
    }

    private boolean isBound() {
        return isBound && service != null;
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

        if (vibrationView != null)
            vibrationView.setImageResource(service.isVibration() ? james.metronome.wear.R.drawable.ic_vibration : james.metronome.wear.R.drawable.ic_sound);

        if (playView != null)
            playView.setImageResource(service.isPlaying() ? james.metronome.wear.R.drawable.ic_pause : james.metronome.wear.R.drawable.ic_play);

        if (bpmView != null)
            bpmView.setText(String.format(Locale.getDefault(), getString(james.metronome.wear.R.string.bpm), String.valueOf(service.getBpm())));

        if (seekBar != null)
            seekBar.setProgress(service.getBpm());
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        isBound = false;
    }

    @Override
    public void onStartTicks() {
        playView.setImageResource(james.metronome.wear.R.drawable.ic_pause);
    }

    @Override
    public void onStopTicks() {
        playView.setImageResource(james.metronome.wear.R.drawable.ic_play);
    }
}
