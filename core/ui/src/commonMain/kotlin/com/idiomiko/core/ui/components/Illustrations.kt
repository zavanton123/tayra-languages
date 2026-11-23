package com.idiomiko.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.IdiomikoTheme
import org.jetbrains.compose.resources.painterResource
import idiomiko.core.ui.generated.resources.Res
import idiomiko.core.ui.generated.resources.archived_books_empty_monkey
import idiomiko.core.ui.generated.resources.courses_loading_monkey
import idiomiko.core.ui.generated.resources.find_books_monkey_v4
import idiomiko.core.ui.generated.resources.illustration_courses
import idiomiko.core.ui.generated.resources.illustration_flashcards
import idiomiko.core.ui.generated.resources.illustration_journey
import idiomiko.core.ui.generated.resources.vocabulary_empty_monkey

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

/** The mascot with a map and a backpack: the next lesson of a course. */
@Composable
fun JourneyIllustration(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.illustration_journey), contentDescription = null, modifier = modifier)
}

/** The mascot peering into an empty box of set-aside books: the archive with nothing in it. */
@Composable
fun ArchivedBooksIllustration(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.archived_books_empty_monkey), contentDescription = null, modifier = modifier)
}

/** The mascot looking through a magnifying glass at an empty card, for a vocabulary with no words to show. */
@Composable
fun VocabularyEmptyIllustration(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.vocabulary_empty_monkey), contentDescription = null, modifier = modifier)
}

/** The mascot reading through a magnifying glass beside a stack of books: finding books to read. */
@Composable
fun FindBooksIllustration(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.find_books_monkey_v4), contentDescription = null, modifier = modifier)
}

/** The mascot stacking books and lesson cards, while a language's courses are being prepared. */
@Composable
fun CoursesLoadingIllustration(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.courses_loading_monkey), contentDescription = null, modifier = modifier)
}
