package james.metronome.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import james.metronome.services.MetronomeService;

public class DummyShortcutActivity extends AppCompatActivity {

    //this activity is stupid

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(getIntent().setClass(this, MetronomeService.class));
        finish();
    }
}
