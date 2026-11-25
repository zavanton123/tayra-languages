package com.idiomiko.core.domain.account

/** The rules a new password is held to, and how strong it is by them and its length. */
object PasswordStrength {
    enum class Rule { LENGTH, DIGIT, LOWERCASE, UPPERCASE, SYMBOL }

    enum class Level { VERY_WEAK, WEAK, FAIR, GOOD, STRONG }

    const val MIN_LENGTH: Int = 8

    /** Long enough, with every rule met, to count as strong rather than good. */
    private const val STRONG_LENGTH = 12

    fun meets(password: String, rule: Rule): Boolean = when (rule) {
        Rule.LENGTH -> password.length >= MIN_LENGTH
        Rule.DIGIT -> password.any { it.isDigit() }
        Rule.LOWERCASE -> password.any { it.isLowerCase() }
        Rule.UPPERCASE -> password.any { it.isUpperCase() }
        Rule.SYMBOL -> password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
    }

    fun meetsAll(password: String): Boolean = Rule.entries.all { meets(password, it) }

    fun level(password: String): Level {
        val met = Rule.entries.count { meets(password, it) }
        return when {
            met == Rule.entries.size && password.length >= STRONG_LENGTH -> Level.STRONG
            met == Rule.entries.size -> Level.GOOD
            met == Rule.entries.size - 1 -> Level.FAIR
            met >= 2 -> Level.WEAK
            else -> Level.VERY_WEAK
        }
    }
}
