package com.family.ns2quest

data class VocabItem(
    val category: String,
    val meaning: String,
    val word: String,
    val distractors: List<String>
)

data class QuizQuestion(
    val category: String,
    val meaning: String,
    val correctWord: String,
    val options: List<String>,
    val correctIndex: Int
)

object QuestionBank {

    private val vocabList = listOf(
        // --- 動詞 Verbs ---
        VocabItem("動詞", "決定；下定決心", "decide", listOf("arrive", "borrow", "invite")),
        VocabItem("動詞", "到達；抵達", "arrive", listOf("decide", "forget", "choose")),
        VocabItem("動詞", "借入；借用", "borrow", listOf("lend", "return", "order")),
        VocabItem("動詞", "借出；把東西借給人", "lend", listOf("borrow", "spend", "save")),
        VocabItem("動詞", "邀請", "invite", listOf("invent", "include", "improve")),
        VocabItem("動詞", "準備", "prepare", listOf("practice", "protect", "promise")),
        VocabItem("動詞", "練習", "practice", listOf("prepare", "provide", "produce")),
        VocabItem("動詞", "解釋；說明", "explain", listOf("expect", "express", "excuse")),
        VocabItem("動詞", "相信", "believe", listOf("become", "belong", "behave")),
        VocabItem("動詞", "記得；牢記", "remember", listOf("forget", "repeat", "receive")),
        VocabItem("動詞", "忘記", "forget", listOf("remember", "forgive", "follow")),
        VocabItem("動詞", "了解；明白", "understand", listOf("underline", "undertake", "introduce")),
        VocabItem("動詞", "改善；進步", "improve", listOf("invent", "invite", "imagine")),
        VocabItem("動詞", "保護", "protect", listOf("prepare", "provide", "promise")),
        VocabItem("動詞", "收集；蒐集", "collect", listOf("correct", "connect", "control")),
        VocabItem("動詞", "創造；創作", "create", listOf("cross", "count", "cover")),
        VocabItem("動詞", "發現；發覺", "discover", listOf("discuss", "disappear", "describe")),
        VocabItem("動詞", "討論", "discuss", listOf("discover", "decide", "design")),
        VocabItem("動詞", "期待；預料", "expect", listOf("explain", "express", "escape")),
        VocabItem("動詞", "表達；表示", "express", listOf("expect", "explain", "exercise")),
        VocabItem("動詞", "跟隨；遵守", "follow", listOf("borrow", "finish", "forgive")),
        VocabItem("動詞", "猜測", "guess", listOf("guide", "guard", "greet")),
        VocabItem("動詞", "發生", "happen", listOf("hurry", "handle", "hang")),
        VocabItem("動詞", "想像", "imagine", listOf("improve", "invite", "include")),
        VocabItem("動詞", "包含；包括", "include", listOf("invite", "invent", "increase")),
        VocabItem("動詞", "注意到；察覺", "notice", listOf("nod", "knock", "need")),
        VocabItem("動詞", "提供；主動給予", "offer", listOf("order", "open", "own")),
        VocabItem("動詞", "點餐；命令；訂購", "order", listOf("offer", "obey", "occur")),
        VocabItem("動詞", "承諾；答應", "promise", listOf("protect", "prepare", "practice")),
        VocabItem("動詞", "收到；接到", "receive", listOf("refuse", "repeat", "return")),
        VocabItem("動詞", "拒絕", "refuse", listOf("receive", "reduce", "repair")),
        VocabItem("動詞", "重複", "repeat", listOf("report", "return", "reply")),
        VocabItem("動詞", "解決（問題）；解答", "solve", listOf("save", "serve", "share")),
        VocabItem("動詞", "支持；支撐", "support", listOf("surprise", "suppose", "survive")),
        VocabItem("動詞", "選擇", "choose", listOf("change", "check", "chase")),
        VocabItem("動詞", "攜帶；搬運", "carry", listOf("catch", "cause", "care")),
        VocabItem("動詞", "建造；建築", "build", listOf("burn", "blow", "break")),
        VocabItem("動詞", "修理", "repair", listOf("repeat", "report", "return")),
        VocabItem("動詞", "分享；共用", "share", listOf("shake", "shine", "shout")),
        VocabItem("動詞", "節省；拯救", "save", listOf("solve", "serve", "send")),

        // --- 形容詞 Adjectives ---
        VocabItem("形容詞", "危險的", "dangerous", listOf("delicious", "different", "difficult")),
        VocabItem("形容詞", "方便的；便利的", "convenient", listOf("comfortable", "confident", "common")),
        VocabItem("形容詞", "舒適的；舒服的", "comfortable", listOf("convenient", "careful", "colorful")),
        VocabItem("形容詞", "美味的；好吃的", "delicious", listOf("dangerous", "different", "diligent")),
        VocabItem("形容詞", "不同的", "different", listOf("difficult", "diligent", "dangerous")),
        VocabItem("形容詞", "困難的", "difficult", listOf("different", "delicious", "dirty")),
        VocabItem("形容詞", "昂貴的", "expensive", listOf("excellent", "exciting", "empty")),
        VocabItem("形容詞", "有名的；著名的", "famous", listOf("favorite", "foreign", "friendly")),
        VocabItem("形容詞", "最喜愛的", "favorite", listOf("famous", "foreign", "fantastic")),
        VocabItem("形容詞", "外國的", "foreign", listOf("famous", "favorite", "formal")),
        VocabItem("形容詞", "友善的；親切的", "friendly", listOf("lonely", "lovely", "lively")),
        VocabItem("形容詞", "健康的", "healthy", listOf("heavy", "hungry", "honest")),
        VocabItem("形容詞", "誠實的", "honest", listOf("humble", "hungry", "healthy")),
        VocabItem("形容詞", "重要的", "important", listOf("impossible", "interesting", "independent")),
        VocabItem("形容詞", "有趣的", "interesting", listOf("important", "intelligent", "international")),
        VocabItem("形容詞", "懶惰的", "lazy", listOf("crazy", "dizzy", "busy")),
        VocabItem("形容詞", "孤單的；寂寞的", "lonely", listOf("lovely", "friendly", "lively")),
        VocabItem("形容詞", "現代的；摩登的", "modern", listOf("modest", "magic", "major")),
        VocabItem("形容詞", "緊張的；焦慮的", "nervous", listOf("noisy", "natural", "normal")),
        VocabItem("形容詞", "有禮貌的", "polite", listOf("popular", "possible", "public")),
        VocabItem("形容詞", "受歡迎的；流行的", "popular", listOf("polite", "possible", "patient")),
        VocabItem("形容詞", "可能的", "possible", listOf("polite", "popular", "powerful")),
        VocabItem("形容詞", "驕傲的；引以為榮的", "proud", listOf("loud", "round", "crowded")),
        VocabItem("形容詞", "安靜的", "quiet", listOf("quick", "quite", "cute")),
        VocabItem("形容詞", "安全的", "safe", listOf("same", "save", "soft")),
        VocabItem("形容詞", "嚴肅的；嚴重的", "serious", listOf("similar", "simple", "special")),
        VocabItem("形容詞", "相似的；類似的", "similar", listOf("simple", "serious", "single")),
        VocabItem("形容詞", "簡單的；樸素的", "simple", listOf("similar", "single", "silent")),
        VocabItem("形容詞", "特別的；特殊的", "special", listOf("social", "strange", "stupid")),
        VocabItem("形容詞", "奇怪的；陌生的", "strange", listOf("strong", "straight", "strict")),
        VocabItem("形容詞", "成功的", "successful", listOf("wonderful", "careful", "helpful")),
        VocabItem("形容詞", "糟糕的；可怕的", "terrible", listOf("comfortable", "possible", "valuable")),
        VocabItem("形容詞", "傳統的", "traditional", listOf("international", "national", "natural")),
        VocabItem("形容詞", "有用的；有幫助的", "useful", listOf("careful", "hopeful", "painful")),
        VocabItem("形容詞", "極好的；精彩的", "wonderful", listOf("successful", "colorful", "powerful")),

        // --- 名詞 Nouns ---
        VocabItem("名詞", "環境", "environment", listOf("equipment", "excitement", "experiment")),
        VocabItem("名詞", "經驗；經歷", "experience", listOf("exercise", "example", "entrance")),
        VocabItem("名詞", "知識；學問", "knowledge", listOf("language", "message", "package")),
        VocabItem("名詞", "語言", "language", listOf("knowledge", "luggage", "village")),
        VocabItem("名詞", "圖書館", "library", listOf("factory", "bakery", "history")),
        VocabItem("名詞", "藥；醫學", "medicine", listOf("machine", "magazine", "mountain")),
        VocabItem("名詞", "博物館", "museum", listOf("stadium", "medium", "aquarium")),
        VocabItem("名詞", "鄰居", "neighbor", listOf("stranger", "passenger", "teenager")),
        VocabItem("名詞", "報紙", "newspaper", listOf("notebook", "network", "neighbor")),
        VocabItem("名詞", "乘客；旅客", "passenger", listOf("stranger", "neighbor", "manager")),
        VocabItem("名詞", "行星", "planet", listOf("plant", "plane", "plate")),
        VocabItem("名詞", "人口", "population", listOf("pollution", "position", "pronunciation")),
        VocabItem("名詞", "總統；校長；總裁", "president", listOf("student", "parent", "accident")),
        VocabItem("名詞", "餐廳；飯店", "restaurant", listOf("department", "apartment", "government")),
        VocabItem("名詞", "科學", "science", listOf("silence", "sentence", "service")),
        VocabItem("名詞", "季節", "season", listOf("reason", "lesson", "person")),
        VocabItem("名詞", "秘密", "secret", listOf("ticket", "pocket", "jacket")),
        VocabItem("名詞", "句子", "sentence", listOf("science", "silence", "service")),
        VocabItem("名詞", "肩膀", "shoulder", listOf("soldier", "shower", "shelter")),
        VocabItem("名詞", "陌生人", "stranger", listOf("passenger", "neighbor", "danger")),
        VocabItem("名詞", "科目；主題", "subject", listOf("object", "project", "reject")),
        VocabItem("名詞", "溫度；體溫", "temperature", listOf("furniture", "future", "nature")),
        VocabItem("名詞", "交通；車流量", "traffic", listOf("topic", "picnic", "magic")),
        VocabItem("名詞", "寶藏；珍寶", "treasure", listOf("pleasure", "measure", "pressure")),
        VocabItem("名詞", "假期；休假", "vacation", listOf("station", "question", "invitation")),
        VocabItem("名詞", "蔬菜", "vegetable", listOf("comfortable", "table", "trouble")),
        VocabItem("名詞", "世紀（一百年）", "century", listOf("country", "factory", "history")),
        VocabItem("名詞", "文化", "culture", listOf("future", "nature", "picture")),
        VocabItem("名詞", "習慣", "habit", listOf("rabbit", "hobby", "human")),
        VocabItem("名詞", "嗜好；業餘愛好", "hobby", listOf("habit", "holiday", "honey")),

        // --- 副詞與常用詞 Adverbs & Others ---
        VocabItem("副詞", "已經", "already", listOf("almost", "always", "altogether")),
        VocabItem("副詞", "幾乎；差不多", "almost", listOf("already", "always", "alone")),
        VocabItem("副詞", "仔細地；小心地", "carefully", listOf("certainly", "clearly", "completely")),
        VocabItem("副詞", "當然；無疑地", "certainly", listOf("carefully", "suddenly", "seriously")),
        VocabItem("副詞", "特別；尤其是", "especially", listOf("finally", "actually", "usually")),
        VocabItem("副詞", "終於；最後", "finally", listOf("friendly", "family", "quickly")),
        VocabItem("副詞", "然而；不過", "however", listOf("forever", "whenever", "wherever")),
        VocabItem("副詞", "立刻；馬上", "immediately", listOf("importantly", "impossibly", "independently")),
        VocabItem("副詞", "代替；反而", "instead", listOf("inside", "indeed", "island")),
        VocabItem("副詞", "也許；大概", "perhaps", listOf("probably", "practice", "purpose")),
        VocabItem("副詞", "很可能；大概", "probably", listOf("perhaps", "properly", "publicly")),
        VocabItem("副詞", "最近；近來", "recently", listOf("suddenly", "secondly", "secretly")),
        VocabItem("副詞", "很少；不常", "seldom", listOf("system", "bottom", "custom")),
        VocabItem("副詞", "突然地", "suddenly", listOf("certainly", "recently", "secondly")),
        VocabItem("副詞", "在國外；到國外", "abroad", listOf("aboard", "across", "around"))
    )

    private var shuffledDeck: MutableList<VocabItem> = mutableListOf()

    /**
     * 從洗牌後的題庫抽取下一題（整輪內絕不重複，且三個選項位置每次隨機排列）
     */
    fun nextRandomQuestion(): QuizQuestion {
        if (shuffledDeck.isEmpty()) {
            shuffledDeck = vocabList.shuffled().toMutableList()
        }
        val item = shuffledDeck.removeAt(0)

        // 隨機挑選 2 個干擾項，與正確答案混合後再次洗牌
        val selectedDistractors = item.distractors.shuffled().take(2)
        val mixedOptions = (listOf(item.word) + selectedDistractors).shuffled()
        val correctIndex = mixedOptions.indexOf(item.word)

        return QuizQuestion(
            category = item.category,
            meaning = item.meaning,
            correctWord = item.word,
            options = mixedOptions,
            correctIndex = correctIndex
        )
    }
}
