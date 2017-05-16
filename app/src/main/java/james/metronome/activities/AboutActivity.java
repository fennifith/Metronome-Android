package james.metronome.activities;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.AestheticActivity;

import james.metronome.R;
import james.metronome.views.ThemesView;
import rx.Subscription;
import rx.functions.Action1;

public class AboutActivity extends AestheticActivity implements ThemesView.OnThemeChangedListener {

    private static final String PREF_THEME = "theme";

    private Toolbar toolbar;
    private ThemesView themesView;

    private Subscription textColorPrimarySubscription;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        themesView = (ThemesView) findViewById(R.id.themes);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        themesView.setTheme(prefs.getInt(PREF_THEME, 0));
        themesView.setListener(this);

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
                        if (toolbar != null)
                            toolbar.setTitleTextColor(integer);

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
