package com.tayra.languages.core.ui.components

import androidx.compose.material.icons.materialIcon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

/** Icons that the core material icon set does not include. */
object AppIcons {
    private fun icon(name: String, pathData: String): ImageVector =
        materialIcon(name = name) { addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black)) }

    val VolumeUp: ImageVector by lazy {
        icon(
            "VolumeUp",
            "M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z",
        )
    }

    val Book: ImageVector by lazy {
        icon(
            "Book",
            "M21 5c-1.11-.35-2.33-.5-3.5-.5-1.95 0-4.05.4-5.5 1.5-1.45-1.1-3.55-1.5-5.5-1.5S2.45 4.9 1 6v14.65c0 .25.25.5.5.5.1 0 .15-.05.25-.05C3.1 20.45 5.05 20 6.5 20c1.95 0 4.05.4 5.5 1.5 1.35-.85 3.8-1.5 5.5-1.5 1.65 0 3.35.3 4.75 1.05.1.05.15.05.25.05.25 0 .5-.25.5-.5V6c-.6-.45-1.25-.75-2-1zm0 13.5c-1.1-.35-2.3-.5-3.5-.5-1.7 0-4.15.65-5.5 1.5V8c1.35-.85 3.8-1.5 5.5-1.5 1.2 0 2.4.15 3.5.5v11.5z",
        )
    }

    val Globe: ImageVector by lazy {
        icon(
            "Globe",
            "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95c-.32-1.25-.78-2.45-1.38-3.56 1.84.63 3.37 1.91 4.33 3.56zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2s.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56-1.84-.63-3.37-1.9-4.33-3.56zm2.95-8H5.08c.96-1.66 2.49-2.93 4.33-3.56C8.81 5.55 8.35 6.75 8.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2s.07-1.35.16-2h4.68c.09.65.16 1.32.16 2s-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95c-.96 1.65-2.49 2.93-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2s-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z",
        )
    }

    val BarChart: ImageVector by lazy { icon("BarChart", "M5 9.2h3V19H5zM10.6 5h2.8v14h-2.8zm5.6 8H19v6h-2.8z") }

    val Flame: ImageVector by lazy {
        icon(
            "Flame",
            "M11.57 13.16c-1.36.28-2.17 1.16-2.17 2.41 0 1.34 1.11 2.42 2.49 2.42 2.05 0 3.71-1.66 3.71-3.71 0-1.07-.15-2.12-.46-3.12-.79 1.07-2.2 1.72-3.57 2zM13.5.67s.74 2.65.74 4.8c0 2.06-1.35 3.73-3.41 3.73-2.07 0-3.63-1.67-3.63-3.73l.03-.36C5.21 7.51 4 10.62 4 14c0 4.42 3.58 8 8 8s8-3.58 8-8C20 8.61 17.41 3.8 13.5.67z",
        )
    }

    val ViewList: ImageVector by lazy { icon("ViewList", "M3 14h4v-4H3v4zm0 5h4v-4H3v4zM3 9h4V5H3v4zm5 5h13v-4H8v4zm0 5h13v-4H8v4zM8 5v4h13V5H8z") }

    val GridView: ImageVector by lazy {
        icon("GridView", "M3 3v8h8V3H3zm6 6H5V5h4v4zm-6 4v8h8v-8H3zm6 6H5v-4h4v4zm4-16v8h8V3h-8zm6 6h-4V5h4v4zm-6 4v8h8v-8h-8zm6 6h-4v-4h4v4z")
    }

    val SwapVert: ImageVector by lazy { icon("SwapVert", "M16 17.01V10h-2v7.01h-3L15 21l4-3.99h-3zM9 3L5 6.99h3V14h2V6.99h3L9 3z") }

    val UnfoldMore: ImageVector by lazy {
        icon("UnfoldMore", "M12 5.83L15.17 9l1.41-1.41L12 3 7.41 7.59 8.83 9 12 5.83zm0 12.34L8.83 15l-1.41 1.41L12 21l4.59-4.59L15.17 15 12 18.17z")
    }

    val Bolt: ImageVector by lazy {
        icon("Bolt", "M11 21h-1l1-7H7.5c-.58 0-.57-.32-.38-.66.19-.34.05-.08.07-.12C8.48 10.94 10.42 7.54 13 3h1l-1 7h3.5c.49 0 .56.33.47.51l-.07.15C12.96 17.55 11 21 11 21z")
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

    val OpenInFull: ImageVector by lazy { icon("OpenInFull", "M21 11V3h-8l3.29 3.29-10 10L3 13v8h8l-3.29-3.29 10-10z") }

    val Fullscreen: ImageVector by lazy { icon("Fullscreen", "M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z") }

    val Page: ImageVector by lazy {
        icon("Page", "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z")
    }

    val PageAdd: ImageVector by lazy {
        icon("PageAdd", "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 14h-3v3h-2v-3H8v-2h3v-3h2v3h3v2zm-3-7V3.5L18.5 9H13z")
    }

    val Bookmark: ImageVector by lazy { icon("Bookmark", "M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2zm0 15l-5-2.18L7 18V5h10v13z") }

    val BookmarkAdd: ImageVector by lazy {
        icon("BookmarkAdd", "M17 11v6.97l-5-2.14-5 2.14V5h6V3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V11h-2zm4-4h-2v2h-2V7h-2V5h2V3h2v2h2v2z")
    }

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

    val Abc: ImageVector by lazy {
        icon(
            "Abc",
            "M21 11h-1.5v-.5h-2v3h2V13H21v1c0 .55-.45 1-1 1h-3c-.55 0-1-.45-1-1v-4c0-.55.45-1 1-1h3c.55 0 1 .45 1 1v1zM8 10v5H6.5v-1.5h-2V15H3v-5c0-.55.45-1 1-1h3c.55 0 1 .45 1 1zm-1.5.5h-2V12h2v-1.5zm7 1.5c.55 0 1 .45 1 1v1c0 .55-.45 1-1 1h-4V9h4c.55 0 1 .45 1 1v1c0 .55-.45 1-1 1zm-2.5-1.5v.5h2v-.5h-2zm2 2.5h-2v.5h2V13z",
        )
    }

    val Download: ImageVector by lazy { icon("Download", "M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z") }

    val Tune: ImageVector by lazy {
        icon("Tune", "M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z")
    }

    /**
     * The Tayra otter from the vector logo. The traced SVG is drawn in tenths of points with a
     * flipped y axis, so the inner group applies that transform and the outer one crops to the
     * otter's bounding box.
     */
    val Otter: ImageVector by lazy {
        ImageVector.Builder(name = "Otter", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 400f, viewportHeight = 400f)
            .group(translationX = -585.25f, translationY = -168.9f) {
                group(translationY = 1024f, scaleX = 0.1f, scaleY = -0.1f) {
                    addPath(pathData = addPathNodes(OTTER_HEAD), fill = SolidColor(Color.Black))
                    addPath(pathData = addPathNodes(OTTER_EYE), fill = SolidColor(Color.Black))
                }
            }
            .build()
    }

    private val OTTER_HEAD =
            "M7560 8388 c-106 -9 -283 -41 -355 -63 -45 -13 -52 -12 -138 17 -134 44 -275 49 -411 14 -55 -14 -125 -40 -155 " +
            "-58 -77 -46 -190 -161 -228 -233 -75 -143 -95 -329 -52 -490 11 -44 21 -84 21 -90 1 -5 -25 -50 -56 -100 -159 " +
            "-251 -233 -498 -243 -820 -8 -268 22 -451 113 -676 98 -244 223 -428 425 -625 305 -296 649 -464 1119 -546 109 " +
            "-20 161 -22 405 -22 221 0 301 3 380 17 137 24 314 72 388 105 l62 28 -72 42 c-367 215 -597 546 -621 897 -21 " +
            "302 69 545 261 702 118 97 221 131 471 157 242 26 366 55 481 115 131 68 236 201 294 372 15 46 42 107 60 134 34 " +
            "53 61 132 61 174 0 51 -46 127 -104 173 -97 76 -785 488 -971 581 -142 71 -372 149 -507 171 -193 33 -437 42 " +
            "-628 24z m-679 -198 c127 -18 235 -84 295 -180 19 -30 34 -60 34 -67 0 -7 -25 5 -56 27 -88 61 -169 85 -294 85 " +
            "-95 0 -111 -3 -162 -28 -107 -52 -171 -149 -171 -258 0 -113 63 -210 175 -269 35 -18 64 -33 65 -34 1 -1 -18 -40 " +
            "-41 -88 l-43 -87 -39 26 c-101 67 -219 216 -248 313 -35 117 -26 243 24 341 54 105 188 202 304 219 73 11 83 11 " +
            "157 0z m1546 -241 c178 -66 233 -297 93 -394 -56 -39 -141 -57 -207 -45 -57 10 -122 39 -142 64 -26 31 -87 61 " +
            "-136 67 l-50 5 38 17 c31 14 37 22 37 47 0 40 33 123 65 166 25 33 82 68 135 85 46 14 109 9 167 -12z m530 -40 " +
            "c192 -75 525 -228 566 -260 15 -12 10 -14 -45 -16 -86 -3 -139 -19 -183 -56 -127 -104 -60 -286 125 -343 30 -9 " +
            "57 -18 58 -20 11 -9 -67 -140 -100 -169 -153 -131 -487 -12 -568 203 -39 102 -14 199 73 279 55 51 82 65 177 93 " +
            "136 41 134 50 -23 182 -67 57 -134 111 -147 120 -36 25 -20 22 67 -13z m-231 -657 c1 -66 50 -157 115 -216 100 " +
            "-90 230 -132 383 -124 147 8 243 70 302 198 37 80 39 83 52 88 22 7 13 -42 -17 -93 -38 -66 -161 -182 -236 -222 " +
            "-118 -62 -226 -81 -586 -103 -406 -24 -616 -89 -889 -274 -128 -86 -290 -245 -380 -373 -71 -101 -147 -242 -169 " +
            "-313 -25 -77 -31 -68 -31 46 0 194 45 387 133 567 23 48 41 87 39 87 -22 0 -229 -195 -266 -250 -21 -33 -20 -16 " +
            "5 66 26 84 76 186 138 279 64 97 238 269 342 338 173 114 320 165 574 197 187 23 311 56 409 109 39 21 73 39 76 " +
            "40 3 0 5 -20 6 -47z " +
            ""

    private val OTTER_EYE =
            "M8301 7910 c-96 -22 -147 -128 -107 -223 26 -64 111 -117 186 -117 36 0 110 40 133 71 26 36 37 93 26 135 -25 89 " +
            "-143 155 -238 134z m152 -71 c16 -26 0 -68 -29 -75 -46 -12 -85 51 -52 84 17 17 68 11 81 -9z " +
            ""
}
