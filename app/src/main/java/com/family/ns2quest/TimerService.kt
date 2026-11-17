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
        const val EXTRA_MINUTES = "extra_minutes"
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
        val minutes = intent?.getIntExtra(EXTRA_MINUTES, 20) ?: 20
        val totalMillis = minutes * 60 * 1000L

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NS2 遊玩計時中")
            .setContentText("剩餘 $minutes 分鐘，時間到將自動鎖定螢幕")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)

        countDownTimer?.cancel()
        isRunning = true

        countDownTimer = object : CountDownTimer(totalMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = millisUntilFinished / 1000L

                // 剩餘 1 分鐘（60秒）發出語音存檔提醒
                if (remainingSeconds == 60L && ttsReady) {
                    tts?.speak(
                        "Attention! 1 minutes left. Please save your game now.",
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

                // 強制彈回 MainActivity，使 TCL 電視自動跳離 HDMI 1
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
