package com.family.ns2quest

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var ttsReady = false

    private var questions: List<Question> = emptyList()
    private var currentStep = 0

    private lateinit var rootLayout: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var emojiText: TextView
    private lateinit var promptText: TextView
    private lateinit var speakBtn: Button
    private val optionButtons = mutableListOf<Button>()

    private val prefs by lazy { getSharedPreferences("ns2_quest_prefs", Context.MODE_PRIVATE) }
    private val PARENT_PIN = "0924" // 家長預設密碼，可自行修改

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        // 防止孩子按電視遙控器「返回鍵」直接跳出 App
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@MainActivity, "完成每日英語試煉即可解鎖 NS2！", Toast.LENGTH_SHORT).show()
            }
        })

        buildUI()
        refreshState()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        refreshState()
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setSpeechRate(0.88f) // 語速稍慢，讓孩子聽得更清楚
            ttsReady = true
            speakCurrentQuestion()
        }
    }

    private fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun isCompletedToday(): Boolean {
        return prefs.getString("completed_date", "") == getTodayString()
    }

    private fun buildUI() {
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(64, 36, 64, 36)
        }

        // 頂部導覽列（進度 + 家長設定按鈕）
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        statusText = TextView(this).apply {
            textSize = 22f
            setTextColor(Color.parseColor("#38BDF8"))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val parentBtn = Button(this).apply {
            text = "🔒 家長設定"
            textSize = 16f
            setTextColor(Color.parseColor("#94A3B8"))
            background = createFocusableBg("#1E293B", "#334155")
            setPadding(28, 12, 28, 12)
            setOnClickListener { showParentPinDialog() }
        }

        topBar.addView(statusText)
        topBar.addView(parentBtn)
        rootLayout.addView(topBar)

        // 懸浮視窗權限提醒（第一次安裝必備，否則20分鐘到無法從 HDMI 1 自動切回）
        if (!Settings.canDrawOverlays(this)) {
            val permBtn = Button(this).apply {
                text = "⚠️ 首次設定：點此開啟「顯示在其他應用程式上層」權限（20分鐘自動切回必備）"
                textSize = 16f
                setTextColor(Color.BLACK)
                background = createFocusableBg("#FACC15", "#FEF08A")
                setPadding(24, 14, 24, 14)
                setOnClickListener {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
            }
            rootLayout.addView(permBtn, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 16 })
        }

        // 題目視覺 Emoji
        emojiText = TextView(this).apply {
            textSize = 72f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 8)
        }
        rootLayout.addView(emojiText)

        // 題目文字
        promptText = TextView(this).apply {
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 20)
        }
        rootLayout.addView(promptText)

        // 再聽一次發音按鈕
        speakBtn = Button(this).apply {
            text = "🔊 再聽一次英文發音"
            textSize = 20f
            setTextColor(Color.WHITE)
            background = createFocusableBg("#0284C7", "#0369A1")
            setPadding(40, 16, 40, 16)
            setOnClickListener { speakCurrentQuestion() }
        }
        rootLayout.addView(speakBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 32 })

        // 三個橫排大選項按鈕（方便遙控器左右切換）
        val optionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        optionButtons.clear()
        for (i in 0 until 3) {
            val btn = Button(this).apply {
                textSize = 26f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                background = createFocusableBg("#1E293B", "#2563EB")
                setPadding(24, 36, 24, 36)
                isFocusable = true
                setOnClickListener { checkAnswer(i) }
            }
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(18, 0, 18, 0)
            }
            optionsRow.addView(btn, params)
            optionButtons.add(btn)
        }

        rootLayout.addView(optionsRow)
        setContentView(rootLayout)
    }

    private fun refreshState() {
        if (isCompletedToday()) {
            // 今日已完成或 20 分鐘已到
            statusText.text = "🛡️ 今日 NS2 冒險狀態：已完成"
            emojiText.text = "🏆"
            promptText.text = "太棒了！今天的 20 分鐘任務已完成。\n好好休息眼睛，明天再來挑戰新關卡！"
            speakBtn.visibility = View.GONE
            optionButtons.forEach { it.visibility = View.GONE }
        } else {
            // 載入今日 3 題挑戰
            if (questions.isEmpty()) {
                questions = QuestionBank.getDailyQuestions(3)
                currentStep = 0
            }
            speakBtn.visibility = View.VISIBLE
            optionButtons.forEach { it.visibility = View.VISIBLE }
            renderCurrentQuestion()
        }
    }

    private fun renderCurrentQuestion() {
        val q = questions[currentStep]
        statusText.text = "⚔️ 每日 NS2 解鎖試煉：第 ${currentStep + 1} / ${questions.size} 關"
        emojiText.text = q.emoji
        promptText.text = q.prompt

        q.options.forEachIndexed { index, optionText ->
            optionButtons[index].text = optionText
        }
        optionButtons[0].requestFocus()
        speakCurrentQuestion()
    }

    private fun speakCurrentQuestion() {
        if (ttsReady && questions.isNotEmpty() && !isCompletedToday()) {
            val word = questions[currentStep].spokenText
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "q_speak")
        }
    }

    private fun checkAnswer(selectedIndex: Int) {
        val q = questions[currentStep]
        if (selectedIndex == q.correctIndex) {
            if (currentStep + 1 < questions.size) {
                currentStep++
                Toast.makeText(this, "答對了！進入下一關 ✨", Toast.LENGTH_SHORT).show()
                renderCurrentQuestion()
            } else {
                // 全部通關！標記今日完成、啟動20分鐘計時器、切換到 HDMI 1
                prefs.edit().putString("completed_date", getTodayString()).apply()
                unlockHdmi1AndStartTimer(22)
            }
        } else {
            Toast.makeText(this, "再聽一次發音想想看喔！💪", Toast.LENGTH_SHORT).show()
            speakCurrentQuestion()
        }
    }

    private fun unlockHdmi1AndStartTimer(minutes: Int) {
        if (ttsReady) {
            tts.speak("Mission complete! Unlocking Switch for $minutes minutes!", TextToSpeech.QUEUE_FLUSH, null, "win")
        }
        Toast.makeText(this, "🎉 闖關成功！正在切換至 HDMI 1（限時 $minutes 分鐘）", Toast.LENGTH_LONG).show()

        // 1. 啟動背景 20 分鐘倒數服務
        val serviceIntent = Intent(this, TimerService::class.java).apply {
            putExtra(TimerService.EXTRA_MINUTES, minutes)
        }
        startForegroundService(serviceIntent)

        // 2. 偵測 TCL 65P735 的 HDMI 1 並跳轉
        switchToHdmi1()
    }

    private fun switchToHdmi1() {
        try {
            val tvInputManager = getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
            val inputs = tvInputManager.tvInputList

            // 優先尋找名稱或 ID 包含 HDMI 1 的訊號源
            val hdmi1 = inputs.firstOrNull { info ->
                info.type == TvInputInfo.TYPE_HDMI &&
                        (info.loadLabel(this).toString().contains("1") ||
                                info.id.contains("HDMI1", ignoreCase = true) ||
                                info.id.contains("HW2", ignoreCase = true))
            } ?: inputs.firstOrNull { it.type == TvInputInfo.TYPE_HDMI }

            if (hdmi1 != null) {
                val uri = TvContract.buildChannelUriForPassthroughInput(hdmi1.id)
                val hdmiIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(hdmiIntent)
            } else {
                Toast.makeText(this, "找不到 HDMI 1 訊號源，請確認 NS2 已接上", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "切換 HDMI 發生錯誤：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showParentPinDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "輸入家長密碼 (預設 1234)"
        }

        AlertDialog.Builder(this)
            .setTitle("家長控制選單")
            .setView(input)
            .setPositiveButton("確認") { _, _ ->
                if (input.text.toString() == PARENT_PIN) {
                    showParentActions()
                } else {
                    Toast.makeText(this, "密碼錯誤", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showParentActions() {
        val actions = arrayOf(
            "🔄 重置今日闖關（讓孩子重新挑戰）",
            "⚡ 立即獎勵 10 分鐘 NS2（直接切 HDMI 1）",
            "🧪 測試切換至 HDMI 1（不計時）"
        )
        AlertDialog.Builder(this)
            .setTitle("請選擇操作")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> {
                        prefs.edit().remove("completed_date").apply()
                        questions = emptyList()
                        stopService(Intent(this, TimerService::class.java))
                        refreshState()
                        Toast.makeText(this, "已重置今日狀態！", Toast.LENGTH_SHORT).show()
                    }
                    1 -> unlockHdmi1AndStartTimer(10)
                    2 -> switchToHdmi1()
                }
            }
            .show()
    }

    private fun createFocusableBg(normalHex: String, focusedHex: String): StateListDrawable {
        val focused = GradientDrawable().apply {
            setColor(Color.parseColor(focusedHex))
            cornerRadius = 24f
            setStroke(8, Color.parseColor("#FACC15")) // 遙控器選中時亮起金色粗邊框
        }
        val normal = GradientDrawable().apply {
            setColor(Color.parseColor(normalHex))
            cornerRadius = 24f
            setStroke(2, Color.parseColor("#334155"))
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), focused)
            addState(intArrayOf(), normal)
        }
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
