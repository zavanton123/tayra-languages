package com.idiomiko.core.ui.components

import androidx.compose.material.icons.materialIcon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

/** Icons that the core material icon set does not include. */
object AppIcons {
    private fun icon(name: String, pathData: String): ImageVector =
        materialIcon(name = name) { addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black)) }

    /** Two lines of a sentence with a magnifier: looking for another example sentence. Drawn in strokes, as the design has it. */
    val FindExample: ImageVector by lazy {
        materialIcon(name = "FindExample") {
            fun line(data: String) = addPath(
                pathData = addPathNodes(data), stroke = SolidColor(Color.Black), strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
            line("M3.5 6h10M3.5 10h7")
            line("M19 15a4 4 0 1 1-8 0a4 4 0 1 1 8 0")
            line("M18 18l3 3")
        }
    }

    val VolumeUp: ImageVector by lazy {
        icon(
            "VolumeUp",
            "M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z",
        )
    }

    /** The same open book as [MenuBook]: the app has one book icon. */
    val Book: ImageVector get() = MenuBook

    val Globe: ImageVector by lazy {
        icon(
            "Globe",
            "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95c-.32-1.25-.78-2.45-1.38-3.56 1.84.63 3.37 1.91 4.33 3.56zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2s.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56-1.84-.63-3.37-1.9-4.33-3.56zm2.95-8H5.08c.96-1.66 2.49-2.93 4.33-3.56C8.81 5.55 8.35 6.75 8.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2s.07-1.35.16-2h4.68c.09.65.16 1.32.16 2s-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95c-.96 1.65-2.49 2.93-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2s-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z",
        )
    }

    /** A price-tag outline (Material "local_offer"), for tags. */
    val Tag: ImageVector by lazy {
        icon("Tag", "M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58.55 0 1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41 0-.55-.23-1.06-.59-1.42zM13 20.01L4 11V4h7v-.01l9 9-7 7.02zM6.5 5C5.67 5 5 5.67 5 6.5S5.67 8 6.5 8 8 7.33 8 6.5 7.33 5 6.5 5z")
    }

    /** Material "cloud_done": synced with the account. */
    val CloudDone: ImageVector by lazy {
        icon("CloudDone", "M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM10 17l-3.5-3.5 1.41-1.41L10 14.17 15.18 9l1.41 1.41L10 17z")
    }

    /** Material "cloud_off": the last sync did not finish. */
    val CloudOff: ImageVector by lazy {
        icon("CloudOff", "M19.35 10.04C18.67 6.59 15.64 4 12 4c-1.48 0-2.85.43-4.01 1.17l1.46 1.46C10.21 6.23 11.08 6 12 6c3.04 0 5.5 2.46 5.5 5.5v.5H19c1.66 0 3 1.34 3 3 0 1.13-.64 2.11-1.56 2.62l1.45 1.45C23.16 18.16 24 16.68 24 15c0-2.64-2.05-4.78-4.65-4.96zM3 5.27l2.75 2.74C2.56 8.15 0 10.77 0 14c0 3.31 2.69 6 6 6h11.73l2 2L21 20.73 4.27 4 3 5.27zM7.73 10l8 8H6c-2.21 0-4-1.79-4-4s1.79-4 4-4h1.73z")
    }

    /** Material "cloud_queue": syncing with the account is available, not yet done. */
    val Cloud: ImageVector by lazy {
        icon("Cloud", "M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM19 18H6c-2.21 0-4-1.79-4-4s1.79-4 4-4h.71C7.37 7.69 9.48 6 12 6c3.04 0 5.5 2.46 5.5 5.5v.5H19c1.66 0 3 1.34 3 3s-1.34 3-3 3z")
    }

    /** Material "cloud_upload": what is kept in the account, safe off this device. */
    val CloudUpload: ImageVector by lazy {
        icon("CloudUpload", "M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z")
    }

    /** Material "devices": a computer and a phone, for the same library on each. */
    val Devices: ImageVector by lazy {
        icon("Devices", "M4 6h18V4H4c-1.1 0-2 .9-2 2v11H0v3h14v-3H4V6zm19 2h-6c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h6c.55 0 1-.45 1-1V9c0-.55-.45-1-1-1zm-1 9h-4v-7h4v7z")
    }

    /** Material "sync": two arrows chasing each other round. */
    val Sync: ImageVector by lazy {
        icon("Sync", "M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46C19.54 15.03 20 13.57 20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74C4.46 8.97 4 10.43 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z")
    }

    /** Material "visibility": show what a password field hides. */
    val Visibility: ImageVector by lazy {
        icon("Visibility", "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z")
    }

    /** Material "visibility_off": hide a password shown in clear again. */
    val VisibilityOff: ImageVector by lazy {
        icon("VisibilityOff", "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z")
    }

    val BarChart: ImageVector by lazy { icon("BarChart", "M5 9.2h3V19H5zM10.6 5h2.8v14h-2.8zm5.6 8H19v6h-2.8z") }

    /** Material "newspaper", for the news. */
    val Newspaper: ImageVector by lazy {
        icon("Newspaper", "M22 3l-1.67 1.67L18.67 3 17 4.67 15.33 3l-1.66 1.67L12 3l-1.67 1.67L8.67 3 7 4.67 5.33 3 3.67 4.67 2 3v16c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V3zM11 19H4v-6h7v6zm9 0h-7v-2h7v2zm0-4h-7v-2h7v2zm0-4H4V8h16v3z")
    }

    val Flame: ImageVector by lazy {
        icon(
            "Flame",
            "M11.57 13.16c-1.36.28-2.17 1.16-2.17 2.41 0 1.34 1.11 2.42 2.49 2.42 2.05 0 3.71-1.66 3.71-3.71 0-1.07-.15-2.12-.46-3.12-.79 1.07-2.2 1.72-3.57 2zM13.5.67s.74 2.65.74 4.8c0 2.06-1.35 3.73-3.41 3.73-2.07 0-3.63-1.67-3.63-3.73l.03-.36C5.21 7.51 4 10.62 4 14c0 4.42 3.58 8 8 8s8-3.58 8-8C20 8.61 17.41 3.8 13.5.67z",
        )
    }

    val ViewList: ImageVector by lazy { icon("ViewList", "M3 14h4v-4H3v4zm0 5h4v-4H3v4zM3 9h4V5H3v4zm5 5h13v-4H8v4zm0 5h13v-4H8v4zM8 5v4h13V5H8z") }

    /** Material "archive": a box with an arrow into it, for the book archive. */
    val Archive: ImageVector by lazy {
        icon("Archive", "M20.54 5.23l-1.39-1.68C18.88 3.21 18.47 3 18 3H6c-.47 0-.88.21-1.16.55L3.46 5.23C3.17 5.57 3 6.02 3 6.5V19c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V6.5c0-.48-.17-.93-.46-1.27zM12 17.5L6.5 12H10v-2h4v2h3.5L12 17.5zM5.12 5l.81-1h12l.94 1H5.12z")
    }

    /** A graduation cap, for the courses. */
    val School: ImageVector by lazy { icon("School", "M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82zM12 3L1 9l11 6 9-4.91V17h2V9L12 3z") }

    /** A fan of cards, for the flashcards; upside down, so it opens downwards. */
    val Style: ImageVector by lazy {
        materialIcon(name = "Style") {
            group(scaleY = -1f, pivotY = 12f) {
                addPath(
                    pathData = addPathNodes("M2.53 19.65l1.34.56v-9.03l-2.43 5.86c-.41 1.02.08 2.19 1.09 2.61zm19.5-3.7L17.07 3.98c-.31-.75-1.04-1.21-1.81-1.23-.26 0-.53.04-.79.15L7.1 5.95c-.75.31-1.21 1.03-1.23 1.8-.01.27.04.54.15.8l4.96 11.97c.31.76 1.05 1.22 1.83 1.23.26 0 .52-.05.77-.15l7.36-3.05c1.02-.42 1.51-1.59 1.09-2.6zM7.88 8.75c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-2 11c0 1.1.9 2 2 2h1.45l-3.45-8.34v6.34z"),
                    fill = SolidColor(Color.Black),
                )
            }
        }
    }

    val PieChart: ImageVector by lazy { icon("PieChart", "M11 2v20c-5.07-.5-9-4.79-9-10s3.93-9.5 9-10zm2.03 0v8.99H22c-.47-4.74-4.24-8.52-8.97-8.99zm0 11.01V22c4.74-.47 8.5-4.25 8.97-8.99h-8.97z") }

    val GridView: ImageVector by lazy {
        icon("GridView", "M3 3v8h8V3H3zm6 6H5V5h4v4zm-6 4v8h8v-8H3zm6 6H5v-4h4v4zm4-16v8h8V3h-8zm6 6h-4V5h4v4zm-6 4v8h8v-8h-8zm6 6h-4v-4h4v4z")
    }

    val SwapVert: ImageVector by lazy { icon("SwapVert", "M16 17.01V10h-2v7.01h-3L15 21l4-3.99h-3zM9 3L5 6.99h3V14h2V6.99h3L9 3z") }

    val UnfoldMore: ImageVector by lazy {
        icon("UnfoldMore", "M12 5.83L15.17 9l1.41-1.41L12 3 7.41 7.59 8.83 9 12 5.83zm0 12.34L8.83 15l-1.41 1.41L12 21l4.59-4.59L15.17 15 12 18.17z")
    }

    val Palette: ImageVector by lazy {
        icon(
            "Palette",
            "M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9c.83 0 1.5-.67 1.5-1.5 0-.39-.15-.74-.39-1.01-.23-.26-.38-.61-.38-.99 0-.83.67-1.5 1.5-1.5H16c2.76 0 5-2.24 5-5 0-4.42-4.03-8-9-8zm-5.5 9c-.83 0-1.5-.67-1.5-1.5S5.67 9 6.5 9 8 9.67 8 10.5 7.33 12 6.5 12zm3-4C8.67 8 8 7.33 8 6.5S8.67 5 9.5 5s1.5.67 1.5 1.5S10.33 8 9.5 8zm5 0c-.83 0-1.5-.67-1.5-1.5S13.67 5 14.5 5s1.5.67 1.5 1.5S15.33 8 14.5 8zm3 4c-.83 0-1.5-.67-1.5-1.5S16.67 9 17.5 9s1.5.67 1.5 1.5-.67 1.5-1.5 1.5z",
        )
    }

    val FormatSize: ImageVector by lazy { icon("FormatSize", "M9 4v3h5v12h3V7h5V4H9zm-6 8h3v7h3v-7h3V9H3v3z") }

    val LineSpacing: ImageVector by lazy {
        icon("LineSpacing", "M6 7h2.5L5 3.5 1.5 7H4v10H1.5L5 20.5 8.5 17H6V7zm4-2v2h12V5H10zm0 14h12v-2H10v2zm0-6h12v-2H10v2z")
    }

    /** A table of rows and columns, for a word's conjugation or inflection. */
    val TableChart: ImageVector by lazy { icon("TableChart", "M10 10.02h5V21h-5zM17 21h3c1.1 0 2-.9 2-2v-9h-5v11zm3-18H5c-1.1 0-2 .9-2 2v3h19V5c0-1.1-.9-2-2-2zM3 19c0 1.1.9 2 2 2h3V10H3v9z") }

    val OpenInFull: ImageVector by lazy { icon("OpenInFull", "M21 11V3h-8l3.29 3.29-10 10L3 13v8h8l-3.29-3.29 10-10z") }

    val Fullscreen: ImageVector by lazy { icon("Fullscreen", "M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z") }

    val FullscreenExit: ImageVector by lazy { icon("FullscreenExit", "M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z") }

    /** A film strip (Material "movie"), for videos. */
    val Movie: ImageVector by lazy { icon("Movie", "M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z") }

    val Subtitles: ImageVector by lazy { icon("Subtitles", "M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zM4 12h4v2H4v-2zm10 6H4v-2h10v2zm6 0h-4v-2h4v2zm0-4H10v-2h10v2z") }

    val Speed: ImageVector by lazy { icon("Speed", "M20.38 8.57l-1.23 1.85a8 8 0 0 1-.22 7.58H5.07A8 8 0 0 1 15.58 6.85l1.85-1.23A10 10 0 0 0 3.35 19a2 2 0 0 0 1.72 1h13.85a2 2 0 0 0 1.74-1 10 10 0 0 0-.27-10.44zm-9.79 6.84a2 2 0 0 0 2.83 0l5.66-8.49-8.49 5.66a2 2 0 0 0 0 2.83z") }

    val Page: ImageVector by lazy {
        icon("Page", "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z")
    }

    val PageAdd: ImageVector by lazy {
        icon("PageAdd", "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 14h-3v3h-2v-3H8v-2h3v-3h2v3h3v2zm-3-7V3.5L18.5 9H13z")
    }

    val Bookmark: ImageVector by lazy { icon("Bookmark", "M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2zm0 15l-5-2.18L7 18V5h10v13z") }

    val Translate: ImageVector by lazy {
        icon(
            "Translate",
            "M12.87 15.07l-2.54-2.51.03-.03c1.74-1.94 2.98-4.17 3.71-6.53H17V4h-7V2H8v2H1v1.99h11.17C11.5 7.92 10.44 9.75 9 11.35 8.07 10.32 7.3 9.19 6.69 8h-2c.73 1.63 1.73 3.17 2.98 4.56l-5.09 5.02L4 19l5-5 3.11 3.11.76-2.04zM18.5 10h-2L12 22h2l1.12-3h4.75L21 22h2l-4.5-12zm-2.62 7l1.62-4.33L19.12 17h-3.24z",
        )
    }

    val Keyboard: ImageVector by lazy {
        icon(
            "Keyboard",
            "M20 5H4c-1.1 0-1.99.9-1.99 2L2 17c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm-9 3h2v2h-2V8zm0 3h2v2h-2v-2zM8 8h2v2H8V8zm0 3h2v2H8v-2zm-1 2H5v-2h2v2zm0-3H5V8h2v2zm9 7H8v-2h8v2zm0-4h-2v-2h2v2zm0-3h-2V8h2v2zm3 3h-2v-2h2v2zm0-3h-2V8h2v2z",
        )
    }

    val Link: ImageVector by lazy {
        icon("Link", "M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4c2.76 0 5-2.24 5-5s-2.24-5-5-5z")
    }

    val ContentCopy: ImageVector by lazy {
        icon("ContentCopy", "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z")
    }

    val OpenInNew: ImageVector by lazy {
        icon("OpenInNew", "M19 19H5V5h7V3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2v-7h-2v7zM14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7z")
    }

    /** Two arrows chasing round: one thing swapped for another. */
    /** Two speech bubbles: example sentences to choose from. */
    val Forum: ImageVector by lazy {
        icon("Forum", "M15 4v7H5.17L4 12.17V4h11m1-2H3c-.55 0-1 .45-1 1v14l4-4h10c.55 0 1-.45 1-1V3c0-.55-.45-1-1-1zm5 4h-2v9H6v2c0 .55.45 1 1 1h11l4 4V7c0-.55-.45-1-1-1z")
    }

    val Abc: ImageVector by lazy {
        icon(
            "Abc",
            "M21 11h-1.5v-.5h-2v3h2V13H21v1c0 .55-.45 1-1 1h-3c-.55 0-1-.45-1-1v-4c0-.55.45-1 1-1h3c.55 0 1 .45 1 1v1zM8 10v5H6.5v-1.5h-2V15H3v-5c0-.55.45-1 1-1h3c.55 0 1 .45 1 1zm-1.5.5h-2V12h2v-1.5zm7 1.5c.55 0 1 .45 1 1v1c0 .55-.45 1-1 1h-4V9h4c.55 0 1 .45 1 1v1c0 .55-.45 1-1 1zm-2.5-1.5v.5h2v-.5h-2zm2 2.5h-2v.5h2V13z",
        )
    }

    /** A closed book with a bookmark, for the offline dictionary. */
    val BookClosed: ImageVector by lazy {
        icon("BookClosed", "M18 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM6 4h5v8l-2.5-1.5L6 12V4z")
    }

    val Download: ImageVector by lazy { icon("Download", "M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z") }

    val PlayArrow: ImageVector by lazy { icon("PlayArrow", "M8 5v14l11-7z") }

    val Stop: ImageVector by lazy { icon("Stop", "M6 6h12v12H6z") }

    val Pause: ImageVector by lazy { icon("Pause", "M6 19h4V5H6v14zm8-14v14h4V5h-4z") }

    /** Lines of text and a pause after them: stopping after each sentence read aloud. */
    val PauseAfterSentence: ImageVector by lazy { icon("PauseAfterSentence", "M2 7h11v2H2zm0 4h11v2H2zm0 4h7v2H2zm14-8h2v10h-2zm4 0h2v10h-2z") }

    // Listening to playlists.

    val Headphones: ImageVector by lazy {
        icon("Headphones", "M12 1c-4.97 0-9 4.03-9 9v7c0 1.66 1.34 3 3 3h3v-8H5v-2c0-3.87 3.13-7 7-7s7 3.13 7 7v2h-4v8h3c1.66 0 3-1.34 3-3v-7c0-4.97-4.03-9-9-9z")
    }

    /** Lines of a list with a note: a playlist. */
    val QueueMusic: ImageVector by lazy {
        icon("QueueMusic", "M15 6H3v2h12V6zm0 4H3v2h12v-2zM3 16h8v-2H3v2zM17 6v8.18c-.31-.11-.65-.18-1-.18-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3V8h3V6h-5z")
    }

    val PlaylistAdd: ImageVector by lazy {
        icon("PlaylistAdd", "M14 10H2v2h12v-2zm0-4H2v2h12V6zm4 8v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zM2 16h8v-2H2v2z")
    }

    val Shuffle: ImageVector by lazy {
        icon("Shuffle", "M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z")
    }

    val Repeat: ImageVector by lazy { icon("Repeat", "M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z") }

    val RepeatOne: ImageVector by lazy { icon("RepeatOne", "M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4zm-4-2V9h-1l-2 1v1h1.5v4H13z") }

    val SkipNext: ImageVector by lazy { icon("SkipNext", "M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z") }

    val SkipPrevious: ImageVector by lazy { icon("SkipPrevious", "M6 6h2v12H6zm3.5 6l8.5 6V6z") }

    /** Two columns of dots: a handle to drag a row by. */
    val DragIndicator: ImageVector by lazy {
        icon(
            "DragIndicator",
            "M11 18c0 1.1-.9 2-2 2s-2-.9-2-2 .9-2 2-2 2 .9 2 2zm-2-8c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0-6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm6 4c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z",
        )
    }

    /** A clock face: when something comes. */
    /** Material "rocket_launch": something new to set off on, such as a recommended course. */
    val RocketLaunch: ImageVector by lazy {
        icon(
            "RocketLaunch",
            "M9.19 6.35c-2.04 2.29-3.44 5.58-3.57 5.89L2 10.69l4.05-4.05c.47-.47 1.15-.68 1.81-.55l1.33.26zM11.17 17s3.74-1.55 5.89-3.7c5.4-5.4 4.5-9.62 4.21-10.57-.95-.3-5.17-1.19-10.57 4.21C8.55 9.09 7 12.83 7 12.83L11.17 17zm6.48-2.19c-2.29 2.04-5.58 3.44-5.89 3.57L13.31 22l4.05-4.05c.47-.47.68-1.15.55-1.81l-.26-1.33zM9 18c0 .83-.34 1.58-.88 2.12C6.94 21.3 2 22 2 22s.7-4.94 1.88-6.12C4.42 15.34 5.17 15 6 15c1.66 0 3 1.34 3 3zm4-9c0-1.1.9-2 2-2s2 .9 2 2-.9 2-2 2-2-.9-2-2z",
        )
    }

    /** Material "adjust": a target, such as the day's goal. */
    val Target: ImageVector by lazy {
        icon("Target", "M12 2C6.49 2 2 6.49 2 12s4.49 10 10 10 10-4.49 10-10S17.51 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm3-8c0 1.66-1.34 3-3 3s-3-1.34-3-3 1.34-3 3-3 3 1.34 3 3z")
    }

    val Schedule: ImageVector by lazy {
        icon("Schedule", "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z")
    }

    val Replay: ImageVector by lazy { icon("Replay", "M12 5V1L7 6l5 5V7c3.31 0 6 2.69 6 6s-2.69 6-6 6-6-2.69-6-6H4c0 4.42 3.58 8 8 8s8-3.58 8-8-3.58-8-8-8z") }

    /** A page with lines: a text. */
    val Description: ImageVector by lazy {
        icon("Description", "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z")
    }

    val Lightbulb: ImageVector by lazy {
        icon(
            "Lightbulb",
            "M9 21c0 .55.45 1 1 1h4c.55 0 1-.45 1-1v-1H9v1zm3-19C8.14 2 5 5.14 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.86-3.14-7-7-7zm2.85 11.1l-.85.6V16h-4v-2.3l-.85-.6C7.8 12.16 7 10.63 7 9c0-2.76 2.24-5 5-5s5 2.24 5 5c0 1.63-.8 3.16-2.15 4.1z",
        )
    }

    val RecordVoiceOver: ImageVector by lazy {
        icon("RecordVoiceOver", "M9 13c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4zm6.08-7.95c.84 1.18.84 2.71 0 3.89l1.68 1.69c2.02-2.02 2.02-5.07 0-7.27l-1.68 1.69zM20.07 2l-1.63 1.63c2.77 3.02 2.77 7.56 0 10.74L20.07 16c3.9-3.89 3.91-9.95 0-14z")
    }

    val VolumeOff: ImageVector by lazy {
        icon(
            "VolumeOff",
            "M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z",
        )
    }

    val Mic: ImageVector by lazy {
        icon("Mic", "M12 14c1.66 0 2.99-1.34 2.99-3L15 5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z")
    }

    val FormatAlignLeft: ImageVector by lazy { icon("FormatAlignLeft", "M15 15H3v2h12v-2zm0-8H3v2h12V7zM3 13h18v-2H3v2zm0 8h18v-2H3v2zM3 3v2h18V3H3z") }

    val ViewStream: ImageVector by lazy { icon("ViewStream", "M4 14h17v-4H4v4zm0 5h17v-4H4v4zM4 5v4h17V5H4z") }

    val ViewColumn: ImageVector by lazy { icon("ViewColumn", "M14.67 5v14H9.33V5h5.34zm1 14H21V5h-5.33v14zm-7.34 0V5H3v14h5.33z") }

    val Tune: ImageVector by lazy {
        icon("Tune", "M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z")
    }

    val Explore: ImageVector by lazy {
        icon(
            "Explore",
            "M12 10.9c-.61 0-1.1.49-1.1 1.1s.49 1.1 1.1 1.1c.61 0 1.1-.49 1.1-1.1s-.49-1.1-1.1-1.1zM12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm2.19 12.19L6 18l3.81-8.19L18 6l-3.81 8.19z",
        )
    }

    val DoneAll: ImageVector by lazy {
        icon("DoneAll", "M18 7l-1.41-1.41-6.34 6.34 1.41 1.41L18 7zm4.24-1.41L11.66 16.17 7.48 12l-1.41 1.41L11.66 19l12-12-1.42-1.41zM.41 13.41L6 19l1.41-1.41L1.83 12 .41 13.41z")
    }

    val ArrowLeft: ImageVector by lazy { icon("ArrowLeft", "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z") }
    val ArrowRight: ImageVector by lazy { icon("ArrowRight", "M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z") }
    val ArrowUp: ImageVector by lazy { icon("ArrowUp", "M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z") }
    val ArrowDown: ImageVector by lazy { icon("ArrowDown", "M20 12l-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z") }

    val UploadFile: ImageVector by lazy {
        icon("UploadFile", "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11zM8 15.01l1.41 1.41L11 14.84V19h2v-4.16l1.59 1.59L16 15.01 12.01 11z")
    }

    val History: ImageVector by lazy {
        icon(
            "History",
            "M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z",
        )
    }

    val FileOutline: ImageVector by lazy { icon("FileOutline", "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zM6 20V4h7v5h5v11H6z") }

    val VerifiedUser: ImageVector by lazy {
        icon("VerifiedUser", "M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z")
    }

    val Share: ImageVector by lazy {
        icon(
            "Share",
            "M16 5l-1.42 1.42-1.59-1.59V16h-1.98V4.83L9.42 6.42 8 5l4-4 4 4zm4 5v11c0 1.1-.9 2-2 2H6c-1.11 0-2-.9-2-2V10c0-1.11.89-2 2-2h3v2H6v11h12V10h-3V8h3c1.1 0 2 .89 2 2z",
        )
    }

    /** Material "delete_outline": a bin drawn in outline, for clearing the data on the device. */
    val DeleteOutline: ImageVector by lazy { icon("DeleteOutline", "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM8 9h8v10H8V9zm7.5-5l-1-1h-5l-1 1H5v2h14V4z") }

    val RemoveCircle: ImageVector by lazy {
        icon("RemoveCircle", "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm5 11H7v-2h10v2z")
    }

    val Flag: ImageVector by lazy { icon("Flag", "M14.4 6L14 4H5v17h2v-7h5.6l.4 2h7V6z") }

    /** Material "account_balance": a columned hall, for politics. */
    val AccountBalance: ImageVector by lazy { icon("AccountBalance", "M4 10v7h3v-7H4zm6 0v7h3v-7h-3zM2 22h19v-3H2v3zm14-12v7h3v-7h-3zm-4.5-9L2 6v2h19V6l-9.5-5z") }

    /** Material "trending_up", for the economy. */
    val TrendingUp: ImageVector by lazy { icon("TrendingUp", "M16 6l2.29 2.29-4.88 4.88-4-4L2 16.59 3.41 18l6-6 4 4 6.3-6.29L22 12V6z") }

    /** Material "emoji_events": a cup, for sport. */
    val Trophy: ImageVector by lazy {
        icon(
            "Trophy",
            "M19 5h-2V3H7v2H5c-1.1 0-2 .9-2 2v1c0 2.55 1.92 4.63 4.39 4.94.63 1.5 1.98 2.63 3.61 2.96V19H7v2h10v-2h-4v-3.1c1.63-.33 2.98-1.46 3.61-2.96C19.08 12.63 21 10.55 21 8V7c0-1.1-.9-2-2-2zM5 8V7h2v3.82C5.84 10.4 5 9.3 5 8zm14 0c0 1.3-.84 2.4-2 2.82V7h2v1z",
        )
    }

    /** Material "science": a flask. */
    val Science: ImageVector by lazy {
        icon("Science", "M19.8 18.4L14 10.67V6.5l1.35-1.69c.26-.33.03-.81-.39-.81H9.04c-.42 0-.65.48-.39.81L10 6.5v4.17L4.2 18.4c-.49.66-.02 1.6.8 1.6h14c.82 0 1.29-.94.8-1.6z")
    }

    /** Material "local_cafe": a cup of coffee, for lifestyle. */
    val LocalCafe: ImageVector by lazy { icon("LocalCafe", "M20 3H4v10c0 2.21 1.79 4 4 4h6c2.21 0 4-1.79 4-4v-3h2c1.11 0 2-.89 2-2V5c0-1.11-.89-2-2-2zm0 5h-2V5h2v3zM4 19h16v2H4z") }

    val Terminal: ImageVector by lazy {
        icon("Terminal", "M20 4H4c-1.11 0-2 .9-2 2v12c0 1.1.89 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.89-2-2-2zm0 14H4V8h16v10zm-2-1h-6v-2h6v2zM7.5 17l-1.41-1.41L8.67 13l-2.59-2.59L7.5 9l4 4-4 4z")
    }

    val MoreHoriz: ImageVector by lazy {
        icon("MoreHoriz", "M6 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm12 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm-6 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z")
    }

    val SwapHoriz: ImageVector by lazy { icon("SwapHoriz", "M6.99 11L3 15l3.99 4v-3H14v-2H6.99v-3zM21 9l-3.99-4v3H10v2h7.01v3L21 9z") }

    val Memory: ImageVector by lazy {
        icon(
            "Memory",
            "M15 9H9v6h6V9zm-2 4h-2v-2h2v2zm8-2V9h-2V7c0-1.1-.9-2-2-2h-2V3h-2v2h-2V3H9v2H7c-1.1 0-2 .9-2 2v2H3v2h2v2H3v2h2v2c0 1.1.9 2 2 2h2v2h2v-2h2v2h2v-2h2c1.1 0 2-.9 2-2v-2h2v-2h-2v-2h2zm-4 6H7V7h10v10z",
        )
    }

    val Storage: ImageVector by lazy {
        icon("Storage", "M2 20h20v-4H2v4zm2-3h2v2H4v-2zM2 4v4h20V4H2zm4 3H4V5h2v2zm-4 7h20v-4H2v4zm2-3h2v2H4v-2z")
    }

    /** An open book in strokes (nav_read.svg), wherever the app shows a book. */
    val MenuBook: ImageVector by lazy {
        navIcon("MenuBook") {
            stroke("M3.4 5.2c3.2-.9 6-.4 8.6 1.4v13c-2.6-1.8-5.4-2.3-8.6-1.4z", width = 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            stroke("M20.6 5.2c-3.2-.9-6-.4-8.6 1.4v13c2.6-1.8 5.4-2.3 8.6-1.4z", width = 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            stroke("M12 6.6v13", width = 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        }
    }

    /** The phone's bottom bar: the design's own tab icons (Home, Read, Playlists, [Flashcards], More), drawn in strokes of 2.1 on a 24 grid. */
    private fun navIcon(name: String, draw: NavIconScope.() -> Unit): ImageVector = materialIcon(name = name) { NavIconScope(this).draw(); this }

    private class NavIconScope(private val builder: ImageVector.Builder) {
        fun stroke(data: String, width: Float = 2.1f, cap: StrokeCap = StrokeCap.Butt, join: StrokeJoin = StrokeJoin.Miter) {
            builder.addPath(pathData = addPathNodes(data), stroke = SolidColor(Color.Black), strokeLineWidth = width, strokeLineCap = cap, strokeLineJoin = join)
        }
        fun fill(data: String) { builder.addPath(pathData = addPathNodes(data), fill = SolidColor(Color.Black)) }
    }

    val NavHome: ImageVector by lazy {
        navIcon("NavHome") {
            stroke("M3.5 10.4 12 3.5l8.5 6.9v8.1a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2z", cap = StrokeCap.Round, join = StrokeJoin.Round)
            stroke("M9 20.5v-6.2c0-.7.6-1.3 1.3-1.3h3.4c.7 0 1.3.6 1.3 1.3v6.2", cap = StrokeCap.Round)
            fill("M10.75 8.7a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0Z")
        }
    }

    val NavRead: ImageVector by lazy {
        navIcon("NavRead") {
            stroke("M3.2 5.6c3.3-.9 6.2-.3 8.8 1.8 2.6-2.1 5.5-2.7 8.8-1.8v13.2c-3.3-.9-6.2-.3-8.8 1.8-2.6-2.1-5.5-2.7-8.8-1.8z", join = StrokeJoin.Round)
            stroke("M12 7.4v13.2", cap = StrokeCap.Round)
            fill("M16.2 5.2V11l1.8-1.2 1.8 1.2V5.4")
        }
    }

    val NavPlaylists: ImageVector by lazy {
        navIcon("NavPlaylists") {
            stroke("M6 4h12a3 3 0 0 1 3 3v10a3 3 0 0 1 -3 3h-12a3 3 0 0 1 -3 -3v-10a3 3 0 0 1 3 -3Z")
            stroke("M5.8 9a2.2 2.2 0 1 0 4.4 0a2.2 2.2 0 1 0 -4.4 0Z", width = 1.7f)
            fill("m7.5 7.9 1.9 1.1-1.9 1.1z")
            stroke("M12.3 8.2h5M12.3 11h3.4M6 15.2h12", cap = StrokeCap.Round)
        }
    }

    val NavMore: ImageVector by lazy {
        navIcon("NavMore") {
            fill(
                "M5.4 3h2.7a2.4 2.4 0 0 1 2.4 2.4v2.7a2.4 2.4 0 0 1 -2.4 2.4h-2.7a2.4 2.4 0 0 1 -2.4 -2.4v-2.7a2.4 2.4 0 0 1 2.4 -2.4Z" +
                    "M17.25 3a3.75 3.75 0 0 1 3.75 3.75a3.75 3.75 0 0 1 -3.75 3.75a3.75 3.75 0 0 1 -3.75 -3.75a3.75 3.75 0 0 1 3.75 -3.75Z" +
                    "M6.75 13.5a3.75 3.75 0 0 1 3.75 3.75a3.75 3.75 0 0 1 -3.75 3.75a3.75 3.75 0 0 1 -3.75 -3.75a3.75 3.75 0 0 1 3.75 -3.75Z" +
                    "M15.9 13.5h2.7a2.4 2.4 0 0 1 2.4 2.4v2.7a2.4 2.4 0 0 1 -2.4 2.4h-2.7a2.4 2.4 0 0 1 -2.4 -2.4v-2.7a2.4 2.4 0 0 1 2.4 -2.4Z",
            )
        }
    }

    /** The phone's Read sheet: the design's own icons for what there is to read, drawn as the bottom bar's. */
    val ReadCourses: ImageVector by lazy {
        navIcon("ReadCourses") {
            stroke("m3 8.2 9-4.8 9 4.8-9 4.8z", cap = StrokeCap.Round, join = StrokeJoin.Round)
            stroke("M6.5 10.2v5c0 1.7 2.5 3.2 5.5 3.2s5.5-1.5 5.5-3.2v-5", cap = StrokeCap.Round, join = StrokeJoin.Round)
            stroke("M21 8.2v6.2", cap = StrokeCap.Round)
            fill("M19.7 16.7a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0Z")
        }
    }

    val ReadBooks: ImageVector by lazy {
        navIcon("ReadBooks") {
            stroke("M4.7 4h2a1.5 1.5 0 0 1 1.5 1.5v13a1.5 1.5 0 0 1 -1.5 1.5h-2a1.5 1.5 0 0 1 -1.5 -1.5v-13a1.5 1.5 0 0 1 1.5 -1.5Z")
            stroke("M11 6.2h2a1.5 1.5 0 0 1 1.5 1.5v10.8a1.5 1.5 0 0 1 -1.5 1.5h-2a1.5 1.5 0 0 1 -1.5 -1.5v-10.8a1.5 1.5 0 0 1 1.5 -1.5Z")
            stroke("m16.8 4.4 3.6-.8 3.1 15.2-3.6.8z", join = StrokeJoin.Round)
            stroke("M4.8 16.8h1.8M11.1 16.8h1.8", cap = StrokeCap.Round)
        }
    }

    val ReadFindBooks: ImageVector by lazy {
        navIcon("ReadFindBooks") {
            stroke("M2.8 5.8c2.8-.8 5.3-.3 7.5 1.5 2.2-1.8 4.7-2.3 7.5-1.5v8.6c-2.8-.8-5.3-.3-7.5 1.5-2.2-1.8-4.7-2.3-7.5-1.5z", join = StrokeJoin.Round)
            stroke("M10.3 7.3v8.6", cap = StrokeCap.Round)
            stroke("M14.3 17.3a3.2 3.2 0 1 0 6.4 0a3.2 3.2 0 1 0 -6.4 0Z")
            stroke("m19.9 19.6 2 2", cap = StrokeCap.Round)
        }
    }

    val ReadArchive: ImageVector by lazy {
        navIcon("ReadArchive") {
            stroke("M4.5 8h15v10.7a2 2 0 0 1-2 2h-11a2 2 0 0 1-2-2z", join = StrokeJoin.Round)
            stroke("M4.6 3.5h14.8a1.6 1.6 0 0 1 1.6 1.6v1.8a1.6 1.6 0 0 1 -1.6 1.6h-14.8a1.6 1.6 0 0 1 -1.6 -1.6v-1.8a1.6 1.6 0 0 1 1.6 -1.6Z")
            stroke("M9 12h6", cap = StrokeCap.Round)
            fill("m12 10.7 2.5 2.6H9.5z")
        }
    }

    val ReadNews: ImageVector by lazy {
        navIcon("ReadNews") {
            stroke("M5.5 4h13a2.5 2.5 0 0 1 2.5 2.5v11a2.5 2.5 0 0 1 -2.5 2.5h-13a2.5 2.5 0 0 1 -2.5 -2.5v-11a2.5 2.5 0 0 1 2.5 -2.5Z")
            stroke("M3 8h18")
            fill("M5.8 13a2.2 2.2 0 1 0 4.4 0a2.2 2.2 0 1 0 -4.4 0Z")
            stroke("M12.5 11.4H18M12.5 14.3H18M6 17.3h12", cap = StrokeCap.Round)
            fill("M5.25 6a0.75 0.75 0 1 0 1.5 0a0.75 0.75 0 1 0 -1.5 0ZM7.95 6a0.75 0.75 0 1 0 1.5 0a0.75 0.75 0 1 0 -1.5 0Z")
        }
    }

    /** The phone's Flashcards sheet: the design's own icons for its pages, drawn as the bottom bar's. */
    val FlashcardsReview: ImageVector by lazy {
        navIcon("FlashcardsReview") {
            stroke("M7.5 6H5.8a2.3 2.3 0 0 0-2.3 2.3v9.9a2.3 2.3 0 0 0 2.3 2.3h8.4a2.3 2.3 0 0 0 2.3-2.3V18", cap = StrokeCap.Round)
            stroke("M9.8 3.5h8.4a2.3 2.3 0 0 1 2.3 2.3v9.9a2.3 2.3 0 0 1 -2.3 2.3h-8.4a2.3 2.3 0 0 1 -2.3 -2.3v-9.9a2.3 2.3 0 0 1 2.3 -2.3Z")
            stroke("M11.2 10.1a3.4 3.4 0 0 1 5.8-1.2", cap = StrokeCap.Round)
            stroke("M17 6.8v2.5h-2.5M16.8 11.7A3.4 3.4 0 0 1 11 13", cap = StrokeCap.Round, join = StrokeJoin.Round)
            stroke("M11 15.1v-2.5h2.5", cap = StrokeCap.Round, join = StrokeJoin.Round)
        }
    }

    val FlashcardsBrowse: ImageVector by lazy {
        navIcon("FlashcardsBrowse") {
            stroke("M5 3.5h3.5a2 2 0 0 1 2 2v3.5a2 2 0 0 1 -2 2h-3.5a2 2 0 0 1 -2 -2v-3.5a2 2 0 0 1 2 -2Z")
            stroke("M15.5 3.5h3.5a2 2 0 0 1 2 2v3.5a2 2 0 0 1 -2 2h-3.5a2 2 0 0 1 -2 -2v-3.5a2 2 0 0 1 2 -2Z")
            stroke("M5 14h3.5a2 2 0 0 1 2 2v2.5a2 2 0 0 1 -2 2h-3.5a2 2 0 0 1 -2 -2v-2.5a2 2 0 0 1 2 -2Z")
            stroke("M15.5 14h3.5a2 2 0 0 1 2 2v2.5a2 2 0 0 1 -2 2h-3.5a2 2 0 0 1 -2 -2v-2.5a2 2 0 0 1 2 -2Z")
            fill("m17.3 5.7.6 1.2 1.2.6-1.2.6-.6 1.2-.6-1.2-1.2-.6 1.2-.6z")
        }
    }

    val FlashcardsStatistics: ImageVector by lazy {
        navIcon("FlashcardsStatistics") {
            stroke("M6 3.5h12a3 3 0 0 1 3 3v11a3 3 0 0 1 -3 3h-12a3 3 0 0 1 -3 -3v-11a3 3 0 0 1 3 -3Z")
            stroke("M6.5 17.2h11", cap = StrokeCap.Round)
            fill("M8.2 12.5h0.4a1.2 1.2 0 0 1 1.2 1.2v2.3a1.2 1.2 0 0 1 -1.2 1.2h-0.4a1.2 1.2 0 0 1 -1.2 -1.2v-2.3a1.2 1.2 0 0 1 1.2 -1.2ZM12.1 9.4h0.4a1.2 1.2 0 0 1 1.2 1.2v5.4a1.2 1.2 0 0 1 -1.2 1.2h-0.4a1.2 1.2 0 0 1 -1.2 -1.2v-5.4a1.2 1.2 0 0 1 1.2 -1.2ZM16 6.5h0.4a1.2 1.2 0 0 1 1.2 1.2v8.3a1.2 1.2 0 0 1 -1.2 1.2h-0.4a1.2 1.2 0 0 1 -1.2 -1.2v-8.3a1.2 1.2 0 0 1 1.2 -1.2Z")
        }
    }

    /** The phone's More sheet: the design's own icons for its pages, drawn as the bottom bar's. Its flashcard settings take [Flashcards]. */
    val MoreVocabulary: ImageVector by lazy {
        navIcon("MoreVocabulary") {
            stroke("M6 4h12a3 3 0 0 1 3 3v10a3 3 0 0 1 -3 3h-12a3 3 0 0 1 -3 -3v-10a3 3 0 0 1 3 -3Z")
            fill("M5.8 9a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0 -2.4 0Z")
            fill("M5.8 15a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0 -2.4 0Z")
            stroke("M10.5 9H18M10.5 15H18", cap = StrokeCap.Round)
        }
    }

    val MoreWordFrequency: ImageVector by lazy {
        navIcon("MoreWordFrequency") {
            stroke("M3 20.5h18", cap = StrokeCap.Round)
            fill("M5.4 12.5h0.4a1.2 1.2 0 0 1 1.2 1.2v3.6a1.2 1.2 0 0 1 -1.2 1.2h-0.4a1.2 1.2 0 0 1 -1.2 -1.2v-3.6a1.2 1.2 0 0 1 1.2 -1.2Z")
            fill("M9.7 7.5h0.4a1.2 1.2 0 0 1 1.2 1.2v8.6a1.2 1.2 0 0 1 -1.2 1.2h-0.4a1.2 1.2 0 0 1 -1.2 -1.2v-8.6a1.2 1.2 0 0 1 1.2 -1.2Z")
            fill("M14 4h0.4a1.2 1.2 0 0 1 1.2 1.2v12.1a1.2 1.2 0 0 1 -1.2 1.2h-0.4a1.2 1.2 0 0 1 -1.2 -1.2v-12.1a1.2 1.2 0 0 1 1.2 -1.2Z")
            fill("M18.3 10h0.4a1.2 1.2 0 0 1 1.2 1.2v6.1a1.2 1.2 0 0 1 -1.2 1.2h-0.4a1.2 1.2 0 0 1 -1.2 -1.2v-6.1a1.2 1.2 0 0 1 1.2 -1.2Z")
        }
    }

    val MoreStatistics: ImageVector by lazy {
        navIcon("MoreStatistics") {
            stroke("M3.7 14h1.1a1.2 1.2 0 0 1 1.2 1.2v4.1a1.2 1.2 0 0 1 -1.2 1.2h-1.1a1.2 1.2 0 0 1 -1.2 -1.2v-4.1a1.2 1.2 0 0 1 1.2 -1.2Z")
            stroke("M8.7 8h1.1a1.2 1.2 0 0 1 1.2 1.2v10.1a1.2 1.2 0 0 1 -1.2 1.2h-1.1a1.2 1.2 0 0 1 -1.2 -1.2v-10.1a1.2 1.2 0 0 1 1.2 -1.2Z")
            stroke("M13.7 11h1.1a1.2 1.2 0 0 1 1.2 1.2v7.1a1.2 1.2 0 0 1 -1.2 1.2h-1.1a1.2 1.2 0 0 1 -1.2 -1.2v-7.1a1.2 1.2 0 0 1 1.2 -1.2Z")
            stroke("M18.7 3.5h1.1a1.2 1.2 0 0 1 1.2 1.2v14.6a1.2 1.2 0 0 1 -1.2 1.2h-1.1a1.2 1.2 0 0 1 -1.2 -1.2v-14.6a1.2 1.2 0 0 1 1.2 -1.2Z")
        }
    }

    val MoreSpeech: ImageVector by lazy {
        navIcon("MoreSpeech") {
            stroke("M4 9h4.2l5-4v14l-5-4H4a1.5 1.5 0 0 1-1.5-1.5v-3A1.5 1.5 0 0 1 4 9Z", join = StrokeJoin.Round)
            stroke("M16.5 8.2a5.3 5.3 0 0 1 0 7.6M19.2 5.5a9.2 9.2 0 0 1 0 13", cap = StrokeCap.Round)
        }
    }

    val MoreTranslation: ImageVector by lazy {
        navIcon("MoreTranslation") {
            stroke("M4 3.5h8.5a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H8L4.5 14v-2.5H4a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2Z", join = StrokeJoin.Round)
            stroke("M11.5 12.5H20a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-.5v2L16 20.5h-4.5a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2Z", join = StrokeJoin.Round)
            stroke("M5 7.5h5.5m-1.8-2 2 2-2 2M19 16.5h-5.5m1.8-2-2 2 2 2", width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        }
    }

    val MoreGeneral: ImageVector by lazy {
        navIcon("MoreGeneral") {
            stroke("m9.7 3.5.5-1.5h3.6l.5 1.5 1.7.7 1.4-.7L20 6.1l-.7 1.4.7 1.7 1.5.5v3.6l-1.5.5-.7 1.7.7 1.4-2.6 2.6-1.4-.7-1.7.7-.5 1.5h-3.6l-.5-1.5-1.7-.7-1.4.7L4.5 17l.7-1.4-.7-1.7-1.5-.5V9.7l1.5-.5.7-1.7-.7-1.4 2.6-2.6 1.4.7z", width = 1.9f, join = StrokeJoin.Round)
            stroke("M8.8 11.5a3.2 3.2 0 1 0 6.4 0a3.2 3.2 0 1 0 -6.4 0Z")
        }
    }

    val MoreGoals: ImageVector by lazy {
        navIcon("MoreGoals") {
            stroke("M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0Z")
            stroke("M6.7 12a5.3 5.3 0 1 0 10.6 0a5.3 5.3 0 1 0 -10.6 0Z")
            fill("M10.2 12a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0Z")
        }
    }

    val MoreLanguages: ImageVector by lazy {
        navIcon("MoreLanguages") {
            stroke("M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0Z")
            stroke("M3.4 9h17.2M3.4 15h17.2M12 3c2.2 2.4 3.3 5.4 3.3 9S14.2 18.6 12 21M12 3C9.8 5.4 8.7 8.4 8.7 12s1.1 6.6 3.3 9", cap = StrokeCap.Round)
        }
    }

    val MoreVocabularySettings: ImageVector by lazy {
        navIcon("MoreVocabularySettings") {
            stroke("M2.7 5.7c2.8-.8 5.2-.3 7.4 1.5 2.2-1.8 4.6-2.3 7.4-1.5v8.9c-2.8-.8-5.2-.3-7.4 1.5-2.2-1.8-4.6-2.3-7.4-1.5z", join = StrokeJoin.Round)
            stroke("M10.1 7.2v8.9", cap = StrokeCap.Round)
            stroke("M14.5 17.4a3.2 3.2 0 1 0 6.4 0a3.2 3.2 0 1 0 -6.4 0Z", width = 1.9f)
            fill("M16.6 17.4a1.1 1.1 0 1 0 2.2 0a1.1 1.1 0 1 0 -2.2 0Z")
            stroke("M17.7 12.8V14M17.7 20.8V22M13.1 17.4h1.2M21.1 17.4h1.2M14.5 14.2l.9.9M20 19.9l.9.9M20.9 14.2l-.9.9M15.4 19.9l-.9.9", width = 1.8f, cap = StrokeCap.Round)
        }
    }

    val MoreBackups: ImageVector by lazy {
        navIcon("MoreBackups") {
            stroke("M3 5.5a7.5 3 0 1 0 15 0a7.5 3 0 1 0 -15 0Z")
            stroke("M3 5.5v10.7c0 1.7 3.4 3 7.5 3 1.2 0 2.3-.1 3.3-.3", cap = StrokeCap.Round)
            stroke("M18 5.5v6.1M3 10.7c0 1.7 3.4 3 7.5 3 1.1 0 2.2-.1 3.1-.3", cap = StrokeCap.Round)
            stroke("M14 17a4 4 0 1 0 8 0a4 4 0 1 0 -8 0Z")
            stroke("m15.9 17 1.4 1.4 2.9-3.1", cap = StrokeCap.Round, join = StrokeJoin.Round)
        }
    }

    val MoreClearData: ImageVector by lazy {
        navIcon("MoreClearData") {
            stroke("M5.5 7h13l-.8 13.5H6.3z", join = StrokeJoin.Round)
            stroke("M3.5 7h17M8.5 7l.8-3h5.4l.8 3M9.5 11v5.5M14.5 11v5.5", cap = StrokeCap.Round)
        }
    }

    val MoreAbout: ImageVector by lazy {
        navIcon("MoreAbout") {
            stroke("M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0Z")
            fill("M10.75 7.8a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0Z")
            stroke("M12 11.5v5.2", width = 2.4f, cap = StrokeCap.Round)
        }
    }

    val MoreExperiments: ImageVector by lazy {
        navIcon("MoreExperiments") {
            stroke("M9 3h6M10 3v5.2L4.7 17a2.6 2.6 0 0 0 2.2 4h10.2a2.6 2.6 0 0 0 2.2-4L14 8.2V3", cap = StrokeCap.Round, join = StrokeJoin.Round)
            stroke("M7.4 15h9.2", cap = StrokeCap.Round)
            fill("M9 17.7a1 1 0 1 0 2 0a1 1 0 1 0 -2 0Z")
            fill("M13 18.3a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0 -2.4 0Z")
        }
    }

    /** The flashcards, wherever the bottom bar, the More sheet and the reader show them: two cards with a sparkle. */
    val Flashcards: ImageVector by lazy {
        navIcon("Flashcards") {
            stroke("M7.5 6H5.8a2.3 2.3 0 0 0-2.3 2.3v9.9a2.3 2.3 0 0 0 2.3 2.3h8.4a2.3 2.3 0 0 0 2.3-2.3V18", cap = StrokeCap.Round)
            stroke("M9.8 3.5h8.4a2.3 2.3 0 0 1 2.3 2.3v9.9a2.3 2.3 0 0 1 -2.3 2.3h-8.4a2.3 2.3 0 0 1 -2.3 -2.3v-9.9a2.3 2.3 0 0 1 2.3 -2.3Z")
            fill("m14 7.2.9 2 2 .9-2 .9-.9 2-.9-2-2-.9 2-.9z")
        }
    }

    /** The reader's stages: the design's own icons, drawn as the bottom bar's. The quiz takes [ReadCourses], the flashcards [Flashcards], reading aloud [MenuBook]. */
    val StageListen: ImageVector by lazy {
        navIcon("StageListen") {
            stroke("M4 12v-1a8 8 0 0 1 16 0v1", cap = StrokeCap.Round)
            stroke("M5.5 11.5a2.5 2.5 0 0 1 2.5 2.5v3.5a2.5 2.5 0 0 1 -2.5 2.5a2.5 2.5 0 0 1 -2.5 -2.5v-3.5a2.5 2.5 0 0 1 2.5 -2.5Z")
            stroke("M18.5 11.5a2.5 2.5 0 0 1 2.5 2.5v3.5a2.5 2.5 0 0 1 -2.5 2.5a2.5 2.5 0 0 1 -2.5 -2.5v-3.5a2.5 2.5 0 0 1 2.5 -2.5Z")
        }
    }

    val StageWords: ImageVector by lazy {
        navIcon("StageWords") {
            stroke("M6 3.5h12a3 3 0 0 1 3 3v11a3 3 0 0 1 -3 3h-12a3 3 0 0 1 -3 -3v-11a3 3 0 0 1 3 -3Z")
            fill("M5.75 8a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0Z")
            fill("M5.75 12a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0Z")
            fill("M5.75 16a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0Z")
            stroke("M10.5 8H17M10.5 12H17M10.5 16H15", cap = StrokeCap.Round)
        }
    }

    val StageReverse: ImageVector by lazy {
        navIcon("StageReverse") {
            stroke("M4 8h14.5M15 4.5 18.5 8 15 11.5M20 16H5.5M9 12.5 5.5 16 9 19.5", cap = StrokeCap.Round, join = StrokeJoin.Round)
        }
    }

    val StageRetell: ImageVector by lazy {
        navIcon("StageRetell") {
            stroke("M12 2.5a4 4 0 0 1 4 4v4a4 4 0 0 1 -4 4a4 4 0 0 1 -4 -4v-4a4 4 0 0 1 4 -4Z")
            stroke("M5.5 11.5v.8a6.5 6.5 0 0 0 13 0v-.8M12 18.8v3M8.5 21.8h7", cap = StrokeCap.Round)
        }
    }

    /** The Idiomiko monkey in its own colours (tools/logo/idiomiko-monkey.svg); draw it untinted. */
    val Logo: ImageVector by lazy {
        ImageVector.Builder(name = "Logo", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 1237.9f, viewportHeight = 1237.9f)
            .group(translationX = -96.5f, translationY = 81.4f) {
                addPath(pathData = addPathNodes(LOGO_PATH_0), fill = SolidColor(Color(0xFF022B46)))
                addPath(pathData = addPathNodes(LOGO_PATH_1), fill = SolidColor(Color(0xFFFD6947)))
                addPath(pathData = addPathNodes(LOGO_PATH_2), fill = SolidColor(Color(0xFFFAE7D1)))
                addPath(pathData = addPathNodes(LOGO_PATH_3), fill = SolidColor(Color(0xFF022B46)))
                addPath(pathData = addPathNodes(LOGO_PATH_4), fill = SolidColor(Color(0xFFFD6947)))
                group(rotate = 85.0f, pivotX = 812.0f, pivotY = 612.5f) { addPath(pathData = addPathNodes("M723.1 612.5A88.9 78.2 0 1 0 900.9 612.5A88.9 78.2 0 1 0 723.1 612.5Z"), fill = SolidColor(Color(0xFF022B46))) }
                group(rotate = 90.4f, pivotX = 514.0f, pivotY = 638.4f) { addPath(pathData = addPathNodes("M423.1 638.4A90.9 81.9 0 1 0 604.9 638.4A90.9 81.9 0 1 0 423.1 638.4Z"), fill = SolidColor(Color(0xFF022B46))) }
                group(rotate = 176.1f, pivotX = 684.7f, pivotY = 707.0f) { addPath(pathData = addPathNodes("M640.7 707.0A44.0 21.1 0 1 0 728.7 707.0A44.0 21.1 0 1 0 640.7 707.0Z"), fill = SolidColor(Color(0xFF022B46))) }
                group(rotate = 91.4f, pivotX = 834.3f, pivotY = 585.9f) { addPath(pathData = addPathNodes("M810.9 585.9A23.4 23.1 0 1 0 857.7 585.9A23.4 23.1 0 1 0 810.9 585.9Z"), fill = SolidColor(Color(0xFFFDFDFD))) }
                group(rotate = 100.8f, pivotX = 539.4f, pivotY = 610.5f) { addPath(pathData = addPathNodes("M515.3 610.5A24.1 23.8 0 1 0 563.5 610.5A24.1 23.8 0 1 0 515.3 610.5Z"), fill = SolidColor(Color(0xFFFDFDFD))) }
                addPath(pathData = addPathNodes("M1071.4 110.1L1002.5 247.8"), stroke = SolidColor(Color(0xFFF2A603)), strokeLineWidth = 80.4f, strokeLineCap = StrokeCap.Round)
                addPath(pathData = addPathNodes("M1235.2 205.5L1099.0 319.4"), stroke = SolidColor(Color(0xFFF2A603)), strokeLineWidth = 78.6f, strokeLineCap = StrokeCap.Round)
                addPath(pathData = addPathNodes("M1296.2 379.3L1161.3 418.2"), stroke = SolidColor(Color(0xFFF2A603)), strokeLineWidth = 76.2f, strokeLineCap = StrokeCap.Round)
            }
            .build()
    }

    private val LOGO_PATH_0 =
        "M679.5 146.1C684.2 145.6 686.7 145.6 690.0 145.6C693.3 145.6 696.3 145.8 699.3 146.2C702.3 146.5 705.3 147.0 707.8 147.7C710.4 148.3 712.5 149.2 714.4" +
        " 150.1C716.4 151.0 718.1 151.9 719.5 153.0C720.9 154.1 722.0 155.2 722.9 156.7C723.7 158.1 724.4 159.8 724.8 161.7C725.1 163.5 725.2 165.6 724.9 167.6" +
        "C724.7 169.6 724.2 171.8 723.4 173.9C722.5 176.0 724.8 174.1 719.7 180.2C714.7 186.4 699.6 202.9 693.1 210.6C686.7 218.4 685.0 220.7 681.0 226.5C677.0" +
        " 232.3 671.3 241.7 669.2 245.5C667.0 249.2 668.1 248.1 668.0 248.9C667.9 249.8 668.2 250.4 668.5 250.7C668.9 251.1 669.3 251.1 670.0 250.9C670.7 250.8" +
        " 668.4 252.5 672.7 249.9C676.9 247.2 688.1 239.5 695.7 235.2C703.2 230.8 710.7 227.0 718.2 223.7C725.7 220.4 733.2 217.5 740.7 215.3C748.3 213.0 758.4" +
        " 211.1 763.5 210.0C768.6 208.9 767.7 209.1 771.3 208.8C774.9 208.5 780.1 208.1 785.3 208.1C790.5 208.1 798.2 208.6 802.4 209.1C806.7 209.5 808.1 210.1" +
        " 810.8 210.7C813.4 211.4 816.2 212.3 818.4 213.1C820.6 213.9 822.1 214.6 824.0 215.5C825.8 216.5 828.0 218.0 829.5 219.0C830.9 220.1 831.8 220.9 832.6" +
        " 221.9C833.5 222.8 834.2 223.6 834.9 224.6C835.5 225.7 836.1 226.9 836.6 227.9C837.0 228.9 837.2 229.8 837.5 230.9C837.7 231.9 837.8 232.9 837.9 234.1" +
        "C837.9 235.4 837.8 236.9 837.6 238.2C837.4 239.6 837.0 240.9 836.5 242.0C836.1 243.2 835.7 244.1 834.9 245.4C834.0 246.7 832.9 248.2 831.4 249.9C829.9" +
        " 251.6 830.2 251.7 825.9 255.4C821.6 259.1 812.8 266.1 805.7 272.2C798.7 278.3 787.7 288.2 783.6 292.1C779.5 295.9 781.6 294.3 781.0 295.2C780.3 296.1" +
        " 780.1 296.6 779.9 297.3C779.7 298.0 779.7 298.8 779.8 299.4C779.9 300.0 780.1 300.3 780.5 300.8C781.0 301.2 781.7 301.8 782.5 302.2C783.3 302.6 782.3" +
        " 302.5 785.3 303.2C788.4 304.0 795.3 305.3 800.7 306.8C806.1 308.2 810.7 309.3 817.7 311.8C824.7 314.3 835.8 318.7 842.8 321.7C849.8 324.7 855.1 327.4" +
        " 859.7 329.8C864.4 332.1 866.3 333.1 870.8 335.7C875.3 338.4 882.3 342.7 886.8 345.7C891.4 348.6 894.0 350.5 898.0 353.5C902.0 356.5 906.7 360.3 910.8" +
        " 363.7C914.9 367.1 918.4 370.3 922.4 374.1C926.5 377.8 931.0 382.0 935.2 386.3C939.3 390.7 943.5 395.4 947.4 400.1C951.3 404.8 955.8 410.7 958.8 414.7" +
        "C961.8 418.7 962.8 420.3 965.4 424.1C967.9 428.0 970.6 432.1 973.8 437.7C977.1 443.2 982.8 453.9 984.9 457.6C987.0 461.2 986.0 459.1 986.6 459.8C987.2" +
        " 460.4 988.0 461.0 988.6 461.4C989.3 461.7 989.7 461.9 990.5 462.0C991.3 462.2 988.9 462.6 993.4 462.2C997.9 461.9 1011.7 460.4 1017.4 460.0C1023.2" +
        " 459.6 1022.6 459.6 1027.8 459.8C1032.9 460.0 1041.9 460.4 1048.2 461.3C1054.5 462.2 1059.9 463.6 1065.4 465.1C1071.0 466.5 1076.1 467.9 1081.6 469.9" +
        "C1087.1 471.9 1092.8 474.4 1098.3 477.2C1103.9 480.0 1109.3 483.0 1114.8 486.7C1120.4 490.3 1126.6 494.7 1131.6 498.9C1136.6 503.0 1141.3 507.9 1144.9" +
        " 511.6C1148.5 515.3 1151.1 518.5 1153.3 521.2C1155.5 523.8 1156.2 524.9 1158.0 527.5C1159.8 530.0 1162.2 533.6 1164.0 536.5C1165.8 539.3 1167.1 541.7" +
        " 1168.8 544.7C1170.4 547.8 1172.1 551.1 1173.8 554.7C1175.4 558.4 1177.2 562.3 1178.8 566.7C1180.5 571.0 1181.9 574.8 1183.5 581.0C1185.1 587.2 1187.5" +
        " 597.0 1188.6 603.9C1189.8 610.7 1190.1 615.7 1190.3 622.2C1190.6 628.6 1190.5 637.4 1190.3 642.8C1190.1 648.2 1189.7 650.7 1189.2 654.7C1188.8 658.8" +
        " 1188.0 663.2 1187.4 666.9C1186.8 670.6 1186.3 673.5 1185.5 677.0C1184.7 680.5 1184.2 683.1 1182.6 688.1C1181.0 693.2 1177.9 701.9 1175.9 707.4C1173.8" +
        " 712.8 1171.9 717.1 1170.2 720.7C1168.5 724.4 1167.9 725.5 1165.8 729.3C1163.7 733.0 1161.0 738.0 1157.6 743.1C1154.3 748.3 1148.7 756.1 1145.7 760.2" +
        "C1142.6 764.3 1143.6 763.4 1139.4 767.9C1135.2 772.3 1127.1 781.0 1120.4 786.9C1113.6 792.8 1104.6 799.4 1099.0 803.5C1093.3 807.5 1090.4 808.9 1086.6" +
        " 811.1C1082.7 813.2 1081.0 814.0 1075.7 816.2C1070.5 818.5 1061.2 822.4 1055.0 824.5C1048.8 826.6 1043.3 827.8 1038.4 828.9C1033.5 830.0 1031.6 830.4" +
        " 1025.6 831.1C1019.6 831.8 1008.0 832.7 1002.6 833.0C997.2 833.3 997.9 833.3 993.4 833.0C989.0 832.7 979.3 831.5 975.7 831.2C972.1 831.0 972.9 831.4" +
        " 971.8 831.6C970.7 831.9 970.1 832.0 969.1 832.7C968.1 833.5 967.4 833.9 965.7 836.2C964.1 838.6 962.7 841.4 959.2 846.7C955.7 852.0 948.8 862.1 944.5" +
        " 868.0C940.1 873.8 938.0 876.4 933.3 881.7C928.5 887.1 922.1 894.1 915.8 900.3C909.5 906.5 900.1 914.6 895.3 918.8C890.5 923.0 891.9 921.9 887.0 925.5" +
        "C882.2 929.2 872.0 936.7 866.2 940.7C860.4 944.7 859.4 945.5 852.2 949.7C845.0 953.8 830.8 961.6 823.1 965.6C815.3 969.5 812.9 970.4 805.9 973.4C798.8" +
        " 976.3 787.0 980.8 780.7 983.2C774.3 985.5 776.7 985.1 768.0 987.5C759.3 990.0 738.8 995.6 728.4 997.9C718.0 1000.2 714.8 1000.1 705.7 1001.2C696.7" +
        " 1002.3 681.8 1003.8 674.0 1004.5C666.2 1005.2 665.1 1005.3 658.9 1005.4C652.8 1005.6 645.8 1005.7 637.2 1005.3C628.7 1004.9 615.8 1003.7 607.6 1002.9" +
        "C599.5 1002.0 593.9 1001.2 588.2 1000.3C582.4 999.4 580.9 999.1 573.1 997.4C565.3 995.7 551.6 992.9 541.4 990.1C531.3 987.2 520.0 983.2 512.1 980.4" +
        "C504.1 977.7 501.1 976.6 494.0 973.5C486.8 970.5 476.7 965.8 469.3 962.2C461.9 958.6 454.4 954.8 449.4 952.1C444.4 949.4 445.6 950.2 439.3 946.2C433.1" +
        " 942.1 420.1 933.8 411.8 927.7C403.5 921.7 395.6 914.8 389.7 909.8C383.9 904.8 380.9 901.9 376.7 897.8C372.6 893.6 368.5 889.2 364.7 884.8C360.9 880.4" +
        " 357.6 876.2 354.0 871.5C350.3 866.8 346.7 862.2 342.9 856.6C339.1 850.9 333.3 841.2 331.0 837.6C328.6 834.0 329.5 835.7 328.8 835.0C328.1 834.3 327.9" +
        " 833.9 326.7 833.6C325.6 833.3 326.4 832.7 322.0 833.1C317.7 833.6 306.7 835.6 300.8 836.3C295.0 837.0 291.4 837.3 286.9 837.4C282.5 837.6 279.8 837.5" +
        " 274.3 837.2C268.8 836.8 261.4 836.6 254.1 835.4C246.9 834.1 237.6 831.6 230.9 829.6C224.3 827.6 218.2 824.9 214.1 823.4C210.0 821.8 209.4 821.5 206.4" +
        " 820.1C203.5 818.7 200.2 817.1 196.5 815.0C192.8 812.9 189.5 811.4 184.0 807.5C178.5 803.7 169.7 797.1 163.7 791.8C157.6 786.6 152.0 780.6 147.6 775.9" +
        "C143.2 771.1 140.6 767.7 137.2 763.3C133.9 758.8 130.2 753.5 127.3 749.2C124.5 744.8 122.4 741.4 120.2 737.3C118.0 733.1 116.0 728.5 114.1 724.4C112.3" +
        " 720.2 110.7 716.1 109.3 712.2C107.8 708.4 106.6 704.8 105.4 701.1C104.3 697.3 103.6 694.6 102.5 690.0C101.4 685.4 99.7 677.6 98.9 673.6C98.1 669.5" +
        " 98.1 669.5 97.7 665.8C97.4 662.0 96.8 656.0 96.6 650.9C96.5 645.8 96.4 640.7 96.7 635.2C97.0 629.7 97.6 624.2 98.5 618.0C99.4 611.7 100.9 603.4 102.2" +
        " 597.7C103.5 592.0 104.5 588.8 106.2 583.7C107.9 578.6 109.8 572.8 112.4 566.9C115.0 561.0 119.1 553.3 121.8 548.3C124.5 543.3 125.8 541.4 128.7 537.2" +
        "C131.5 532.9 135.0 527.6 139.1 522.6C143.3 517.6 148.9 511.7 153.7 507.2C158.5 502.6 162.5 499.3 167.9 495.4C173.3 491.5 180.1 487.1 186.1 483.6C192.1" +
        " 480.1 198.9 476.9 204.0 474.5C209.2 472.2 211.2 471.2 216.9 469.4C222.6 467.6 232.3 465.1 238.3 463.8C244.2 462.4 247.7 461.9 252.8 461.3C258.0 460.8" +
        " 261.2 460.5 269.0 460.5C276.9 460.5 294.1 461.1 300.0 461.1C305.8 461.1 303.0 460.9 304.2 460.6C305.3 460.3 305.8 460.3 307.0 459.5C308.1 458.6 307.6" +
        " 460.3 311.1 455.6C314.5 450.9 321.9 438.9 327.8 431.3C333.6 423.6 342.2 414.3 346.2 409.7C350.3 405.1 349.1 406.4 352.1 403.6C355.1 400.8 361.5 395.5" +
        " 364.3 392.8C367.2 390.2 368.1 389.3 369.3 387.8C370.6 386.4 371.1 385.6 371.7 384.2C372.2 382.9 372.7 385.3 372.8 379.7C373.0 374.2 372.4 358.6 372.6" +
        " 351.1C372.8 343.6 373.4 339.1 374.0 334.5C374.6 329.9 375.1 327.6 376.1 323.6C377.0 319.5 378.3 314.8 379.8 310.3C381.3 305.8 382.8 301.6 385.1 296.6" +
        "C387.4 291.5 390.6 285.0 393.6 280.1C396.5 275.2 399.9 270.9 402.8 267.3C405.7 263.7 409.1 260.6 411.2 258.7C413.3 256.8 414.0 256.5 415.2 255.7C416.4" +
        " 254.9 416.7 254.5 418.4 253.9C420.1 253.3 423.5 252.4 425.5 252.0C427.4 251.7 428.5 251.8 430.0 251.8C431.4 251.8 433.1 252.0 434.4 252.2C435.6 252.4" +
        " 436.4 252.6 437.5 253.1C438.5 253.5 439.4 253.8 440.7 254.8C442.0 255.9 443.5 257.2 445.2 259.3C446.9 261.4 447.0 260.7 451.0 267.5C455.0 274.3 465.8" +
        " 294.3 469.2 300.2C472.7 306.0 471.0 302.3 471.8 302.8C472.6 303.3 473.4 303.3 474.1 303.1C474.7 302.9 475.3 302.4 475.8 301.8C476.3 301.3 475.4 303.2" +
        " 477.2 299.6C479.1 296.0 483.7 286.0 486.7 280.2C489.7 274.4 491.8 270.8 495.4 264.9C499.1 259.1 504.8 250.6 508.7 245.2C512.7 239.8 515.4 237.0 519.2" +
        " 232.7C523.0 228.3 528.3 222.3 531.5 219.0C534.7 215.7 533.9 216.4 538.2 212.7C542.4 208.9 551.3 201.0 557.0 196.5C562.7 192.0 566.9 189.4 572.3 185.8" +
        "C577.7 182.2 584.4 178.1 589.6 175.1C594.8 172.2 598.1 170.6 603.6 168.1C609.0 165.5 616.8 162.1 622.3 159.8C627.9 157.5 632.2 155.9 636.9 154.4C641.5" +
        " 152.8 645.8 151.5 650.0 150.5C654.1 149.5 656.8 149.0 661.8 148.3C666.7 147.5 674.8 146.5 679.5 146.1Z"

    private val LOGO_PATH_1 =
        "M259.6 523.1C264.7 522.7 272.6 522.4 278.0 522.6C283.3 522.7 288.3 523.3 291.7 523.8C295.1 524.2 296.5 524.7 298.3 525.2C300.2 525.7 301.5 526.1 302.7" +
        " 526.7C303.9 527.3 304.8 527.9 305.5 528.6C306.2 529.4 306.7 530.2 306.8 531.3C306.9 532.4 307.3 532.1 306.3 535.2C305.4 538.2 302.8 543.8 300.9 549.4" +
        "C299.0 554.9 296.8 561.4 294.9 568.4C293.0 575.4 291.0 585.0 289.7 591.2C288.5 597.5 288.2 600.5 287.5 606.0C286.9 611.5 286.2 616.9 285.9 624.4C285.6" +
        " 631.9 285.3 641.9 285.6 650.9C285.9 660.0 287.0 671.7 287.7 678.8C288.5 685.9 289.1 688.5 290.1 693.4C291.0 698.4 291.7 702.3 293.2 708.3C294.7 714.3" +
        " 297.2 723.5 299.0 729.5C300.9 735.4 302.4 739.1 304.4 744.1C306.4 749.1 309.7 756.1 310.9 759.5C312.2 762.8 311.8 763.1 311.9 764.1C312.0 765.2 311.8" +
        " 765.4 311.5 765.9C311.3 766.5 311.3 766.8 310.6 767.4C309.8 768.1 309.0 768.9 307.2 769.7C305.3 770.5 302.5 771.3 299.5 772.0C296.5 772.7 292.7 773.4" +
        " 289.3 773.8C285.9 774.2 282.6 774.4 279.0 774.4C275.4 774.4 271.5 774.3 267.6 773.9C263.7 773.5 259.4 772.8 255.6 771.9C251.8 771.1 248.7 770.2 244.7" +
        " 768.8C240.6 767.4 235.0 765.0 231.3 763.2C227.5 761.5 225.4 760.4 222.1 758.4C218.8 756.4 215.0 753.7 211.5 751.0C208.0 748.3 204.1 744.9 201.2 742.3" +
        "C198.3 739.7 196.9 738.4 194.2 735.3C191.4 732.2 187.3 727.1 184.8 723.7C182.3 720.4 181.0 718.4 179.1 715.4C177.3 712.4 175.5 709.0 173.8 705.7C172.1" +
        " 702.4 170.5 699.0 169.1 695.4C167.6 691.9 166.4 688.4 165.1 684.4C163.9 680.3 162.5 675.2 161.6 670.9C160.6 666.7 160.0 663.0 159.5 659.0C159.0 655.0" +
        " 158.7 651.0 158.6 646.9C158.5 642.8 158.5 638.3 158.8 634.3C159.0 630.2 159.5 626.6 160.1 622.6C160.8 618.6 161.6 614.0 162.6 610.1C163.6 606.2 164.5" +
        " 603.0 165.9 599.4C167.2 595.7 169.0 591.6 170.6 588.1C172.3 584.6 173.8 581.7 175.8 578.3C177.8 575.0 180.1 571.5 182.6 568.1C185.1 564.8 188.5 560.9" +
        " 190.8 558.3C193.1 555.7 193.5 555.1 196.2 552.7C199.0 550.3 204.0 546.3 207.3 543.8C210.7 541.3 213.0 539.8 216.3 537.8C219.7 535.9 224.3 533.6 227.5" +
        " 532.0C230.8 530.5 232.7 529.7 236.1 528.6C239.4 527.4 243.8 526.1 247.7 525.2C251.6 524.3 254.6 523.6 259.6 523.1Z M1019.6 523.1C1022.3 522.9 1026.5" +
        " 522.6 1030.0 522.6C1033.4 522.6 1036.4 522.7 1040.2 523.3C1044.0 523.8 1048.9 524.8 1052.7 525.8C1056.6 526.8 1059.9 527.8 1063.3 529.2C1066.8 530.5" +
        " 1070.1 532.1 1073.6 533.9C1077.0 535.8 1081.2 538.3 1084.1 540.4C1087.1 542.4 1088.7 543.7 1091.4 546.1C1094.1 548.5 1097.9 552.3 1100.5 555.0C1103.1" +
        " 557.8 1105.0 560.0 1107.0 562.5C1109.0 565.0 1110.4 566.9 1112.3 570.2C1114.3 573.4 1116.9 578.1 1118.7 581.8C1120.5 585.5 1121.9 589.0 1123.1 592.4" +
        "C1124.4 595.8 1125.3 598.8 1126.2 602.3C1127.1 605.7 1127.8 608.9 1128.4 613.1C1129.1 617.3 1129.7 623.5 1130.0 627.5C1130.3 631.5 1130.4 632.8 1130.2" +
        " 636.8C1130.1 640.9 1129.5 647.5 1129.1 651.6C1128.6 655.6 1128.3 657.5 1127.6 661.1C1126.9 664.7 1125.9 669.4 1124.8 673.3C1123.7 677.2 1122.6 680.4" +
        " 1121.0 684.5C1119.4 688.6 1117.0 694.0 1115.2 697.7C1113.4 701.4 1112.4 703.2 1110.3 706.8C1108.1 710.3 1105.0 715.2 1102.4 718.9C1099.7 722.6 1097.4" +
        " 725.4 1094.4 728.9C1091.3 732.3 1087.0 736.6 1084.0 739.5C1081.0 742.3 1079.4 743.6 1076.4 745.9C1073.4 748.2 1069.3 751.0 1065.7 753.2C1062.1 755.5" +
        " 1058.5 757.5 1054.8 759.3C1051.0 761.0 1047.1 762.6 1043.4 763.9C1039.8 765.3 1036.3 766.3 1032.7 767.2C1029.0 768.0 1025.0 768.6 1021.4 768.9C1017.8" +
        " 769.3 1014.5 769.4 1011.1 769.3C1007.8 769.2 1004.1 768.7 1001.2 768.3C998.4 767.8 995.7 767.0 994.1 766.5C992.5 765.9 992.2 765.6 991.5 765.2C990.9" +
        " 764.7 990.5 764.3 990.1 763.9C989.8 763.4 989.5 762.8 989.3 762.3C989.1 761.7 989.0 761.4 989.1 760.4C989.2 759.4 988.6 760.2 990.0 756.3C991.4 752.5" +
        " 995.2 744.0 997.6 737.1C1000.0 730.2 1002.6 722.2 1004.6 715.1C1006.6 707.9 1008.1 702.3 1009.8 694.3C1011.4 686.2 1013.3 674.1 1014.3 666.8C1015.4" +
        " 659.5 1015.5 656.5 1016.0 650.5C1016.5 644.5 1017.1 637.7 1017.3 630.8C1017.5 623.9 1017.4 616.3 1017.2 609.3C1016.9 602.3 1016.4 595.5 1015.7 588.8" +
        "C1015.1 582.1 1014.8 578.9 1013.2 569.2C1011.7 559.6 1007.3 537.6 1006.2 530.7C1005.2 523.8 1006.5 528.6 1007.0 527.8C1007.6 526.9 1008.3 526.3 1009.4" +
        " 525.7C1010.5 525.1 1011.9 524.6 1013.6 524.1C1015.3 523.7 1016.9 523.4 1019.6 523.1Z"

    private val LOGO_PATH_2 =
        "M767.6 418.1C772.2 417.5 773.3 417.6 778.0 417.5C782.7 417.5 791.2 417.5 795.8 417.7C800.3 418.0 802.0 418.4 805.5 419.0C809.1 419.6 813.0 420.4 817.0" +
        " 421.5C821.1 422.6 825.4 423.9 829.9 425.6C834.4 427.3 839.6 429.7 843.8 431.8C847.9 433.8 851.4 435.7 854.8 437.7C858.2 439.7 860.5 441.1 863.9 443.6" +
        "C867.3 446.0 872.0 449.7 875.2 452.3C878.4 455.0 879.9 456.4 883.0 459.5C886.1 462.6 890.3 466.7 893.9 470.6C897.4 474.6 901.2 479.5 904.1 483.4C907.1" +
        " 487.3 909.1 490.1 911.5 494.0C914.0 497.9 916.4 501.9 918.9 506.6C921.4 511.2 924.4 517.2 926.6 521.9C928.8 526.5 930.3 530.0 932.0 534.5C933.7 538.9" +
        " 935.2 543.3 936.9 548.6C938.6 553.9 940.7 561.6 942.0 566.5C943.3 571.4 943.5 572.8 944.5 578.0C945.4 583.2 947.0 592.4 947.7 597.8C948.5 603.2 948.6" +
        " 604.9 949.1 610.4C949.5 616.0 950.2 623.3 950.3 631.2C950.5 639.0 950.5 649.4 950.1 657.6C949.8 665.9 948.9 674.1 948.2 680.7C947.5 687.3 946.6 692.7" +
        " 945.9 697.4C945.2 702.0 944.9 703.6 943.9 708.4C942.9 713.3 941.5 720.1 939.8 726.3C938.2 732.5 935.7 740.2 934.0 745.5C932.3 750.8 931.7 752.8 929.6" +
        " 758.1C927.5 763.3 923.9 771.8 921.6 777.1C919.2 782.3 918.7 783.8 915.3 789.8C911.9 795.7 905.1 806.9 901.3 812.8C897.5 818.6 895.4 821.2 892.5 825.0" +
        "C889.7 828.8 887.3 831.9 884.1 835.6C881.0 839.4 877.0 844.0 873.9 847.4C870.8 850.8 869.8 852.0 865.5 856.0C861.2 860.0 852.6 867.7 848.0 871.5C843.4" +
        " 875.4 841.4 876.8 837.9 879.4C834.3 882.0 830.8 884.5 826.7 887.2C822.5 889.9 817.0 893.3 813.1 895.6C809.3 897.9 807.5 898.9 803.5 901.0C799.4 903.1" +
        " 793.8 905.9 788.7 908.2C783.7 910.6 780.5 912.2 773.3 914.8C766.2 917.5 756.0 921.4 745.7 924.2C735.4 927.0 721.0 930.0 711.3 931.8C701.6 933.6 695.8" +
        " 934.2 687.5 935.0C679.1 935.7 671.2 936.3 661.1 936.2C651.0 936.2 635.3 935.2 627.0 934.5C618.6 933.9 616.4 933.3 611.0 932.5C605.7 931.6 599.5 930.5" +
        " 594.9 929.5C590.4 928.6 587.5 928.0 583.5 927.0C579.5 926.0 575.2 924.8 571.0 923.5C566.7 922.3 562.3 920.9 558.0 919.5C553.8 918.0 550.9 917.0 545.7" +
        " 914.8C540.4 912.7 531.9 909.1 526.7 906.8C521.5 904.5 520.3 904.1 514.5 901.0C508.8 897.8 499.6 892.6 492.4 888.1C485.3 883.5 477.1 877.6 471.8 873.7" +
        "C466.5 869.8 464.7 868.0 460.8 864.7C456.9 861.4 453.1 858.2 448.6 853.9C444.1 849.6 437.9 843.4 433.7 838.8C429.4 834.2 426.4 830.5 423.1 826.4C419.8" +
        " 822.4 417.3 819.2 413.9 814.6C410.5 810.0 406.2 803.7 402.9 798.6C399.7 793.5 397.4 789.5 394.5 784.0C391.6 778.6 387.8 770.9 385.5 766.0C383.1 761.2" +
        " 382.0 758.8 380.4 755.1C378.8 751.3 378.2 750.3 376.0 743.5C373.8 736.7 369.4 723.1 367.2 714.3C364.9 705.6 363.7 699.8 362.3 691.2C360.9 682.6 359.5" +
        " 671.7 358.8 662.7C358.0 653.7 357.5 647.2 357.8 637.2C358.1 627.3 359.8 610.7 360.6 603.0C361.3 595.3 361.7 595.0 362.4 590.9C363.2 586.8 364.0 583.0" +
        " 365.1 578.6C366.2 574.2 367.5 569.5 369.1 564.6C370.7 559.7 372.1 555.1 374.6 549.1C377.2 543.2 381.6 534.1 384.3 528.8C387.0 523.5 388.3 521.3 390.8" +
        " 517.3C393.4 513.4 396.1 509.4 399.5 505.0C402.8 500.5 407.5 494.6 411.1 490.6C414.7 486.6 417.3 484.2 421.2 480.7C425.1 477.3 429.7 473.5 434.6 470.1" +
        "C439.4 466.6 445.9 462.8 450.6 460.1C455.3 457.5 458.7 455.9 462.7 454.2C466.7 452.5 470.9 451.2 474.6 450.1C478.3 449.0 481.4 448.3 485.0 447.5C488.5" +
        " 446.7 492.1 445.9 495.8 445.4C499.6 444.8 504.2 444.4 507.6 444.1C510.9 443.8 512.7 443.7 515.9 443.8C519.2 443.8 523.2 444.2 526.9 444.6C530.7 445.0" +
        " 532.5 445.0 538.3 446.2C544.1 447.4 555.3 449.9 561.7 451.8C568.1 453.7 571.1 455.0 576.8 457.7C582.4 460.4 590.4 464.8 595.7 467.9C600.9 470.9 604.3" +
        " 473.4 608.3 476.2C612.3 479.0 614.8 480.7 619.6 484.9C624.4 489.1 633.6 498.1 637.3 501.2C640.9 504.3 640.2 503.1 641.5 503.6C642.9 504.0 644.3 504.2" +
        " 645.6 504.1C646.9 503.9 648.2 503.5 649.4 502.8C650.7 502.0 650.1 503.4 653.1 499.6C656.1 495.8 662.8 486.0 667.5 480.0C672.2 474.1 677.3 468.5 681.5" +
        " 464.0C685.7 459.5 688.5 456.9 692.7 453.2C696.9 449.6 702.1 445.3 706.6 442.1C711.0 438.9 714.4 436.8 719.6 434.1C724.7 431.3 732.0 427.8 737.2 425.7" +
        "C742.4 423.6 745.7 422.6 750.8 421.3C755.9 420.0 763.1 418.8 767.6 418.1Z"

    private val LOGO_PATH_3 =
        "M765.5 752.5C770.9 751.4 767.7 752.4 768.6 752.6C769.6 752.7 770.6 752.9 771.4 753.3C772.3 753.7 773.0 754.2 773.7 754.9C774.4 755.5 774.9 756.1 775.4" +
        " 757.1C776.0 758.0 776.5 759.3 777.0 760.5C777.4 761.8 777.7 762.4 777.9 764.6C778.0 766.7 778.4 769.2 778.0 773.5C777.6 777.9 776.4 785.4 775.3 790.8" +
        "C774.2 796.2 772.8 801.4 771.4 805.9C769.9 810.3 768.6 813.7 766.9 817.4C765.2 821.0 762.8 824.8 761.3 827.8C759.8 830.8 759.0 833.1 757.8 835.3C756.7" +
        " 837.5 755.7 839.1 754.4 840.9C753.1 842.8 752.3 843.9 749.9 846.4C747.6 848.9 743.3 853.3 740.4 855.9C737.6 858.6 735.4 860.4 732.8 862.3C730.3 864.3" +
        " 727.6 866.1 725.1 867.6C722.7 869.2 720.7 870.3 718.1 871.6C715.5 872.8 712.3 874.1 709.6 875.1C706.9 876.1 704.9 876.7 701.9 877.4C699.0 878.1 695.2" +
        " 878.9 691.9 879.4C688.5 879.8 685.1 880.2 681.8 880.3C678.6 880.4 675.7 880.4 672.3 880.2C669.0 879.9 665.1 879.4 661.6 878.9C658.2 878.3 654.8 877.5" +
        " 651.8 876.7C648.7 875.9 646.4 875.1 643.5 874.0C640.6 872.9 637.4 871.4 634.6 869.9C631.7 868.4 629.0 866.8 626.4 865.1C623.8 863.4 621.1 861.4 618.9" +
        " 859.6C616.6 857.9 615.3 856.7 613.0 854.5C610.7 852.3 607.3 848.8 605.2 846.3C603.0 843.8 602.8 844.4 600.0 839.5C597.2 834.7 590.9 822.4 588.4 817.1" +
        "C585.9 811.8 585.9 810.8 584.9 807.6C583.8 804.5 583.0 802.2 582.2 798.3C581.3 794.5 580.3 788.4 579.9 784.7C579.4 780.9 579.6 778.6 579.6 776.0C579.6" +
        " 773.5 579.7 771.2 580.0 769.4C580.2 767.7 580.5 766.5 580.9 765.4C581.3 764.3 581.8 763.6 582.3 762.8C582.9 762.1 583.5 761.5 584.4 761.0C585.3 760.4" +
        " 586.7 759.9 587.8 759.6C588.9 759.3 589.8 759.2 591.0 759.2C592.3 759.1 590.3 758.7 595.3 759.4C600.3 760.0 613.5 762.4 621.2 763.3C628.9 764.1 636.2" +
        " 764.1 641.6 764.6C646.9 765.0 647.8 765.9 653.3 766.2C658.9 766.5 666.5 766.7 674.8 766.3C683.2 766.0 696.5 764.6 703.5 764.0C710.5 763.3 711.3 763.2" +
        " 716.8 762.3C722.3 761.5 728.3 760.6 736.4 758.9C744.6 757.3 760.2 753.6 765.5 752.5Z"

    private val LOGO_PATH_4 =
        "M665.6 801.1C670.2 800.7 676.9 800.5 682.0 800.5C687.1 800.5 691.5 800.6 696.3 801.2C701.0 801.8 705.9 802.8 710.5 804.0C715.2 805.2 720.5 807.1 724.1" +
        " 808.4C727.7 809.7 729.5 810.5 731.8 811.7C734.2 812.8 736.0 813.8 738.3 815.2C740.7 816.5 743.3 818.2 745.7 819.8C748.1 821.4 750.9 823.6 752.5 825.0" +
        "C754.2 826.3 754.8 826.9 755.6 827.8C756.4 828.7 757.0 829.7 757.4 830.5C757.8 831.4 757.9 832.0 757.9 832.9C757.8 833.9 757.8 834.7 757.2 836.1C756.6" +
        " 837.4 755.5 839.3 754.4 840.9C753.3 842.6 752.6 843.6 750.4 845.9C748.3 848.2 744.0 852.5 741.5 855.0C738.9 857.4 737.2 858.9 735.1 860.6C733.0 862.3" +
        " 730.9 863.8 728.8 865.3C726.6 866.8 724.3 868.3 722.0 869.5C719.8 870.8 717.7 871.8 715.3 872.8C713.0 873.9 710.5 874.8 708.1 875.6C705.8 876.4 703.7" +
        " 877.0 701.1 877.6C698.6 878.2 695.5 878.8 692.7 879.2C690.0 879.7 687.3 880.0 684.7 880.2C682.1 880.3 679.7 880.4 677.1 880.4C674.4 880.3 671.5 880.1" +
        " 668.7 879.8C665.8 879.5 662.7 879.1 659.9 878.6C657.1 878.0 654.4 877.4 651.8 876.7C649.2 876.0 646.7 875.2 644.2 874.3C641.7 873.3 638.9 872.1 636.6" +
        " 870.9C634.2 869.8 632.6 868.9 630.1 867.4C627.6 865.9 624.1 863.6 621.7 861.8C619.3 860.0 618.0 859.1 615.6 856.9C613.2 854.7 609.5 851.0 607.1 848.4" +
        "C604.7 845.8 602.6 843.3 601.2 841.3C599.8 839.3 599.0 837.9 598.5 836.5C598.0 835.1 597.9 834.1 598.3 832.8C598.7 831.6 599.0 830.6 600.6 829.0C602.2" +
        " 827.5 605.1 825.3 607.9 823.4C610.6 821.4 613.6 819.4 617.0 817.5C620.4 815.5 624.8 813.3 628.2 811.7C631.6 810.1 634.3 809.0 637.4 807.9C640.4 806.7" +
        " 643.5 805.7 646.4 804.9C649.2 804.1 651.3 803.6 654.5 803.0C657.7 802.4 661.1 801.6 665.6 801.1Z"
}
