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
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // ==========================================
    // 家長可自訂核心參數區
    // ==========================================
    private val MINUTES_PER_QUESTION = 2   // 每答對 1 題獲得幾分鐘
    private val MAX_QUESTIONS = 20         // 每輪最多授權答對幾題（20題 = 40分鐘）
    private val BUFFER_MINUTES = 3         // 結算時額外贈送的隱藏連線緩衝分鐘數
    private val PARENT_PIN = "1234"        // 家長重置與設定密碼
    // ==========================================

    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private val handler = Handler(Looper.getMainLooper())

    private var currentQuestion: QuizQuestion? = null
    private var correctCount = 0
    private var comboStreak = 0
    private var isCooldown = false

    // UI 元件
    private lateinit var rootLayout: LinearLayout
    private lateinit var titleText: TextView
    private lateinit var comboText: TextView
    private lateinit var scoreFormulaText: TextView
    private lateinit var quizContainer: LinearLayout
    private lateinit var categoryBadgeText: TextView
    private lateinit var meaningText: TextView
    private lateinit var hintBtn: Button
    private val optionButtons = mutableListOf<Button>()
    private lateinit var maxReachedText: TextView

    private lateinit var settleBtn: Button
    private lateinit var resetBtn: Button

    private val prefs by lazy { getSharedPreferences("ns2_quest_v2_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        // 讀取未結算的累積答對題數
        correctCount = prefs.getInt("saved_correct_count", 0)

        // 防止孩子按遙控器「返回鍵」直接跳出
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@MainActivity, "完成英語單字挑戰並按「結算」即可開啟 NS2！", Toast.LENGTH_SHORT).show()
            }
        })

        buildUI()
        loadNextQuestion()
        updateScoreDisplay()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.getBooleanExtra("TIME_UP", false) == true) {
            // 時間到被強制拉回時，確保累積已歸零並重載新題目
            correctCount = 0
            comboStreak = 0
            prefs.edit().putInt("saved_correct_count", 0).apply()
            loadNextQuestion()
            updateScoreDisplay()
        }
    }

    override fun onResume() {
        super.onResume()
        correctCount = prefs.getInt("saved_correct_count", 0)
        updateScoreDisplay()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setSpeechRate(0.88f)
            ttsReady = true
        }
    }

    private fun buildUI() {
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.parseColor("#0B1120"))
            setPadding(56, 28, 56, 28)
        }

        // 1. 頂部標題列
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        titleText = TextView(this).apply {
            text = "⚔️ NS2 勇者會考單字試煉"
            textSize = 22f
            setTextColor(Color.parseColor("#38BDF8"))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        comboText = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.parseColor("#FB923C"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 24, 0)
        }

        val parentBtn = Button(this).apply {
            text = "⚙️ 家長選單"
            textSize = 15f
            setTextColor(Color.parseColor("#94A3B8"))
            background = createFocusableBg("#1E293B", "#334155", "#94A3B8")
            setPadding(24, 10, 24, 10)
            setOnClickListener { showTvPinDialog("家長控制選單") { showParentMenu() } }
        }

        topBar.addView(titleText)
        topBar.addView(comboText)
        topBar.addView(parentBtn)
        rootLayout.addView(topBar)

        // 首次懸浮視窗權限提醒
        if (!Settings.canDrawOverlays(this)) {
            val permBtn = Button(this).apply {
                text = "⚠️ 首次設定：點此開啟「顯示在其他應用程式上層」權限（時間到自動切回必備）"
                textSize = 15f
                setTextColor(Color.BLACK)
                background = createFocusableBg("#FACC15", "#FEF08A", "#FFFFFF")
                setPadding(24, 10, 24, 10)
                setOnClickListener {
                    startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    )
                }
            }
            rootLayout.addView(permBtn, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 10 })
        }

        // 2. 即時累積計分板（目前答對 X 題 × 2 分鐘 = Y 分鐘）
        val scoreCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#162032"))
                cornerRadius = 20f
                setStroke(3, Color.parseColor("#38BDF8"))
            }
            setPadding(32, 18, 32, 18)
        }

        scoreFormulaText = TextView(this).apply {
            textSize = 26f
            setTextColor(Color.parseColor("#FACC15"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        scoreCard.addView(scoreFormulaText)

        rootLayout.addView(scoreCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 16; bottomMargin = 16 })

        // 3. 中央答題區
        quizContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        categoryBadgeText = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.parseColor("#A5B4FC"))
            typeface = Typeface.DEFAULT_BOLD
        }
        quizContainer.addView(categoryBadgeText)

        meaningText = TextView(this).apply {
            textSize = 36f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 16)
        }
        quizContainer.addView(meaningText)

        hintBtn = Button(this).apply {
            text = "🔊 聽單字發音提示"
            textSize = 17f
            setTextColor(Color.parseColor("#E0F2FE"))
            background = createFocusableBg("#0369A1", "#0284C7", "#FACC15")
            setPadding(32, 10, 32, 10)
            setOnClickListener {
                currentQuestion?.let { speakWord(it.correctWord) }
            }
        }
        quizContainer.addView(hintBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 24 })

        // 三個橫排單字選項按鈕
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
                textSize = 28f
                isAllCaps = false // 保留英文單字正常大小寫
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                background = createFocusableBg("#1E293B", "#2563EB", "#FACC15")
                setPadding(20, 32, 20, 32)
                isFocusable = true
                setOnClickListener { onOptionSelected(i) }
            }
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(16, 0, 16, 0)
            }
            optionsRow.addView(btn, params)
            optionButtons.add(btn)
        }
        quizContainer.addView(optionsRow)

        // 達 20 題上限時的提示字樣
        maxReachedText = TextView(this).apply {
            text = "🏆 已達成單輪最高 20 題挑戰（累積 40 分鐘）！\n請按下方綠色「結算並前往 NS2」開始遊玩！"
            textSize = 28f
            setTextColor(Color.parseColor("#4ADE80"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            visibility = View.GONE
        }

        rootLayout.addView(quizContainer)
        rootLayout.addView(maxReachedText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        // 4. 底部操作列：左側【結算按鈕】，右側遠離處【重置按鈕】
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
        }

        settleBtn = Button(this).apply {
            textSize = 22f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(40, 22, 40, 22)
            isFocusable = true
            setOnClickListener { handleSettleAndPlay() }
        }

        // 中間留出大段空白距離，避免小孩按結算時誤觸重置
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }

        resetBtn = Button(this).apply {
            text = "🔄 重置下一輪"
            textSize = 18f
            setTextColor(Color.parseColor("#FCA5A5"))
            background = createFocusableBg("#3F1D24", "#7F1D1D", "#FACC15")
            setPadding(28, 22, 28, 22)
            isFocusable = true
            setOnClickListener {
                showTvPinDialog("輸入家長密碼以重置下一輪") {
                    resetRound()
                }
            }
        }

        bottomBar.addView(settleBtn)
        bottomBar.addView(spacer)
        bottomBar.addView(resetBtn)
        rootLayout.addView(bottomBar)

        setContentView(rootLayout)
    }

    private fun loadNextQuestion() {
        if (correctCount >= MAX_QUESTIONS) return
        currentQuestion = QuestionBank.nextRandomQuestion()
        val q = currentQuestion!!

        categoryBadgeText.text = "【國中會考字彙・${q.category}】請選出正確的英文單字"
        meaningText.text = "「 ${q.meaning} 」"

        q.options.forEachIndexed { index, word ->
            optionButtons[index].text = word
            optionButtons[index].background = createFocusableBg("#1E293B", "#2563EB", "#FACC15")
            optionButtons[index].isEnabled = true
        }
        isCooldown = false
        optionButtons[0].requestFocus()
    }

    private fun onOptionSelected(selectedIndex: Int) {
        if (isCooldown || correctCount >= MAX_QUESTIONS) return
        val q = currentQuestion ?: return

        if (selectedIndex == q.correctIndex) {
            // 答對！唸出單字加深印象，累積題數 +1
            speakWord(q.correctWord)
            correctCount++
            comboStreak++
            prefs.edit().putInt("saved_correct_count", correctCount).apply()

            updateScoreDisplay()

            if (correctCount >= MAX_QUESTIONS) {
                Toast.makeText(this, "🎉 太神啦！已集滿 20 題（40 分鐘）上限！", Toast.LENGTH_LONG).show()
                settleBtn.requestFocus()
            } else {
                Toast.makeText(this, "✅ 答對！${q.correctWord}（+${MINUTES_PER_QUESTION} 分鐘）", Toast.LENGTH_SHORT).show()
                loadNextQuestion()
            }
        } else {
            // 答錯！啟動 1.5 秒冷卻鎖定防止狂按瞎猜，並播放正確發音提示
            comboStreak = 0
            updateScoreDisplay()
            isCooldown = true
            speakWord(q.correctWord)
            Toast.makeText(this, "❌ 再想一下！聽聽看發音提示（冷卻 1.5 秒）", Toast.LENGTH_SHORT).show()

            optionButtons[selectedIndex].background = createFocusableBg("#7F1D1D", "#991B1B", "#EF4444")
            optionButtons.forEach { it.isEnabled = false }

            handler.postDelayed({
                isCooldown = false
                optionButtons.forEachIndexed { idx, btn ->
                    btn.isEnabled = true
                    btn.background = createFocusableBg("#1E293B", "#2563EB", "#FACC15")
                    if (idx == selectedIndex) btn.requestFocus()
                }
            }, 1500L)
        }
    }

    private fun updateScoreDisplay() {
        val earnedMinutes = correctCount * MINUTES_PER_QUESTION
        scoreFormulaText.text = "目前答對：$correctCount / $MAX_QUESTIONS 題  ×  $MINUTES_PER_QUESTION 分鐘  ＝  累積 $earnedMinutes 分鐘"

        comboText.text = if (comboStreak >= 2) "🔥 連對 $comboStreak 題！" else ""

        if (correctCount >= MAX_QUESTIONS) {
            quizContainer.visibility = View.GONE
            maxReachedText.visibility = View.VISIBLE
        } else {
            quizContainer.visibility = View.VISIBLE
            maxReachedText.visibility = View.GONE
        }

        if (correctCount > 0) {
            settleBtn.text = "🎮 結算並前往 NS2（啟動 $earnedMinutes 分鐘）"
            settleBtn.background = createFocusableBg("#059669", "#10B981", "#FACC15")
        } else {
            settleBtn.text = "🎮 結算並前往 NS2（目前 0 分鐘）"
            settleBtn.background = createFocusableBg("#334155", "#475569", "#94A3B8")
        }
    }

    private fun handleSettleAndPlay() {
        if (correctCount <= 0) {
            Toast.makeText(this, "目前累積 0 分鐘，請先答對至少 1 題再結算喔！", Toast.LENGTH_SHORT).show()
            optionButtons[0].requestFocus()
            return
        }

        val displayMinutes = correctCount * MINUTES_PER_QUESTION
        val actualMinutes = displayMinutes + BUFFER_MINUTES // 偷加 3 分鐘進遊戲連線緩衝

        // 結算後立刻將累積歸零（代表點數已兌換使用，時間到被踢回時就是 0 分鐘）
        correctCount = 0
        comboStreak = 0
        prefs.edit().putInt("saved_correct_count", 0).apply()
        loadNextQuestion()
        updateScoreDisplay()

        launchHdmi1WithTimer(displayMinutes, actualMinutes)
    }

    private fun launchHdmi1WithTimer(displayMinutes: Int, actualMinutes: Int) {
        if (ttsReady) {
            tts.speak("Mission complete! Unlocking Switch for $displayMinutes minutes.", TextToSpeech.QUEUE_FLUSH, null, "go")
        }
        Toast.makeText(this, "🚀 結算成功！開放 $displayMinutes 分鐘（含 $BUFFER_MINUTES 分鐘連線緩衝）", Toast.LENGTH_LONG).show()

        val serviceIntent = Intent(this, TimerService::class.java).apply {
            putExtra(TimerService.EXTRA_DISPLAY_MINUTES, displayMinutes)
            putExtra(TimerService.EXTRA_ACTUAL_MINUTES, actualMinutes)
        }
        startForegroundService(serviceIntent)

        switchToHdmi1()
    }

    private fun resetRound() {
        stopService(Intent(this, TimerService::class.java))
        correctCount = 0
        comboStreak = 0
        prefs.edit().putInt("saved_correct_count", 0).apply()
        loadNextQuestion()
        updateScoreDisplay()
        Toast.makeText(this, "🔄 已重置！新一輪挑戰開始！", Toast.LENGTH_SHORT).show()
    }

    private fun switchToHdmi1() {
        try {
            val tvInputManager = getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
            val inputs = tvInputManager.tvInputList

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

    private fun speakWord(word: String) {
        if (ttsReady) {
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "word_tts")
        }
    }

    /**
     * 專為電視遙控器設計的 0-9 數字密碼輸入視窗（免叫出虛擬鍵盤）
     */
    private fun showTvPinDialog(title: String, onSuccess: () -> Unit) {
        var enteredPin = ""
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(36, 24, 36, 24)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        val pinDisplay = TextView(this).apply {
            text = "請輸入密碼（預設 1234）：_ _ _ _"
            textSize = 22f
            setTextColor(Color.parseColor("#FACC15"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        container.addView(pinDisplay)

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(container)
            .setNegativeButton("取消", null)
            .create()

        val grid = GridLayout(this).apply {
            columnCount = 6
            rowCount = 2
        }

        val keys = listOf("1", "2", "3", "4", "5", "清除", "6", "7", "8", "9", "0", "確認")
        keys.forEach { key ->
            val btn = Button(this).apply {
                text = key
                textSize = 18f
                setTextColor(Color.WHITE)
                background = createFocusableBg("#334155", "#2563EB", "#FACC15")
                setOnClickListener {
                    when (key) {
                        "清除" -> {
                            enteredPin = ""
                            pinDisplay.text = "請輸入密碼：_ _ _ _"
                        }
                        "確認" -> {
                            if (enteredPin == PARENT_PIN) {
                                dialog.dismiss()
                                onSuccess()
                            } else {
                                Toast.makeText(this@MainActivity, "密碼錯誤！", Toast.LENGTH_SHORT).show()
                                enteredPin = ""
                                pinDisplay.text = "密碼錯誤，請重試"
                            }
                        }
                        else -> {
                            if (enteredPin.length < 4) {
                                enteredPin += key
                                pinDisplay.text = "已輸入：" + "*".repeat(enteredPin.length)
                                if (enteredPin == PARENT_PIN) {
                                    dialog.dismiss()
                                    onSuccess()
                                }
                            }
                        }
                    }
                }
            }
            val params = GridLayout.LayoutParams().apply {
                width = 110
                height = 80
                setMargins(6, 6, 6, 6)
            }
            grid.addView(btn, params)
        }

        container.addView(grid)
        dialog.show()
    }

    private fun showParentMenu() {
        val actions = arrayOf(
            "⚡ 立即獎勵 15 分鐘（直接切 HDMI 1）",
            "🧪 測試切換至 HDMI 1（不計時）",
            "⏭️ 跳過目前這道單字"
        )
        AlertDialog.Builder(this)
            .setTitle("家長控制選單")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> launchHdmi1WithTimer(15, 15 + BUFFER_MINUTES)
                    1 -> switchToHdmi1()
                    2 -> loadNextQuestion()
                }
            }
            .show()
    }

    private fun createFocusableBg(normalHex: String, focusedHex: String, strokeHex: String): StateListDrawable {
        val focused = GradientDrawable().apply {
            setColor(Color.parseColor(focusedHex))
            cornerRadius = 20f
            setStroke(6, Color.parseColor(strokeHex))
        }
        val normal = GradientDrawable().apply {
            setColor(Color.parseColor(normalHex))
            cornerRadius = 20f
            setStroke(2, Color.parseColor("#475569"))
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), focused)
            addState(intArrayOf(), normal)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
