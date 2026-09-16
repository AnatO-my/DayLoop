package com.dayloop.app.today

sealed interface NaturalIntent {
    data class StartFocus(val minutes: Int? = null) : NaturalIntent
    data class StartBreak(val minutes: Int? = null) : NaturalIntent
    data class AddTask(val title: String) : NaturalIntent
}

class NaturalIntentParser {
    fun parse(input: String): NaturalIntent? {
        val normalized = input.trim().lowercase()
        
        // 1. Focus intents
        if (normalized.contains("focus") || normalized.contains("pomodoro") || normalized.contains("work")) {
            val minutes = "\\d+".toRegex().find(normalized)?.value?.toInt()
            return NaturalIntent.StartFocus(minutes)
        }
        
        // 2. Break intents
        if (normalized.contains("break") || normalized.contains("rest")) {
            val minutes = "\\d+".toRegex().find(normalized)?.value?.toInt()
            return NaturalIntent.StartBreak(minutes)
        }
        
        // 3. Add task intents
        val prefixes = listOf("remember to", "add task", "i need to", "remind me to", "todo", "new task")
        for (prefix in prefixes) {
            if (normalized.startsWith(prefix)) {
                val title = input.substring(normalized.indexOf(prefix) + prefix.length).trim()
                if (title.isNotEmpty()) return NaturalIntent.AddTask(title)
            }
        }
        
        return null
    }
}
