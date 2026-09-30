package dev.uniflow.app

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.workDataOf
import androidx.work.testing.TestListenableWorkerBuilder
import dev.uniflow.app.data.*
import dev.uniflow.app.notifications.DeadlineWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ReminderTest {
    @Test fun deadlineNotificationUsesCurrentRecordAndDoesNotRepeat() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<UniFlowApplication>()
        val before = app.preferences.settings.first()
        if (Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(app.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val dao = app.database.dao()
        val course = dao.save(Course(name = "Notification QA"))
        val day = LocalDate.now().toEpochDay()
        val id = dao.save(StudyRecord(kind = "TASK", title = "Reminder QA", courseId = course, day = day))
        val nm = app.getSystemService(NotificationManager::class.java)
        try {
            app.preferences.save(before.copy(reminders = true))
            fun worker() = TestListenableWorkerBuilder<DeadlineWorker>(app).setInputData(workDataOf("id" to id, "day" to day)).build()
            worker().doWork()
            assertTrue(nm.activeNotifications.any { it.id == id.toInt() })
            dao.save(dao.record(id)!!.copy(title = "Edited after reminder"))
            worker().doWork()
            assertEquals("Reminder QA", nm.activeNotifications.first { it.id == id.toInt() }.notification.extras.getString("android.title"))
        } finally {
            app.preferences.save(before)
            nm.cancel(id.toInt())
            dao.delete(dao.courses().first().first { it.id == course })
            app.getSharedPreferences("sent_reminders", Context.MODE_PRIVATE).edit().remove("$id:$day").apply()
        }
    }
}
