package dev.uniflow.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class Kind { CLASS, TASK, EXAM, GRADE, ATTENDANCE, NOTE }

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val teacher: String = "", val email: String = "",
    val office: String = "", val credits: Int = 3, val color: Int = 0
)

// One normalized table for student records. The category controls validation and UI.
@Entity(tableName = "records", foreignKeys = [ForeignKey(entity = Course::class,
    parentColumns = ["id"], childColumns = ["courseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("courseId"), Index(value = ["kind", "day"])])
data class StudyRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String, val title: String, val body: String = "", val courseId: Long? = null,
    val day: Long = 0, val weekday: Int = 1, val start: Int = 540, val end: Int = 630,
    val place: String = "", val priority: Int = 1, val done: Boolean = false,
    val score: Double = 0.0, val maximum: Double = 100.0, val weight: Double = 1.0,
    val present: Boolean = true, val pinned: Boolean = false
)

@Dao
interface StudyDao {
    @Query("SELECT * FROM courses ORDER BY name COLLATE NOCASE") fun courses(): Flow<List<Course>>
    @Query("SELECT * FROM records ORDER BY day, start, id") fun records(): Flow<List<StudyRecord>>
    @Query("SELECT * FROM records WHERE kind IN ('TASK','EXAM') AND done = 0 AND day >= :today")
    suspend fun upcoming(today: Long): List<StudyRecord>
    @Upsert suspend fun save(course: Course): Long
    @Upsert suspend fun save(record: StudyRecord): Long
    @Delete suspend fun delete(course: Course)
    @Delete suspend fun delete(record: StudyRecord)
    @Query("SELECT count(*) FROM courses") suspend fun courseCount(): Int
    @Query("DELETE FROM records") suspend fun clearRecords()
    @Query("DELETE FROM courses") suspend fun clearCourses()
    @Query("SELECT * FROM records WHERE id = :id") suspend fun record(id: Long): StudyRecord?
}

@Database(entities = [Course::class, StudyRecord::class], version = 1, exportSchema = true)
abstract class UniDatabase : RoomDatabase() {
    abstract fun dao(): StudyDao
    companion object {
        fun create(context: Context) = Room.databaseBuilder(context, UniDatabase::class.java, "uniflow.db").build()
    }
}
