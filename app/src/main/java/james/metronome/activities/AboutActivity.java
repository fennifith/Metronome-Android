package james.metronome.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.AestheticActivity;

import james.metronome.R;
import james.metronome.views.ThemesView;
import rx.Subscription;
import rx.functions.Action1;

public class AboutActivity extends AestheticActivity implements ThemesView.OnThemeChangedListener {

    private static final String PREF_THEME = "theme";

    private Toolbar toolbar;
    private View iconView;
    private ThemesView themesView;
    private View buttonsView;
    private View librariesView;

    private Subscription textColorPrimarySubscription;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        iconView = findViewById(R.id.icon);
        themesView = (ThemesView) findViewById(R.id.themes);
        buttonsView = findViewById(R.id.buttons);
        View donateView = findViewById(R.id.donate);
        View githubView = findViewById(R.id.github);
        View playView = findViewById(R.id.play);
        librariesView = findViewById(R.id.libraries);
        View aesthetic = findViewById(R.id.aesthetic);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        themesView.setTheme(prefs.getInt(PREF_THEME, 0));
        themesView.setListener(this);

        donateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=james.donate")));
            }
        });

        githubView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/TheAndroidMaster/Metronome-Android")));
            }
        });

        playView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=james.metronome")));
            }
        });

        aesthetic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/afollestad/aesthetic")));
            }
        });

        subscribe();
    }

    private void subscribe() {
        if (themesView != null)
            themesView.subscribe();

        textColorPrimarySubscription = Aesthetic.get()
                .textColorPrimary()
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        if (toolbar != null && iconView != null && buttonsView != null && librariesView != null) {
                            toolbar.setTitleTextColor(integer);
                            DrawableCompat.setTint(toolbar.getBackground(), integer);
                            DrawableCompat.setTint(iconView.getBackground(), integer);
                            DrawableCompat.setTint(buttonsView.getBackground(), integer);
                            DrawableCompat.setTint(librariesView.getBackground(), integer);
                        }

                        ActionBar actionBar = getSupportActionBar();
                        if (actionBar != null) {
                            Drawable drawable = ContextCompat.getDrawable(AboutActivity.this, R.drawable.ic_back);
                            DrawableCompat.setTint(drawable, integer);
                            actionBar.setHomeAsUpIndicator(drawable);
                        }
                    }
                });
    }

    private void unsubscribe() {
        if (themesView != null)
            themesView.unsubscribe();

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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onThemeChanged(int theme) {
        prefs.edit().putInt(PREF_THEME, theme).apply();
    }
}
