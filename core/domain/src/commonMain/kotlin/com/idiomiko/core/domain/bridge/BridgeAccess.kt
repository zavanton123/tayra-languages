package com.idiomiko.core.domain.bridge

/**
 * Who may call the app: the extension, with the token it gets from the helper the app registers with
 * the browsers ([token]). A browser lets only the extensions named in the registration start the
 * helper, so no other extension or page gets the token.
 */
class BridgeAccess(private val token: () -> String) {

    fun accepts(sent: String?): Boolean = sent != null && same(token(), sent)

    /** Compares without stopping at the first difference, so timing tells nothing about a token. */
    private fun same(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }
}
