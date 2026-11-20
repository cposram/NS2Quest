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
    private var pendingAlertText: String? = null

    private var halfTimeWarned = false
    private var oneMinWarned = false

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
            pendingAlertText?.let { msg ->
                pendingAlertText = null
                tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "timer_alert_retry")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val actualMinutes = intent?.getIntExtra(EXTRA_ACTUAL_MINUTES, 23) ?: 23
        val displayMinutes = intent?.getIntExtra(EXTRA_DISPLAY_MINUTES, 20) ?: 20
        val totalMillis = actualMinutes * 60 * 1000L

        // 計算「取得遊玩時間的一半」（例如取得 40 分鐘，剩餘 20 分鐘＝1200 秒時提醒換人）
        val halfRemainingSeconds = (displayMinutes * 60L) / 2L

        halfTimeWarned = false
        oneMinWarned = false

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

                // 1. 剩餘一半時間時提醒換人（僅在總時間大於 3 分鐘、避免與最後 1 分鐘重疊時觸發）
                if (!halfTimeWarned && halfRemainingSeconds > 90L && remainingSeconds <= halfRemainingSeconds) {
                    halfTimeWarned = true
                    speakAlert("Half time! Please switch players now.")
                }

                // 2. 最後剩餘 1 分鐘（60秒）提醒存檔
                if (!oneMinWarned && remainingSeconds <= 60L) {
                    oneMinWarned = true
                    speakAlert("One minute left. Please save your game now.")
                }
            }

            override fun onFinish() {
                isRunning = false
                remainingSeconds = 0L

                speakAlert("Time is up! Great job playing today.")

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

    private fun speakAlert(message: String) {
        if (!ttsReady || tts == null) {
            pendingAlertText = message
            reinitTts()
            return
        }
        val res = tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "timer_alert")
        if (res == TextToSpeech.ERROR) {
            ttsReady = false
            pendingAlertText = message
            reinitTts()
        }
    }

    private fun reinitTts() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = TextToSpeech(this, this)
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
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
