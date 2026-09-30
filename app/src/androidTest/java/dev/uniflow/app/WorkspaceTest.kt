package dev.uniflow.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File

class WorkspaceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun screenshot(name: String) {
        rule.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File) ?: context.getExternalFilesDir("qa")!!
        directory.mkdirs()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun waitFor(text: String) { rule.waitUntil(15000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    @Test fun screensAndThreeLanguagesRender() {
        val app = rule.activity.application as UniFlowApplication
        fun preferences(language: String, theme: String) = runBlocking {
            app.preferences.save(app.preferences.settings.first().copy(language = language, theme = theme))
        }
        try {
            preferences("en", "light")
            rule.waitUntil(15000) { rule.onAllNodesWithContentDescription("Home").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithContentDescription("Home").performClick()
            waitFor("Your campus, in sync")
            screenshot("home-en-light")
            rule.onNodeWithContentDescription("More").performClick()
            val routes = listOf("Calendar & exams", "Grades & insights", "Attendance", "Notes", "Teachers", "Settings")
            routes.forEach { title ->
                rule.onNodeWithTag("page-list").performScrollToNode(hasText(title))
                waitFor(title)
                rule.onAllNodesWithText(title).onFirst().performClick()
                waitFor(title)
                if (title == "Calendar & exams") screenshot("calendar-en-light")
                rule.onNodeWithContentDescription("More").performClick()
            }
            preferences("ru", "dark")
            rule.waitUntil(15000) { rule.onAllNodesWithContentDescription("Главная").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithContentDescription("Главная").performClick()
            waitFor("Учёба в одном месте")
            val pixels = rule.onNodeWithText("Привет, Student").captureToImage().toPixelMap()
            assertTrue("Dark theme heading must use a bright foreground", (0 until pixels.height step 2).any { y -> (0 until pixels.width step 2).any { x -> pixels[x, y].luminance() > .65f } })
            screenshot("home-ru-dark")
            preferences("kk", "dark")
            rule.waitUntil(15000) { rule.onAllNodesWithContentDescription("Тағы").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithContentDescription("Тағы").performClick()
            rule.onNodeWithTag("page-list").performScrollToNode(hasText("Бағалар мен талдау"))
            waitFor("Бағалар мен талдау")
            rule.onNodeWithText("Бағалар мен талдау").performClick()
            waitFor("Салмақталған орташа")
            screenshot("grades-kk-dark")
        } finally { preferences("en", "light") }
    }
}
