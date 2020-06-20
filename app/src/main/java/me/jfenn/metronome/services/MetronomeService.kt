package me.jfenn.metronome.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.*
import android.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import me.jfenn.metronome.R
import me.jfenn.metronome.views.TicksView
import java.util.*

class MetronomeService : Service() {

    private val binder: IBinder = LocalBinder()
    private val prefs: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    var interval: Long
        get() = prefs.getLong(PREF_INTERVAL, 500)
        set(value) {
            prefs.edit().putLong(PREF_INTERVAL, value).apply()
            listener?.onBpmChanged(toBpm(value))
        }

    var bpm: Int
        get() = toBpm(interval)
        set(value) {
            interval = toInterval(value)
        }

    private val soundPool: SoundPool by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SoundPool.Builder()
                    .setMaxStreams(1)
                    .setAudioAttributes(AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .build()
        } else SoundPool(1, AudioManager.STREAM_MUSIC, 0)
    }

    private var timer = Timer()
    private var timerTask = MetronomeTask(this)

    fun createTimerTask(delay: Long, interval: Long) {
        val oldTimer = timer
        timerTask = MetronomeTask(this)
        timer = Timer().apply {
            scheduleAtFixedRate(timerTask, delay, interval)
        }

        // cancel the old timer to stop executions (should be gc'd shortly after)
        oldTimer.cancel()
    }

    fun cancelTimerTask() {
        timer.cancel()
    }

    var tick: Int
        get() = prefs.getInt(PREF_TICK, 0)
        set(tick) {
            prefs.edit().putInt(PREF_TICK, tick).apply()
            soundId = createSoundId(tick)
        }

    private fun createSoundId(tick: Int): Int {
        return if (tick >= 0 && tick < TicksView.ticks.size && !TicksView.ticks[tick].isVibration)
            soundPool.load(this, TicksView.ticks[tick].soundRes, 1)
        else -1
    }

    private var soundId: Int = -1
        get() {
            if (field == -1)
                field = createSoundId(tick)

            return field
        }

    var isPlaying = false
        private set

    var emphasisList: MutableList<Boolean>
        get() = ArrayList<Boolean>().apply {
            val emphasisSize = prefs.getInt(PREF_EMPHASIS_SIZE, 4)
            for (i in 0 until emphasisSize) {
                add(prefs.getBoolean(PREF_EMPHASIS + i, false))
            }
        }
        set(value) {
            emphasisIndex = 0
            prefs.edit().apply {
                putInt(PREF_EMPHASIS_SIZE, value.size)
                for (i in value.indices) {
                    putBoolean(PREF_EMPHASIS + i, value[i])
                }

                apply()
            }
        }

    private var emphasisIndex = 0
    private val vibratorService: Vibrator by lazy { getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    private val notificationManager: NotificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private var listener: TickListener? = null

    override fun onCreate() {
        super.onCreate()
        interval = prefs.getLong(PREF_INTERVAL, 500)
        bpm = toBpm(interval)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                bpm = intent.getIntExtra(EXTRA_BPM, bpm)
                pause()
                play()
            }
            ACTION_PAUSE -> pause()
        }

        return START_STICKY
    }

    fun play() {
        isPlaying = true
        emphasisIndex = 0
        val intent = Intent(this, MetronomeService::class.java).apply { action = ACTION_PAUSE }

        val builder: NotificationCompat.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel("metronome", getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW))
            NotificationCompat.Builder(this, "metronome")
        } else NotificationCompat.Builder(this)

        startForeground(530,
                builder.setContentTitle(getString(R.string.notification_title))
                        .setContentText(getString(R.string.notification_desc))
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentIntent(PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_ONE_SHOT))
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .build()
        )

        createTimerTask(0, interval)
        listener?.onStartTicks()
    }

    fun pause() {
        cancelTimerTask()
        stopForeground(true)

        isPlaying = false
        listener?.onStopTicks()
    }

    fun setTickListener(listener: TickListener?) {
        this.listener = listener
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        listener = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cancelTimerTask()
        super.onDestroy()
    }

    inner class LocalBinder : Binder() {
        val service: MetronomeService
            get() = this@MetronomeService
    }

    private inner class MetronomeTask(
            val service: MetronomeService
    ) : TimerTask() {

        var interval = -1L
        val mainHandler = Handler(Looper.getMainLooper())

        override fun run() {
            if (service.isPlaying) {
                if (interval == -1L)
                    interval = service.interval

                if (service.emphasisIndex >= service.emphasisList.size)
                    service.emphasisIndex = 0

                val isEmphasis = service.emphasisList[service.emphasisIndex]

                when {
                    service.soundId != -1 -> service.soundPool.play(service.soundId, 1f, 1f, 0, 0, if (isEmphasis) 1.5f else 1f)
                    Build.VERSION.SDK_INT >= 26 -> service.vibratorService.vibrate(VibrationEffect.createOneShot(if (isEmphasis) 50 else 20.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
                    else -> service.vibratorService.vibrate(if (isEmphasis) 50 else 20.toLong())
                }

                mainHandler.post {
                    service.listener?.onTick(isEmphasis, service.emphasisIndex)
                    service.emphasisIndex++
                }

                if (interval != service.interval) {
                    interval = service.interval
                    service.createTimerTask(interval, interval)
                }
            }
        }
    }

    interface TickListener {
        fun onStartTicks()
        fun onTick(isEmphasis: Boolean, index: Int)
        fun onBpmChanged(bpm: Int)
        fun onStopTicks()
    }

    companion object {
        const val ACTION_START = "james.metronome.ACTION_START"
        const val ACTION_PAUSE = "james.metronome.ACTION_PAUSE"
        const val EXTRA_BPM = "james.metronome.EXTRA_BPM"
        const val PREF_TICK = "tick"
        const val PREF_INTERVAL = "interval"
        const val PREF_EMPHASIS_SIZE = "emphasisSize"
        const val PREF_EMPHASIS = "emphasis"

        private fun toBpm(interval: Long): Int {
            return (60000 / interval).toInt()
        }

        private fun toInterval(bpm: Int): Long {
            return 60000.toLong() / bpm
        }
    }
}