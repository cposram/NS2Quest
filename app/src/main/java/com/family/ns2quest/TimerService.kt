package com.family.ns2quest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale

class TimerService : Service(), TextToSpeech.OnInitListener {

    private var countDownTimer: CountDownTimer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    companion object {
        const val CHANNEL_ID = "ns2_timer_channel"
        const val EXTRA_ACTUAL_MINUTES = "extra_actual_minutes"
        const val EXTRA_DISPLAY_MINUTES = "extra_display_minutes"
        var remainingSeconds: Long = 0L
        var isRunning: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        createNotificationChannel()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            ttsReady = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val actualMinutes = intent?.getIntExtra(EXTRA_ACTUAL_MINUTES, 23) ?: 23
        val displayMinutes = intent?.getIntExtra(EXTRA_DISPLAY_MINUTES, 20) ?: 20
        val totalMillis = actualMinutes * 60 * 1000L

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NS2 勇者遊玩計時中")
            .setContentText("本輪遊玩時間：$displayMinutes 分鐘（含連線緩衝）")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)

        countDownTimer?.cancel()
        isRunning = true

        countDownTimer = object : CountDownTimer(totalMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = millisUntilFinished / 1000L

                // 僅在最後剩餘 1 分鐘（60秒）時提醒存檔
                if (remainingSeconds == 60L && ttsReady) {
                    tts?.speak(
                        "One minute left. Please save your game now.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "warn_1min"
                    )
                }
            }

            override fun onFinish() {
                isRunning = false
                remainingSeconds = 0L

                if (ttsReady) {
                    tts?.speak(
                        "Time is up! Great job playing today.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "time_up"
                    )
                }

                // 強制彈回 MainActivity，將畫面拉離 HDMI 1
                val lockIntent = Intent(this@TimerService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("TIME_UP", true)
                }
                startActivity(lockIntent)
                stopSelf()
            }
        }.start()

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "NS2 Play Timer",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        isRunning = false
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
