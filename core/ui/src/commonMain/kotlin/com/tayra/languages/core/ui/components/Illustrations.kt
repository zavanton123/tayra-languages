package com.tayra.languages.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.TayraTheme
import org.jetbrains.compose.resources.painterResource
import tayra_languages.core.ui.generated.resources.Res
import tayra_languages.core.ui.generated.resources.illustration_courses

/** An open book in front of two text cards, on a soft disc: the courses page before there are courses. */
@Composable
fun CoursesIllustration(modifier: Modifier = Modifier) {
    val dimmed = if (TayraTheme.current.isDark) Modifier.alpha(AppAlpha.muted) else Modifier
    Image(painterResource(Res.drawable.illustration_courses), contentDescription = null, modifier = modifier.then(dimmed))
}
