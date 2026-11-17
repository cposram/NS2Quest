package com.family.ns2quest

data class Question(
    val emoji: String,
    val prompt: String,
    val spokenText: String,
    val options: List<String>,
    val correctIndex: Int
)

object QuestionBank {
    private val allQuestions = listOf(
        Question("🍎", "聽聽看，這是哪一種水果？", "Apple", listOf("Banana", "Apple", "Orange"), 1),
        Question("🐘", "哪一個單字是「大象」？", "Elephant", listOf("Elephant", "Tiger", "Rabbit"), 0),
        Question("🚀", "在遊戲裡準備出發時，我們會說？", "Let's go!", listOf("Good night!", "Let's go!", "Thank you!"), 1),
        Question("🌧️", "外面正在下雨，英文怎麼說？", "It is rainy today.", listOf("It is sunny.", "It is snowy.", "It is rainy."), 2),
        Question("hungry", "肚子好餓想吃晚餐，你會說？", "I am hungry.", listOf("I am hungry.", "I am sleepy.", "I am cold."), 0),
        Question("🐢", "走路慢吞吞的動物是哪一個？", "Turtle", listOf("Cheetah", "Turtle", "Eagle"), 1),
        Question("⏰", "早上起床看到爸爸媽媽要說？", "Good morning!", listOf("Good morning!", "Good afternoon!", "See you!"), 0),
        Question("🎮", "贏得比賽時，你可以大喊？", "We are the champions!", listOf("I am sorry.", "We are the champions!", "Excuse me."), 1),
        Question("🍓", "紅色酸酸甜甜的「草莓」是？", "Strawberry", listOf("Watermelon", "Grape", "Strawberry"), 2),
        Question("🦈", "海裡牙齒很尖的「鯊魚」是？", "Shark", listOf("Whale", "Shark", "Dolphin"), 1),
        Question("🥤", "口渴想喝水時，你會說？", "May I have some water?", listOf("May I have some water?", "Where is my shoe?", "It is too loud."), 0),
        Question("🌈", "雨過天晴天空出現的「彩虹」是？", "Rainbow", listOf("Cloud", "Thunder", "Rainbow"), 2),
        Question("🦁", "森林之王「獅子」的英文是？", "Lion", listOf("Monkey", "Lion", "Zebra"), 1),
        Question("🚲", "兩個輪子用腳踩的「腳踏車」是？", "Bicycle", listOf("Airplane", "Bicycle", "Submarine"), 1),
        Question("⭐", "晚上在夜空閃閃發光的「星星」是？", "Star", listOf("Moon", "Sun", "Star"), 2)
    )

    fun getDailyQuestions(count: Int = 3): List<Question> {
        return allQuestions.shuffled().take(count)
    }
}