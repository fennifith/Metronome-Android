package james.metronome.activities;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.AestheticActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import james.metronome.Metronome;
import james.metronome.R;
import james.metronome.services.MetronomeService;
import james.metronome.utils.ConversionUtils;
import james.metronome.utils.WhileHeldListener;
import james.metronome.views.AppIconView;
import james.metronome.views.EmphasisSwitch;
import james.metronome.views.MetronomeView;
import james.metronome.views.SeekBar;
import james.metronome.views.ThemesView;
import james.metronome.views.TicksView;

public class MainActivity extends AestheticActivity implements TicksView.OnTickChangedListener, ServiceConnection, MetronomeService.TickListener, EmphasisSwitch.OnCheckedChangeListener, SeekBar.OnProgressChangeListener {

    private static final String PREF_BOOKMARKS_LENGTH = "bookmarksLength";
    private static final String PREF_BOOKMARK = "bookmark";

    private boolean isBound;
    private MetronomeService service;

    private AppIconView appIcon;
    private MetronomeView metronomeView;
    private ImageView playView;
    private LinearLayout emphasisLayout;
    private LinearLayout bookmarkLayout;
    private TextView bpmView;
    private ImageView aboutView;
    private ImageView bookmarkView;
    private ImageView touchView;
    private ImageView lessView;
    private ImageView moreView;
    private ImageView addEmphasisView;
    private ImageView removeEmphasisView;
    private TicksView ticksView;
    private SeekBar seekBar;

    private Disposable colorAccentSubscription;
    private Disposable colorBackgroundSubscription;
    private Disposable textColorPrimarySubscription;

    private int colorAccent;
    private int textColorPrimary;

    private SharedPreferences prefs;
    private List<Integer> bookmarks;

    private long prevTouchInterval;
    private long prevTouchTime;

    private Metronome metronome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        metronome = (Metronome) getApplicationContext();
        metronome.onCreateActivity();

        if (Aesthetic.isFirstTime())
            ThemesView.themes[0].apply(this);

        appIcon = findViewById(R.id.appIcon);
        metronomeView = findViewById(R.id.metronome);
        playView = findViewById(R.id.play);
        emphasisLayout = findViewById(R.id.emphasis);
        bookmarkLayout = findViewById(R.id.bookmarks);
        addEmphasisView = findViewById(R.id.add);
        removeEmphasisView = findViewById(R.id.remove);
        bpmView = findViewById(R.id.bpm);
        lessView = findViewById(R.id.less);
        moreView = findViewById(R.id.more);
        ticksView = findViewById(R.id.ticks);
        aboutView = findViewById(R.id.about);
        bookmarkView = findViewById(R.id.bookmark);
        touchView = findViewById(R.id.touch);
        seekBar = findViewById(R.id.seekBar);

        seekBar.setMaxProgress(300);

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

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        bookmarks = new ArrayList<>();
        int bookmarksLength = prefs.getInt(PREF_BOOKMARKS_LENGTH, 0);
        for (int i = 0; i < bookmarksLength; i++) {
            bookmarks.add(prefs.getInt(PREF_BOOKMARK + i, -1));
        }
        updateBookmarks(true);

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

        bookmarkView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isBound()) {
                    int bpm = service.getBpm();
                    if (bookmarks.contains(bpm))
                        removeBookmark(bpm);
                    else {
                        metronome.onPremium(MainActivity.this);
                        addBookmark(bpm);
                    }
                }
            }
        });

        touchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isBound()) {
                    if (prevTouchTime > 0) {
                        long interval = System.currentTimeMillis() - prevTouchTime;
                        if (interval > 200 && interval < 20000) {
                            if (prevTouchInterval == -1)
                                prevTouchInterval = interval;
                            else prevTouchInterval = (prevTouchInterval + interval) / 2;
                        }

                        seekBar.setProgress((int) (60000 / prevTouchInterval));
                    } else prevTouchTime = System.currentTimeMillis();
                }
            }
        });

        addEmphasisView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isBound()) {
                    if (service.getEmphasisList().size() < 50) {
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

        seekBar.setOnProgressChangeListener(this);

        ticksView.setListener(this);
        subscribe();

        new SplashThread(this).start();
    }

    private void addBookmark(int bpm) {
        if (bookmarks.contains(bpm))
            return;

        bookmarks.add(bpm);
        saveBookmarks();
        if (isBound())
            service.setBpm(bpm);
    }

    private void removeBookmark(int bpm) {
        if (isBound()) {
            if (!bookmarks.contains(bpm))
                return;

            bookmarks.remove((Object) bpm); //yes, you are a thing
            saveBookmarks();
            if (service.getBpm() == bpm)
                service.setBpm(bpm);
        }
    }

    private void saveBookmarks() {
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < bookmarks.size(); i++) {
            editor.putInt(PREF_BOOKMARK + i, bookmarks.get(i));
        }
        editor.putInt(PREF_BOOKMARKS_LENGTH, bookmarks.size());
        editor.apply();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Collections.sort(bookmarks);
            ShortcutManager manager = (ShortcutManager) getSystemService(Context.SHORTCUT_SERVICE);
            if (manager != null) {
                List<ShortcutInfo> shortcuts = new ArrayList<>();
                for (int bpm : bookmarks) {
                    shortcuts.add(
                            new ShortcutInfo.Builder(this, String.valueOf(bpm))
                                    .setShortLabel(getString(R.string.bpm, String.valueOf(bpm)))
                                    .setIcon(Icon.createWithResource(this, R.drawable.ic_note))
                                    .setIntent(getBookmarkIntent(bpm))
                                    .build()
                    );
                }

                manager.setDynamicShortcuts(shortcuts);
            }
        }

        updateBookmarks(true);
    }

    private void updateBookmarks(boolean contentChanged) {
        if (contentChanged) {
            Collections.sort(bookmarks);

            for (int i = 0; i < bookmarks.size(); i++) {
                if (!isBookmark(bookmarks.get(i))) {
                    int bpm = bookmarks.get(i);
                    boolean isSelected = false;
                    if (isBound())
                        isSelected = bpm == service.getBpm();

                    View view = LayoutInflater.from(this).inflate(R.layout.item_bookmark, bookmarkLayout, false);
                    view.setTag(bpm);
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (isBound() && view.getTag() != null && view.getTag() instanceof Integer)
                                service.setBpm((Integer) view.getTag());
                        }
                    });

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
                        view.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                                if (view.getTag() != null && view.getTag() instanceof Integer) {
                                    final int bpm = (Integer) view.getTag();

                                    new AlertDialog.Builder(MainActivity.this)
                                            .setTitle(R.string.title_add_shortcut)
                                            .setMessage(R.string.msg_add_shortcut)
                                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    Intent intent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
                                                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, getBookmarkIntent(bpm));
                                                    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.bpm, String.valueOf(bpm)));
                                                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
                                                    intent.putExtra("duplicate", false);
                                                    sendBroadcast(intent);

                                                    startActivity(new Intent(Intent.ACTION_MAIN)
                                                            .addCategory(Intent.CATEGORY_HOME)
                                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

                                                    dialogInterface.dismiss();
                                                }
                                            })
                                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    dialogInterface.dismiss();
                                                }
                                            })
                                            .show();
                                }
                                return false;
                            }
                        });
                    }

                    bookmarkLayout.addView(view, i);

                    ImageView imageView = view.findViewById(R.id.image);
                    imageView.setColorFilter(isSelected ? colorAccent : textColorPrimary);
                    imageView.invalidate();
                    TextView titleView = view.findViewById(R.id.title);
                    titleView.setText(getString(R.string.bpm, String.valueOf(bpm)));
                    titleView.setTextColor(isSelected ? colorAccent : textColorPrimary);
                }
            }

            for (int i = 0; i < bookmarkLayout.getChildCount(); i++) {
                View view = bookmarkLayout.getChildAt(i);
                if (!isBookmark(view))
                    bookmarkLayout.removeViewAt(i);
            }
        } else if (isBound()) {
            for (int i = 0; i < bookmarkLayout.getChildCount(); i++) {
                View view = bookmarkLayout.getChildAt(i);
                if (view.getTag() != null && view.getTag() instanceof Integer) {
                    boolean isSelected = service.getBpm() == (Integer) view.getTag();

                    final ImageView imageView = view.findViewById(R.id.image);
                    final TextView titleView = view.findViewById(R.id.title);

                    ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), titleView.getCurrentTextColor(), isSelected ? colorAccent : textColorPrimary);
                    animator.setDuration(250);
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                            int color = (int) valueAnimator.getAnimatedValue();
                            imageView.setColorFilter(color);
                            titleView.setTextColor(color);
                        }
                    });
                    animator.start();
                }
            }
        }
    }

    private boolean isBookmark(int bpm) {
        for (int i = 0; i < bookmarkLayout.getChildCount(); i++) {
            View view = bookmarkLayout.getChildAt(i);
            if (view.getTag() != null && view.getTag() instanceof Integer && bpm == (Integer) view.getTag())
                return true;
        }

        return false;
    }

    private boolean isBookmark(View view) {
        return view.getTag() != null && view.getTag() instanceof Integer && bookmarks.contains(view.getTag());
    }

    private Intent getBookmarkIntent(int bpm) {
        return new Intent(this, DummyShortcutActivity.class)
                .setAction(MetronomeService.ACTION_START)
                .putExtra(MetronomeService.EXTRA_BPM, bpm)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
        if (metronomeView != null && ticksView != null && seekBar != null && appIcon != null) {
            metronomeView.subscribe();
            ticksView.subscribe();
            seekBar.subscribe();
            appIcon.subscribe();
        }

        if (emphasisLayout != null) {
            for (int i = 0; i < emphasisLayout.getChildCount(); i++) {
                ((EmphasisSwitch) emphasisLayout.getChildAt(i)).subscribe();
            }
        }

        colorAccentSubscription = Aesthetic.get()
                .colorAccent()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        colorAccent = integer;
                        updateBookmarks(false);
                    }
                });

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
                        playView.setColorFilter(integer);
                        addEmphasisView.setColorFilter(integer);
                        removeEmphasisView.setColorFilter(integer);
                        moreView.setColorFilter(integer);
                        lessView.setColorFilter(integer);
                        aboutView.setColorFilter(integer);
                        bookmarkView.setColorFilter(integer);
                        textColorPrimary = integer;
                        updateBookmarks(false);
                    }
                });
    }

    public void unsubscribe() {
        if (metronomeView != null && ticksView != null && seekBar != null && appIcon != null) {
            metronomeView.unsubscribe();
            ticksView.unsubscribe();
            seekBar.unsubscribe();
            appIcon.unsubscribe();
        }

        if (emphasisLayout != null) {
            for (int i = 0; i < emphasisLayout.getChildCount(); i++) {
                ((EmphasisSwitch) emphasisLayout.getChildAt(i)).unsubscribe();
            }
        }

        colorAccentSubscription.dispose();
        colorBackgroundSubscription.dispose();
        textColorPrimarySubscription.dispose();
    }

    private EmphasisSwitch getEmphasisSwitch(boolean isChecked, boolean subscribe) {
        EmphasisSwitch emphasisSwitch = new EmphasisSwitch(this);
        emphasisSwitch.setChecked(isChecked);
        emphasisSwitch.setOnCheckedChangeListener(this);
        emphasisSwitch.setLayoutParams(new LinearLayout.LayoutParams(ConversionUtils.getPixelsFromDp(40), ConversionUtils.getPixelsFromDp(40)));

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
    protected void onDestroy() {
        if (metronome != null)
            metronome.onDestroyActivity();
        super.onDestroy();
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
    public void onTick(boolean isEmphasis, int index) {
        metronomeView.onTick(isEmphasis);

        for (int i = 0; i < emphasisLayout.getChildCount(); i++) {
            ((EmphasisSwitch) emphasisLayout.getChildAt(i)).setAccented(i == index);
        }
    }

    @Override
    public void onBpmChanged(int bpm) {
        if (isBound()) {
            metronomeView.setInterval(service.getInterval());
            bpmView.setText(String.format(Locale.getDefault(), getString(R.string.bpm), String.valueOf(bpm)));
            bookmarkView.setImageResource(bookmarks.contains(bpm) ? R.drawable.ic_bookmark : R.drawable.ic_bookmark_border);
            updateBookmarks(false);

            if (seekBar.getProgress() != bpm) {
                seekBar.setOnProgressChangeListener(null);
                seekBar.setProgress(bpm);
                seekBar.setOnProgressChangeListener(this);
            }
        }
    }

    @Override
    public void onStopTicks() {
        playView.setImageResource(R.drawable.ic_play);

        for (int i = 0; i < emphasisLayout.getChildCount(); i++) {
            ((EmphasisSwitch) emphasisLayout.getChildAt(i)).setAccented(false);
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

    @Override
    public void onProgressChange(int progress) {
        if (progress > 0 && isBound())
            service.setBpm(progress);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (metronome != null && requestCode == Metronome.REQUEST_PURCHASE)
            metronome.onPremiumBought(resultCode, data);
    }

    private class SplashThread extends Thread {

        private WeakReference<MainActivity> activityReference;

        public SplashThread(MainActivity activity) {
            activityReference = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            try {
                sleep(3000);
            } catch (InterruptedException e) {
                return;
            }

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    MainActivity activity = activityReference.get();
                    if (activity != null)
                        activity.findViewById(R.id.icon).setVisibility(View.GONE);
                }
            });
        }
    }
}
