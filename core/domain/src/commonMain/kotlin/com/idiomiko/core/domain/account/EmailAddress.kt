package com.idiomiko.core.domain.account

/** Whether text reads as an email address, before it is sent to sign in, make an account or reset a password. */
object EmailAddress {
    private const val LOCAL_SIGNS = "!#$%&'*+/=?^_`{|}~.-"

    /**
     * One @, a name before it of letters, digits and the signs addresses allow, with no dot at either end
     * or two together, and a domain of dot-separated names ending in two or more letters ("you@mail.example.com").
     * Letters of any script count, for addresses such as "anna@почта.рф".
     */
    fun isValid(text: String): Boolean {
        val email = text.trim()
        if (email.isEmpty() || email.length > 254) return false
        val at = email.indexOf('@')
        if (at <= 0 || at != email.lastIndexOf('@')) return false
        val name = email.substring(0, at)
        val domain = email.substring(at + 1)
        if (name.length > 64 || name.startsWith('.') || name.endsWith('.') || ".." in name) return false
        if (!name.all { it.isLetterOrDigit() || it in LOCAL_SIGNS }) return false
        val labels = domain.split('.')
        if (labels.size < 2) return false
        if (labels.any { it.isEmpty() || it.length > 63 || it.startsWith('-') || it.endsWith('-') || !it.all { c -> c.isLetterOrDigit() || c == '-' } }) return false
        val top = labels.last()
        return top.length >= 2 && top.all { it.isLetter() }
    }
}
