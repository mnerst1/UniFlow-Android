package dev.uniflow.app.domain

import dev.uniflow.app.data.*

object Analytics {
    fun percentage(grades: List<StudyRecord>): Double? {
        val valid = grades.filter { it.maximum > 0 && it.weight > 0 }
        if (valid.isEmpty()) return null
        return valid.sumOf { it.score / it.maximum * 100 * it.weight } / valid.sumOf { it.weight }
    }
    fun points(percent: Double) = when {
        percent >= 95 -> 4.0; percent >= 90 -> 3.67; percent >= 85 -> 3.33
        percent >= 80 -> 3.0; percent >= 75 -> 2.67; percent >= 70 -> 2.33
        percent >= 65 -> 2.0; percent >= 60 -> 1.67; percent >= 55 -> 1.33
        percent >= 50 -> 1.0; else -> 0.0
    }
    fun gpa(courses: List<Course>, records: List<StudyRecord>): Double? {
        val values = courses.mapNotNull { c -> percentage(records.filter { it.kind == Kind.GRADE.name && it.courseId == c.id })?.let { points(it) to c.credits } }
        val credits = values.sumOf { it.second }
        return if (credits == 0) null else values.sumOf { it.first * it.second } / credits
    }
    fun attendance(records: List<StudyRecord>): Double? = records.filter { it.kind == Kind.ATTENDANCE.name }.let {
        if (it.isEmpty()) null else it.count { r -> r.present } * 100.0 / it.size
    }
}

enum class Validation { TITLE, COURSE, DATE, TIME, NUMBER }
fun validate(record: StudyRecord): Validation? = when {
    record.title.isBlank() -> Validation.TITLE
    record.kind != Kind.NOTE.name && record.courseId == null -> Validation.COURSE
    record.kind in listOf(Kind.CLASS.name, Kind.EXAM.name) && (record.weekday !in 1..7 || record.start !in 0..1439 || record.end !in 1..1439 || record.end <= record.start) -> Validation.TIME
    record.kind == Kind.GRADE.name && (!record.score.isFinite() || !record.maximum.isFinite() || !record.weight.isFinite() || record.maximum <= 0 || record.score < 0 || record.score > record.maximum || record.weight <= 0) -> Validation.NUMBER
    else -> null
}
