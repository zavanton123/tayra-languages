package com.tayra.languages.feature.reading

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tayra.languages.core.ui.audio.Speaker

/**
 * Reads the page aloud sentence after sentence, turning to the next page at the end of one.
 * With [autoPause] it stops after each sentence and stays on it; the next [play] moves on to the
 * following one. [current] is the sentence being read, or the one it stopped on.
 */
@Stable
class ContinuousReading(private val speaker: Speaker) {
    var active: Boolean by mutableStateOf(false)
        private set

    var current: String? by mutableStateOf(null)
        private set

    var autoPause: Boolean = false

    /** Turns to the next page; false on the last page. */
    var turnPage: () -> Boolean = { false }

    private var sentences: List<String> = emptyList()
    private var languageCode: String? = null

    /** Set when the page was turned mid-reading, so the new page is read from its first sentence. */
    private var readNewPage = false

    /** Set when an auto-pause fell at the end of a page, so the next play turns the page. */
    private var pageEnded = false

    /** Set when an auto-pause stopped after [current] was read, so the next play reads the one after it. */
    private var moveOnPlay = false

    fun setPage(sentences: List<String>, languageCode: String?) {
        val changed = sentences != this.sentences
        this.sentences = sentences
        this.languageCode = languageCode
        if (!changed) return
        pageEnded = false
        moveOnPlay = false
        if (readNewPage) {
            readNewPage = false
            if (sentences.isNotEmpty()) read(0) else stopReading()
        } else if (current !in sentences) {
            // Another page was opened by hand while reading: the old page's sentence stops too.
            if (active) {
                speaker.stop()
                stopReading()
            }
            current = null
        }
    }

    fun toggle() = if (active) pause() else play()

    fun play() {
        if (pageEnded) {
            pageEnded = false
            nextPage()
            return
        }
        if (sentences.isEmpty()) return
        val index = sentences.indexOf(current).coerceAtLeast(0)
        if (moveOnPlay) {
            moveOnPlay = false
            if (index + 1 < sentences.size) read(index + 1) else nextPage()
            return
        }
        read(index)
    }

    /** Reads the sentence after the current one, or the next page's first at the end of the page. */
    fun next() {
        if (sentences.isEmpty()) return
        val index = sentences.indexOf(current) + 1
        pageEnded = false
        if (index < sentences.size) read(index) else nextPage()
    }

    /** Reads the sentence before the current one (the first stays the first). */
    fun previous() {
        if (sentences.isEmpty()) return
        pageEnded = false
        read((sentences.indexOf(current) - 1).coerceAtLeast(0))
    }

    /** Reads the current sentence again from its start. */
    fun repeat() {
        if (sentences.isEmpty()) return
        pageEnded = false
        read(sentences.indexOf(current).coerceAtLeast(0))
    }

    /** Stops where it is; the next [play] reads the same sentence again. */
    fun pause() {
        active = false
        readNewPage = false
        speaker.stop()
    }

    /**
     * A sentence's own button: while reading, reading carries on from it (or pauses on the one
     * being read); otherwise it is read alone and the next [play] starts from it.
     */
    fun sentenceClicked(sentence: String) {
        val index = sentences.indexOf(sentence)
        if (active && index >= 0) {
            if (sentence == current) pause() else read(index)
            return
        }
        if (index >= 0) {
            current = sentence
            pageEnded = false
            moveOnPlay = false
        }
        speaker.toggle(sentence, languageCode)
    }

    /**
     * Something else took the speaker (a word read on click, say), so reading stops. Silence means
     * nothing here: the speaker is silent for an instant whenever it moves from one sentence to
     * the next, and while the next page loads; the end of a sentence comes through its callback.
     */
    fun speakerTaken(playing: String?) {
        if (active && !readNewPage && playing != null && playing != current) {
            active = false
            readNewPage = false
        }
    }

    private fun read(index: Int) {
        val sentence = sentences[index]
        moveOnPlay = false
        active = true
        current = sentence
        speaker.speak(sentence, languageCode) { finished(sentence) }
    }

    private fun finished(sentence: String) {
        if (!active || current != sentence) return
        val next = sentences.indexOf(sentence) + 1
        when {
            // The highlight stays on the sentence just read until play is pressed again.
            next in sentences.indices && autoPause -> {
                active = false
                moveOnPlay = true
            }
            next in sentences.indices -> read(next)
            autoPause -> {
                active = false
                pageEnded = true
            }
            else -> nextPage()
        }
    }

    private fun nextPage() {
        if (turnPage()) {
            active = true
            readNewPage = true
        } else {
            stopReading()
            current = null
        }
    }

    private fun stopReading() {
        active = false
        readNewPage = false
    }
}
