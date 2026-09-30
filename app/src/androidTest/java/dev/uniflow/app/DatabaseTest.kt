package dev.uniflow.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.uniflow.app.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    @Test fun crudAndCascadeDoNotLeaveOrphans() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), UniDatabase::class.java).build()
        try {
            val dao = db.dao()
            val id = dao.save(Course(name = "Algorithms"))
            val recordId = dao.save(StudyRecord(kind = "TASK", title = "Assignment", courseId = id))
            val task = dao.record(recordId)!!
            dao.save(task.copy(done = true))
            assertTrue(dao.record(recordId)!!.done)
            dao.delete(dao.courses().first().single())
            assertTrue(dao.records().first().isEmpty())
        } finally { db.close() }
    }
    @Test fun dataSurvivesDatabaseCloseAndReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "persistence-test.db"
        context.deleteDatabase(name)
        try {
            val first = Room.databaseBuilder(context, UniDatabase::class.java, name).build()
            val id = first.dao().save(Course(name = "Persistent course"))
            first.dao().save(StudyRecord(kind = "NOTE", title = "Persistent note", courseId = id))
            first.close()
            val second = Room.databaseBuilder(context, UniDatabase::class.java, name).build()
            assertEquals("Persistent note", second.dao().records().first().single().title)
            second.close()
        } finally { context.deleteDatabase(name) }
    }
}
