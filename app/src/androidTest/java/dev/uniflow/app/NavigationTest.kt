package dev.uniflow.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import dev.uniflow.app.data.Kind
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*

class NavigationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun dashboardAndEditorCanBeOpened() {
        rule.waitUntil(15000) { rule.onAllNodesWithText("UniFlow").fetchSemanticsNodes().isNotEmpty() }
        // Content descriptions survive locale changes by reading the active app resources.
        val search = rule.activity.getString(R.string.search)
        rule.waitUntil(15000) { rule.onAllNodesWithContentDescription(search).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithContentDescription(search).onFirst().performClick()
        rule.waitUntil(15000) { rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Algorithms")
        rule.onAllNodes(hasText("Algorithms") and !hasSetTextAction()).onFirst().assertExists()
    }

    @Test fun taskCreatedInUiSurvivesActivityRecreation() {
        val app = rule.activity.application as UniFlowApplication
        runBlocking { app.preferences.save(app.preferences.settings.first().copy(language = "en")) }
        rule.waitUntil(15000) { rule.onAllNodesWithContentDescription("Tasks").fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithContentDescription("Tasks").onFirst().performClick()
        rule.waitUntil(15000) { rule.onAllNodesWithContentDescription("Add").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Add").performClick()
        val title = "UI persistence check"
        try {
            rule.onNode(hasSetTextAction() and hasText("Title")).performTextInput(title)
            rule.onNodeWithText("Save").performClick()
            rule.waitUntil(10000) { runBlocking { app.database.dao().records().first().any { it.title == title && it.kind == Kind.TASK.name } } }
            rule.activityRule.scenario.recreate()
            rule.waitForIdle()
            assertTrue(runBlocking { app.database.dao().records().first().any { it.title == title } })
        } finally {
            runBlocking { app.database.dao().records().first().filter { it.title == title }.forEach { app.database.dao().delete(it) } }
        }
    }
}
