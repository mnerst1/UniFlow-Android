package dev.uniflow.app.domain

import dev.uniflow.app.data.*
import org.junit.Assert.*
import org.junit.Test

class AnalyticsTest {
    private fun grade(course: Long, score: Double, maximum: Double = 100.0, weight: Double = 1.0) = StudyRecord(kind = "GRADE", title = "Test", courseId = course, score = score, maximum = maximum, weight = weight)
    @Test fun weightedGradesNormalizeDifferentScales() {
        assertEquals(75.0, Analytics.percentage(listOf(grade(1, 50.0), grade(1, 10.0, 10.0)))!!, .001)
        assertEquals(87.5, Analytics.percentage(listOf(grade(1, 50.0), grade(1, 10.0, 10.0, 3.0)))!!, .001)
    }
    @Test fun gpaUsesCreditsAndExcludesUngradedCourses() {
        val courses = listOf(Course(1, "A", credits = 3), Course(2, "B", credits = 1), Course(3, "Ungraded", credits = 10))
        assertEquals(3.25, Analytics.gpa(courses, listOf(grade(1, 95.0), grade(2, 50.0)))!!, .001)
    }
    @Test fun attendanceOnlyCountsAttendanceRecords() {
        assertEquals(50.0, Analytics.attendance(listOf(StudyRecord(kind = "ATTENDANCE", title = "A"), StudyRecord(kind = "ATTENDANCE", title = "B", present = false), grade(1, 80.0)))!!, .001)
        assertNull(Analytics.attendance(emptyList()))
        assertNull(Analytics.gpa(emptyList(), emptyList()))
    }
    @Test fun rejectsBadGradesAndBrokenTimeRanges() {
        assertEquals(Validation.NUMBER, validate(grade(1, 101.0)))
        assertEquals(Validation.NUMBER, validate(grade(1, Double.NaN)))
        assertEquals(Validation.NUMBER, validate(grade(1, 1.0, 0.0)))
        assertEquals(Validation.TIME, validate(StudyRecord(kind = "CLASS", title = "A", courseId = 1, start = 600, end = 599)))
        assertEquals(Validation.COURSE, validate(StudyRecord(kind = "TASK", title = "A")))
        assertNull(validate(StudyRecord(kind = "NOTE", title = "A")))
    }
    @Test fun gradeScaleBoundaries() {
        assertEquals(4.0, Analytics.points(95.0), .001)
        assertEquals(3.67, Analytics.points(94.9), .001)
        assertEquals(1.0, Analytics.points(50.0), .001)
        assertEquals(0.0, Analytics.points(49.9), .001)
    }
}
