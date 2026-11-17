# 🎮 NS2 勇者英語試煉 (NS2Quest for TCL Google TV)

專為 **TCL 65P735 (Android TV / Google TV)** 設計的每日任務解鎖器。孩子必須透過電視遙控器完成每日英語聽力與單字試煉，系統才會自動切換訊號源至 **HDMI 1 (Nintendo Switch 2)**，並在遊玩時間結束後自動將畫面強制切回鎖定頁面。

---

## 🛠️ 客製化修改指南（免裝開發軟體，直接在 GitHub 網頁編輯即可）

在 GitHub 網頁點開對應檔案 $\rightarrow$ 點右上角 **鉛筆圖示 (Edit)** 修改 $\rightarrow$ 按 **Commit changes**，約 2 分鐘後即可至 **Actions** 分頁下載新版 APK。

### 1. 修改「每日題數」與「語音速度（難度調整）」
* **檔案位置**：`app/src/main/java/com/family/ns2quest/MainActivity.kt`

#### ① 調整每天要答對幾題才能解鎖（預設 `3` 題）
搜尋 `getDailyQuestions`，修改括號內的數字：
```kotlin
// 將 3 改成 5，代表每天要通過 5 題才能開玩
questions = QuestionBank.getDailyQuestions(3)
```

#### ② 調整英文發音語速（預設 `0.88f`）
搜尋 `setSpeechRate`，數值越小唸得越慢（適合低年級），`1.0f` 為正常母語人士語速：
```kotlin
tts.setSpeechRate(0.88f) // 建議範圍：0.75f (慢速) ~ 1.05f (標準)
```

---

### 2. 新增或修改「英語題庫」
* **檔案位置**：`app/src/main/java/com/family/ns2quest/QuestionBank.kt`

在 `allQuestions = listOf(...)` 裡面依照格式新增題目（注意逗號分隔）：
```kotlin
Question(
    emoji = "🦖",                          // 畫面顯示的大圖示
    prompt = "恐龍的英文是哪一個？",          // 中文提示題目
    spokenText = "Dinosaur",               // 電視喇叭會唸出來的英文（可放單字或整句對話）
    options = listOf("Dragon", "Dinosaur", "Dolphin"), // 三個選項
    correctIndex = 1                       // 正確答案位置：第1個填 0、第2個填 1、第3個填 2
),
```
> **💡 進階玩法（純聽力盲測）**：把 `prompt` 改成 `"仔細聽發音，選出正確的句子！"`，把 `spokenText` 寫成完整句子（例如 `"Where is my backpack?"`），就能訓練純英文聽力。

---

### 3. 修改「每日遊玩時間上限」（預設 20 分鐘）
* **檔案位置**：`app/src/main/java/com/family/ns2quest/MainActivity.kt`

#### ① 修改正常闖關成功後的解鎖分鐘數
搜尋 `unlockHdmi1AndStartTimer(20)`，修改括號內的分鐘數：
```kotlin
// 全部通關！將 20 改成 30 即為開放 30 分鐘
unlockHdmi1AndStartTimer(20)
```

#### ② 修改家長密碼加時獎勵的分鐘數（預設 `10` 分鐘）
搜尋 `showParentActions`，修改 `1 ->` 後方的數字：
```kotlin
1 -> unlockHdmi1AndStartTimer(10) // 家長手動獎勵的分鐘數
```

---

### 4. 修改「時間快到前的語音預警時間」
* **檔案位置**：`app/src/main/java/com/family/ns2quest/TimerService.kt`

系統採**倒數計時**邏輯（例如總長 20 分鐘時，剩餘 120 秒 = 第 18 分鐘發出提醒）。
搜尋 `remainingSeconds == 120L`，修改秒數與語音台詞：
```kotlin
// 120L 代表「還剩 120 秒（2分鐘）」時提醒；若想改為「還剩 5 分鐘」提醒，請改成 300L
if (remainingSeconds == 120L && ttsReady) {
    tts?.speak(
        "Attention! Two minutes left. Please save your game now.", // 預警英文語音內容
        TextToSpeech.QUEUE_FLUSH,
        null,
        "warn_2min"
    )
}
```
* **時間到強制切回的語音**（同一檔案往下約 15 行）：
```kotlin
tts?.speak(
    "Time is up! Great job playing today.", // 時間到切回鎖定畫面時的語音
    TextToSpeech.QUEUE_FLUSH,
    null,
    "time_up"
)
```

---

### 5. 修改「家長設定密碼」（預設 `1234`）
* **檔案位置**：`app/src/main/java/com/family/ns2quest/MainActivity.kt`

搜尋 `PARENT_PIN`，修改引號內的數字：
```kotlin
private val PARENT_PIN = "1234"
```

---

## 📺 TCL 65P735 安裝與首次設定備忘
1. **權限開啟（必做）**：首次啟動 APP 時，點選畫面上方黃色按鈕開啟 **「顯示在其他應用程式上層」** 權限，20 分鐘時間一到電視才會自動從 HDMI 1 彈回鎖定畫面。
2. **覆蓋更新**：日後修改題庫或時間後，直接用新下載的 `app-debug.apk` 安裝覆蓋即可，無需先解除安裝。
