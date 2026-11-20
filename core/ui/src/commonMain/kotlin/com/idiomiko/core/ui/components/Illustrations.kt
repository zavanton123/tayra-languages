package com.idiomiko.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.IdiomikoTheme
import org.jetbrains.compose.resources.painterResource
import idiomiko.core.ui.generated.resources.Res
import idiomiko.core.ui.generated.resources.illustration_courses
import idiomiko.core.ui.generated.resources.illustration_flashcards

/** An open book in front of two text cards, on a soft disc: the courses page before there are courses. */
@Composable
fun CoursesIllustration(modifier: Modifier = Modifier) {
    val dimmed = if (IdiomikoTheme.current.isDark) Modifier.alpha(AppAlpha.muted) else Modifier
    Image(painterResource(Res.drawable.illustration_courses), contentDescription = null, modifier = modifier.then(dimmed))
}

/** The mascot holding up a fan of flashcards: Home's next step when cards are due. */
@Composable
fun FlashcardsIllustration(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.illustration_flashcards), contentDescription = null, modifier = modifier)
}
