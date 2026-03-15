package com.example.replybubble.correction

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuiltInTextCorrectionEngine @Inject constructor() : TextCorrectionEngine {
    override suspend fun correct(text: String): String {
        var corrected = text
            .replace('\u00A0', ' ')
            .replace(Regex("[\\t ]+"), " ")
            .replace(Regex("\\s+([,.!?])"), "$1")
            .replace(Regex("([,.!?])(\\S)"), "$1 $2")
            .replace(Regex("\\s+"), " ")
            .trim()

        COMMON_TYPO_MAP.forEach { (wrong, right) ->
            corrected = corrected.replace(wrong, right)
        }

        corrected = corrected
            .replace(" 않되", " 안 되")
            .replace(" 안되", " 안 돼")
            .replace(" 되요", " 돼요")
            .replace(" 웬지", " 왠지")
            .replace(Regex("\\s+([ㅋㅋㅎㅎ]+)$"), " $1")
            .trim()

        return corrected.ifBlank { text.trim() }
    }

    companion object {
        private val COMMON_TYPO_MAP = linkedMapOf(
            "됬" to "됐",
            "왠만" to "웬만",
            "몇일" to "며칠",
            "어의없" to "어이없",
            "금새" to "금세",
            "되게됬" to "되게 됐",
        )
    }
}
