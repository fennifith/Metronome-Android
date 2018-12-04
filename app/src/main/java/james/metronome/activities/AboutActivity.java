package james.metronome.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.AestheticActivity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import james.metronome.Metronome;
import james.metronome.R;
import james.metronome.views.AppIconView;
import james.metronome.views.ThemesView;

public class AboutActivity extends AestheticActivity implements ThemesView.OnThemeChangedListener {

    private static final String PREF_THEME = "theme";

    private Toolbar toolbar;
    private AppIconView appIcon;
    private View iconView;
    private ThemesView themesView;
    private View buttonsView;
    private View creditsView;
    private View librariesView;

    private Disposable textColorPrimarySubscription;

    private SharedPreferences prefs;
    private Metronome metronome;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        metronome = (Metronome) getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        toolbar = findViewById(R.id.toolbar);
        appIcon = findViewById(R.id.appIcon);
        iconView = findViewById(R.id.icon);
        themesView = findViewById(R.id.themes);
        buttonsView = findViewById(R.id.buttons);
        View donateView = findViewById(R.id.donate);
        View githubView = findViewById(R.id.github);
        View playView = findViewById(R.id.play);
        View tewtwenteyonepxView = findViewById(R.id.tewtwenteyonepx);
        creditsView = findViewById(R.id.credits);
        View aesthetic = findViewById(R.id.aesthetic);
        View dialogs = findViewById(R.id.dialogs);
        View glide = findViewById(R.id.glide);
        librariesView = findViewById(R.id.libraries);

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
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://jfenn.me/redirects/?t=github&d=Metronome-Android")));
            }
        });

        playView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=james.metronome")));
            }
        });

        tewtwenteyonepxView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.221pixels.com/")));
            }
        });

        aesthetic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/afollestad/aesthetic")));
            }
        });

        dialogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/afollestad/material-dialogs")));
            }
        });

        glide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bumptech/glide")));
            }
        });

        subscribe();
    }

    private void subscribe() {
        if (themesView != null && appIcon != null) {
            themesView.subscribe();
            appIcon.subscribe();
        }

        textColorPrimarySubscription = Aesthetic.Companion.get()
                .textColorPrimary()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(@NonNull Integer integer) throws Exception {
                        if (toolbar != null && iconView != null && buttonsView != null && librariesView != null) {
                            toolbar.setTitleTextColor(integer);
                            DrawableCompat.setTint(toolbar.getBackground(), integer);
                            DrawableCompat.setTint(iconView.getBackground(), integer);
                            DrawableCompat.setTint(buttonsView.getBackground(), integer);
                            DrawableCompat.setTint(creditsView.getBackground(), integer);
                            DrawableCompat.setTint(librariesView.getBackground(), integer);
                        }

                        ActionBar actionBar = getSupportActionBar();
                        if (actionBar != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            Drawable drawable = ContextCompat.getDrawable(AboutActivity.this, R.drawable.ic_back);
                            DrawableCompat.setTint(drawable, integer);
                            actionBar.setHomeAsUpIndicator(drawable);
                        }
                    }
                });
    }

    private void unsubscribe() {
        if (themesView != null && appIcon != null) {
            themesView.unsubscribe();
            appIcon.unsubscribe();
        }

        textColorPrimarySubscription.dispose();
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (metronome != null && requestCode == Metronome.REQUEST_PURCHASE)
            metronome.onPremiumBought(resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish(); //IT ALL RETURNS TO NOTHING
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onThemeChanged(int theme) {
        metronome.onPremium(this);
        prefs.edit().putInt(PREF_THEME, theme).apply();
    }
}
