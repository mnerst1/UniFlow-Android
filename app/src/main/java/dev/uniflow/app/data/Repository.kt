package dev.uniflow.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class Repository(val db: UniDatabase, val preferences: Preferences) {
    private val dao = db.dao()
    val courses = dao.courses()
    val records = dao.records()
    suspend fun save(value: Course) = dao.save(value)
    suspend fun save(value: StudyRecord) = dao.save(value)
    suspend fun delete(value: Course) = dao.delete(value)
    suspend fun delete(value: StudyRecord) = dao.delete(value)
    suspend fun seedIfNeeded() {
        if (preferences.initialized.first()) return
        db.withTransaction { if (dao.courseCount() == 0) seed() }
        preferences.initialized()
    }
    suspend fun resetDemo() {
        db.withTransaction { dao.clearRecords(); dao.clearCourses(); seed() }
        preferences.initialized()
    }
    private suspend fun seed() {
        val today = LocalDate.now()
        val courses = listOf(
            Course(name = "Algorithms", teacher = "Dana Sadykova", email = "dana@example.edu", office = "B-305", credits = 5, color = 0),
            Course(name = "Linear Algebra", teacher = "Alex Kim", email = "alex@example.edu", office = "A-204", credits = 4, color = 1),
            Course(name = "Academic English", teacher = "Aigerim Nur", email = "aigerim@example.edu", office = "C-102", credits = 3, color = 2),
            Course(name = "Databases", teacher = "Sam Lee", email = "sam@example.edu", office = "B-210", credits = 5, color = 3)
        ).map { dao.save(it) }
        courses.forEachIndexed { index, id ->
            for (offset in 0..1) dao.save(StudyRecord(kind = "CLASS", title = if (offset == 0) "Lecture" else "Practice", courseId = id, weekday = (today.dayOfWeek.value + index + offset - 1) % 7 + 1, start = 540 + index * 100, end = 620 + index * 100, place = "${'A' + index}-20${index + 1}"))
            dao.save(StudyRecord(kind = "GRADE", title = "Midterm", courseId = id, day = today.minusDays(5).toEpochDay(), score = 92.0 - index * 4, weight = 2.0))
            dao.save(StudyRecord(kind = "GRADE", title = "Coursework", courseId = id, day = today.minusDays(2).toEpochDay(), score = 96.0 - index * 3))
            for (offset in 1..5) dao.save(StudyRecord(kind = "ATTENDANCE", title = "Lecture", courseId = id, day = today.minusDays(offset.toLong()).toEpochDay(), present = offset != 4 || index % 2 == 0))
        }
        dao.save(StudyRecord(kind = "TASK", title = "Implement Dijkstra’s algorithm", body = "Compare adjacency lists and matrices. Include complexity analysis and unit tests.", courseId = courses[0], day = today.plusDays(1).toEpochDay(), priority = 2))
        dao.save(StudyRecord(kind = "TASK", title = "Matrix decomposition", body = "Complete exercises 4–12.", courseId = courses[1], day = today.plusDays(3).toEpochDay()))
        dao.save(StudyRecord(kind = "TASK", title = "Design a normalized schema", courseId = courses[3], day = today.toEpochDay(), priority = 2))
        dao.save(StudyRecord(kind = "TASK", title = "Read research paper", courseId = courses[2], day = today.minusDays(1).toEpochDay(), done = true))
        dao.save(StudyRecord(kind = "EXAM", title = "Algorithms final", courseId = courses[0], day = today.plusDays(12).toEpochDay(), start = 600, end = 720, place = "B-305"))
        dao.save(StudyRecord(kind = "EXAM", title = "Algebra quiz", courseId = courses[1], day = today.plusDays(5).toEpochDay(), start = 660, end = 720, place = "A-204"))
        dao.save(StudyRecord(kind = "NOTE", title = "Study sprint", body = "1. Review lecture notes\n2. Practice retrieval\n3. Take a short break\n4. Teach the concept to a friend", day = today.toEpochDay(), pinned = true))
        dao.save(StudyRecord(kind = "NOTE", title = "Database normalization", body = "1NF: atomic values\n2NF: no partial dependencies\n3NF: no transitive dependencies", courseId = courses[3], day = today.toEpochDay()))
    }
}
