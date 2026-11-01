package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppSpacing

/** The page sizes a long list offers. */
val LIST_PAGE_SIZES = listOf(25, 50, 100)

/** How many pages [total] items fill at [pageSize] a page; one when there are none. */
fun pageCount(total: Int, pageSize: Int): Int = if (total == 0) 1 else (total + pageSize - 1) / pageSize

/** Under a long list: how many items a page holds, which of them are shown, and the way to the pages before and after. */
@Composable
fun ListPager(page: Int, pageSize: Int, total: Int, onPageSize: (Int) -> Unit, onPage: (Int) -> Unit, compact: Boolean = false) {
    // With every item on one page at the smallest page size there is nothing to page through or resize.
    if (page == 0 && total <= LIST_PAGE_SIZES.first()) return
    val colors = MaterialTheme.colorScheme
    val pages = pageCount(total, pageSize)
    val from = if (total == 0) 0 else page * pageSize + 1
    val to = minOf(total, (page + 1) * pageSize)
    Row(Modifier.fillMaxWidth().padding(top = AppSpacing.xl), verticalAlignment = Alignment.CenterVertically) {
        SelectMenu(LIST_PAGE_SIZES, pageSize, onPageSize, { tr("{0} per page", it) })
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            val pagerTint = { enabled: Boolean -> if (enabled) colors.primary else colors.outlineVariant }
            AppIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, tr("Previous page"), { onPage(page - 1) }, enabled = page > 0, style = IconButtonStyle.Outlined, tint = pagerTint(page > 0))
            Text(tr("{0}–{1} of {2}", from, to, total), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            AppIconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, tr("Next page"), { onPage(page + 1) }, enabled = page < pages - 1, style = IconButtonStyle.Outlined, tint = pagerTint(page < pages - 1))
        }
        if (!compact) Spacer(Modifier.weight(1f))
    }
}
