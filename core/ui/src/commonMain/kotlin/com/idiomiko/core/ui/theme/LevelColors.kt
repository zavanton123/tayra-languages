package com.idiomiko.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.idiomiko.core.domain.courses.CourseLevel

/** Each European level's colour, the same wherever a level is shown: the courses' journey and badges, and the vocabulary level's cards. */
val CourseLevel.tint: Color
    get() = when (this) {
        CourseLevel.A1 -> AppAccents.green
        CourseLevel.A2 -> AppAccents.blue
        CourseLevel.B1 -> AppAccents.purple
        CourseLevel.B2 -> AppAccents.orange
        CourseLevel.C1 -> AppAccents.red
        CourseLevel.C2 -> AppAccents.teal
    }
