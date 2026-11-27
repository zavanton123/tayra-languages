package com.idiomiko.core.ui.theme

import com.idiomiko.core.domain.settings.UserSettings
import kotlin.test.Test
import kotlin.test.assertEquals

/** GitHub Light is the theme the app starts in, first in the list; the old default stays as Classic. */
class AppThemesTest {

    @Test
    fun gitHubLightIsTheDefaultAndFirst() {
        assertEquals("github_light", AppThemes.default.id)
        assertEquals(AppThemes.default, AppThemes.all.first())
        assertEquals(AppThemes.default, AppThemes.byId(UserSettings.DEFAULT_THEME))
        assertEquals(1, AppThemes.all.count { it.id == "github_light" }, "listed once")
    }

    @Test
    fun theOldDefaultMovesToGitHubLightAndStaysAsClassic() {
        assertEquals(AppThemes.default, AppThemes.byId("default"), "those who never chose a theme get the new default")
        assertEquals("Classic", AppThemes.byId("classic").englishLabel)
        assertEquals(AppThemes.classic, AppThemes.all[1])
    }
}
