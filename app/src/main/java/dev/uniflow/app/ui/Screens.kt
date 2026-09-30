package dev.uniflow.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.uniflow.app.R
import dev.uniflow.app.data.*
import dev.uniflow.app.domain.Analytics
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable fun Page(content: LazyListScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 840.dp).fillMaxSize().testTag("page-list"), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}
@Composable fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 8.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun RecordView(r: StudyRecord, state: StudyState, actions: RecordActions) {
    RecordCard(r, state.courses, { actions.edit(r) }, { actions.delete(r) }, if (r.kind in listOf("TASK", "EXAM", "ATTENDANCE")) ({ actions.toggle(r) }) else null)
}
@Composable fun Dashboard(state: StudyState, navigate: (String) -> Unit, addTask: () -> Unit, actions: RecordActions) {
    val today = LocalNow.current.toLocalDate()
    val now = LocalNow.current.toLocalTime().let { it.hour * 60 + it.minute }
    val todayClasses = state.records.filter { it.kind == "CLASS" && it.weekday == today.dayOfWeek.value }.sortedBy { it.start }
    val next = todayClasses.firstOrNull { it.end > now }
    val tasks = state.records.filter { it.kind == "TASK" }
    val completed = tasks.count { it.done }
    val upcoming = state.records.filter { it.kind in listOf("TASK", "EXAM") && !it.done }.sortedWith(compareBy<StudyRecord> { it.day }.thenByDescending { it.priority }).take(4)
    Page {
        item {
            Text(stringResource(R.string.greeting, state.settings.name), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.dashboard), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(dateLabel(today.toEpochDay()), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
        }
        item {
            val colors = MaterialTheme.colorScheme
            Surface(shape = RoundedCornerShape(28.dp), color = colors.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.background(Brush.linearGradient(listOf(colors.primary.copy(alpha = .14f), colors.secondary.copy(alpha = .12f)))).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Bolt, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.next_class), style = MaterialTheme.typography.labelLarge) }
                    Text(next?.let { r -> state.courses.find { it.id == r.courseId }?.name ?: r.title } ?: stringResource(R.string.no_classes), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (next != null) Text("${timeLabel(next.start)} – ${timeLabel(next.end)} · ${next.place}")
                    TextButton(onClick = { navigate("schedule") }) { Text(stringResource(R.string.schedule)); Icon(Icons.Outlined.ChevronRight, null) }
                }
            }
        }
        item {
            GlassCard {
                Row(Modifier.fillMaxWidth()) {
                    Metric(Analytics.gpa(state.courses, state.records)?.let { "%.2f".format(it) } ?: "—", stringResource(R.string.gpa), Modifier.weight(1f))
                    Metric(Analytics.attendance(state.records)?.let { "%.0f%%".format(it) } ?: "—", stringResource(R.string.attendance), Modifier.weight(1f))
                    Metric(tasks.count { !it.done }.toString(), stringResource(R.string.pending), Modifier.weight(1f))
                }
                Row { Text(stringResource(R.string.progress), Modifier.weight(1f)); Text("$completed / ${tasks.size}") }
                LinearProgressIndicator(progress = { if (tasks.isEmpty()) 0f else completed.toFloat() / tasks.size }, modifier = Modifier.fillMaxWidth())
            }
        }
        item { SectionTitle(stringResource(R.string.quick_access)) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WorkspaceTile(R.string.calendar, Icons.Outlined.Event, Modifier.weight(1f)) { navigate("calendar") }
            WorkspaceTile(R.string.grades, Icons.AutoMirrored.Outlined.TrendingUp, Modifier.weight(1f)) { navigate("grades") }
        } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WorkspaceTile(R.string.notes, Icons.AutoMirrored.Outlined.StickyNote2, Modifier.weight(1f)) { navigate("notes") }
            WorkspaceTile(R.string.attendance, Icons.Outlined.CheckCircle, Modifier.weight(1f)) { navigate("attendance") }
        } }
        item { SectionTitle(stringResource(R.string.today), stringResource(R.string.view_all)) { navigate("schedule") } }
        if (todayClasses.isEmpty()) item { Text(stringResource(R.string.weekend), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else items(todayClasses, key = { "today${it.id}" }) { RecordView(it, state, actions) }
        item { SectionTitle(stringResource(R.string.upcoming), stringResource(R.string.add_task), addTask) }
        if (upcoming.isEmpty()) item { EmptyCard(addTask) }
        else items(upcoming, key = { "upcoming${it.id}" }) { RecordView(it, state, actions) }
    }
}
@Composable fun WorkspaceTile(label: Int, icon: ImageVector, modifier: Modifier, click: () -> Unit) {
    Surface(modifier = modifier, onClick = click, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .95f), contentColor = MaterialTheme.colorScheme.onSurface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .18f))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(label), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable fun ScheduleScreen(state: StudyState, add: (Long) -> Unit, actions: RecordActions) {
    var offset by rememberSaveable { mutableLongStateOf(0) }
    var selected by rememberSaveable { mutableIntStateOf(LocalDate.now().dayOfWeek.value) }
    val monday = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(offset)
    val date = monday.plusDays(selected.toLong() - 1)
    val classes = state.records.filter { it.kind == "CLASS" && it.weekday == selected }.sortedBy { it.start }
    val exams = state.records.filter { it.kind == "EXAM" && it.day == date.toEpochDay() }
    val conflict = classes.zipWithNext().any { (a, b) -> a.end > b.start }
    val locale = LocalConfiguration.current.locales[0]
    Page {
        item { GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { offset-- }) { Icon(Icons.Outlined.ChevronLeft, stringResource(R.string.week_previous)) }
                Text("${dateLabel(monday.toEpochDay())} – ${dateLabel(monday.plusDays(6).toEpochDay())}", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = { offset++ }) { Icon(Icons.Outlined.ChevronRight, stringResource(R.string.week_next)) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                (1..7).forEach { day ->
                    val d = monday.plusDays(day.toLong() - 1)
                    Surface(onClick = { selected = day }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = if (day == selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent) {
                        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(DayOfWeek.of(day).getDisplayName(TextStyle.NARROW, locale), style = MaterialTheme.typography.labelSmall)
                            Text(d.dayOfMonth.toString(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Text(stringResource(R.string.recurring), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        item { SectionTitle(dateLabel(date.toEpochDay()), stringResource(R.string.add_class)) { add(date.toEpochDay()) } }
        if (conflict) item { Text(stringResource(R.string.conflict), color = MaterialTheme.colorScheme.error) }
        if (classes.isEmpty() && exams.isEmpty()) item { EmptyCard { add(date.toEpochDay()) } }
        items(classes + exams, key = { it.id }) { RecordView(it, state, actions) }
    }
}

@Composable fun CourseFilter(courses: List<Course>, selected: Long?, change: (Long?) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected == null, { change(null) }, label = { Text(stringResource(R.string.all)) })
        courses.forEach { c -> FilterChip(selected == c.id, { change(c.id) }, label = { Text(c.name) }) }
    }
}
@Composable fun TasksScreen(state: StudyState, add: () -> Unit, actions: RecordActions) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var course by rememberSaveable { mutableStateOf<Long?>(null) }
    val today = LocalDate.now().toEpochDay()
    val list = state.records.filter { it.kind == "TASK" && (course == null || it.courseId == course) && when (filter) { 1 -> !it.done; 2 -> it.done; 3 -> !it.done && it.day < today; else -> true } }
        .sortedWith(compareBy<StudyRecord> { it.done }.thenBy { it.day }.thenByDescending { it.priority })
    Page {
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.all, R.string.active, R.string.completed, R.string.overdue).forEachIndexed { index, label -> FilterChip(filter == index, { filter = index }, label = { Text(stringResource(label)) }) }
        } }
        item { CourseFilter(state.courses, course) { course = it } }
        if (list.isEmpty()) item { EmptyCard(add) }
        items(list, key = { it.id }) { RecordView(it, state, actions) }
    }
}
@Composable fun CoursesScreen(state: StudyState, edit: (Course?) -> Unit, delete: (Course) -> Unit) {
    Page {
        item { SectionTitle(stringResource(R.string.courses), stringResource(R.string.add_course)) { edit(null) } }
        if (state.courses.isEmpty()) item { EmptyCard { edit(null) } }
        items(state.courses, key = { it.id }) { c ->
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.School, null, tint = MaterialTheme.colorScheme.primary)
                    Text(c.name, Modifier.weight(1f).padding(start = 10.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { edit(c) }) { Icon(Icons.Outlined.Edit, stringResource(R.string.edit)) }
                }
                Text("${stringResource(R.string.teacher)}: ${c.teacher.ifBlank { "—" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (c.email.isNotBlank()) Text(c.email)
                if (c.office.isNotBlank()) Text("${stringResource(R.string.office)}: ${c.office}")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${stringResource(R.string.credits)}: ${c.credits}", Modifier.weight(1f))
                    IconButton(onClick = { delete(c) }) { Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.delete)) }
                }
                val linked = state.records.count { it.courseId == c.id }
                Text("${stringResource(R.string.total)}: $linked", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
@Composable fun MoreScreen(navigate: (String) -> Unit) {
    val options = listOf(Triple("calendar", R.string.calendar, Icons.Outlined.Event), Triple("grades", R.string.grades, Icons.AutoMirrored.Outlined.TrendingUp),
        Triple("attendance", R.string.attendance, Icons.Outlined.CheckCircle), Triple("notes", R.string.notes, Icons.AutoMirrored.Outlined.StickyNote2),
        Triple("teachers", R.string.teachers, Icons.Outlined.People), Triple("search", R.string.search, Icons.Outlined.Search), Triple("settings", R.string.settings, Icons.Outlined.Settings))
    Page {
        item { Text(stringResource(R.string.quick_access), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        items(options) { (route, label, icon) ->
            Surface(onClick = { navigate(route) }, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .95f)) {
                Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(label), Modifier.weight(1f).padding(horizontal = 16.dp), fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Outlined.ChevronRight, null)
                }
            }
        }
        item { Text(stringResource(R.string.about), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable fun CalendarScreen(state: StudyState, add: (Kind, Long) -> Unit, actions: RecordActions) {
    var monthOffset by rememberSaveable { mutableLongStateOf(0) }
    var selected by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    val month = YearMonth.now().plusMonths(monthOffset)
    val dates = state.records.filter { it.kind in listOf("TASK", "EXAM") }
    val events = dates.filter { it.day == selected }.sortedBy { it.start }
    val locale = LocalConfiguration.current.locales[0]
    Page {
        item { GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthOffset--; selected = month.minusMonths(1).atDay(1).toEpochDay() }) { Icon(Icons.Outlined.ChevronLeft, stringResource(R.string.month_previous)) }
                Text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)), Modifier.weight(1f), fontWeight = FontWeight.Bold)
                IconButton(onClick = { monthOffset++; selected = month.plusMonths(1).atDay(1).toEpochDay() }) { Icon(Icons.Outlined.ChevronRight, stringResource(R.string.month_next)) }
            }
            Row { (1..7).forEach { Text(DayOfWeek.of(it).getDisplayName(TextStyle.NARROW, locale), Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            val startOffset = month.atDay(1).dayOfWeek.value - 1
            val cells = ((startOffset + month.lengthOfMonth() + 6) / 7) * 7
            for (week in 0 until cells / 7) Row(Modifier.fillMaxWidth()) {
                for (weekday in 0..6) {
                    val day = week * 7 + weekday - startOffset + 1
                    if (day !in 1..month.lengthOfMonth()) Spacer(Modifier.weight(1f).height(48.dp))
                    else {
                        val epoch = month.atDay(day).toEpochDay()
                        Surface(onClick = { selected = epoch }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = if (selected == epoch) MaterialTheme.colorScheme.primaryContainer else Color.Transparent) {
                            Column(Modifier.heightIn(min = 48.dp).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(day.toString(), fontWeight = if (epoch == LocalDate.now().toEpochDay()) FontWeight.Bold else FontWeight.Normal)
                                Text(if (dates.any { it.day == epoch }) "•" else " ", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        } }
        item { SectionTitle("${stringResource(R.string.events)} · ${dateLabel(selected)}") }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { add(Kind.EXAM, selected) }, Modifier.weight(1f)) { Text(stringResource(R.string.add_exam)) }
            OutlinedButton(onClick = { add(Kind.TASK, selected) }, Modifier.weight(1f)) { Text(stringResource(R.string.add_task)) }
        } }
        if (events.isEmpty()) item { EmptyCard { add(Kind.EXAM, selected) } }
        items(events, key = { it.id }) { RecordView(it, state, actions) }
        item { SectionTitle(stringResource(R.string.upcoming)) }
        items(dates.filter { it.kind == "EXAM" && !it.done && it.day >= LocalDate.now().toEpochDay() && it.day != selected }.sortedBy { it.day }, key = { "upcoming${it.id}" }) { RecordView(it, state, actions) }
    }
}

@Composable fun GradesScreen(state: StudyState, add: () -> Unit, actions: RecordActions) {
    var course by rememberSaveable { mutableStateOf<Long?>(null) }
    val grades = state.records.filter { it.kind == "GRADE" && (course == null || it.courseId == course) }
    Page {
        item { GlassCard {
            Metric(Analytics.gpa(state.courses, state.records)?.let { "%.2f / 4.0".format(it) } ?: "—", stringResource(R.string.gpa))
            Text(stringResource(R.string.gpa_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        items(state.courses, key = { "course${it.id}" }) { c ->
            val average = Analytics.percentage(state.records.filter { it.kind == "GRADE" && it.courseId == c.id })
            GlassCard {
                Text(c.name, fontWeight = FontWeight.Bold)
                Row { Text(stringResource(R.string.average), Modifier.weight(1f)); Text(average?.let { "%.1f%% · %.2f".format(it, Analytics.points(it)) } ?: stringResource(R.string.no_grades)) }
                LinearProgressIndicator(progress = { (average?.div(100)?.toFloat() ?: 0f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            }
        }
        item { CourseFilter(state.courses, course) { course = it } }
        if (grades.isEmpty()) item { EmptyCard(add) }
        items(grades.sortedByDescending { it.day }, key = { "grade${it.id}" }) { RecordView(it, state, actions) }
    }
}
@Composable fun AttendanceScreen(state: StudyState, add: () -> Unit, actions: RecordActions) {
    var course by rememberSaveable { mutableStateOf<Long?>(null) }
    val records = state.records.filter { it.kind == "ATTENDANCE" && (course == null || it.courseId == course) }
    Page {
        item { CourseFilter(state.courses, course) { course = it } }
        item { GlassCard {
            Metric(Analytics.attendance(records)?.let { "%.0f%%".format(it) } ?: "—", stringResource(R.string.attended))
            Row {
                Metric(records.count { it.present }.toString(), stringResource(R.string.present), Modifier.weight(1f))
                Metric(records.count { !it.present }.toString(), stringResource(R.string.absent), Modifier.weight(1f))
            }
        } }
        if (records.isEmpty()) item { EmptyCard(add) }
        items(records.sortedByDescending { it.day }, key = { it.id }) { RecordView(it, state, actions) }
    }
}
@Composable fun NotesScreen(state: StudyState, add: () -> Unit, actions: RecordActions) {
    var query by rememberSaveable { mutableStateOf("") }
    val records = state.records.filter { it.kind == "NOTE" && (it.title.contains(query, true) || it.body.contains(query, true)) }.sortedWith(compareByDescending<StudyRecord> { it.pinned }.thenByDescending { it.day })
    Page {
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.search)) }, singleLine = true, leadingIcon = { Icon(Icons.Outlined.Search, null) }) }
        if (records.isEmpty()) item { EmptyCard(add) }
        items(records, key = { it.id }) { RecordView(it, state, actions) }
    }
}
@Composable fun TeachersScreen(state: StudyState, edit: (Course) -> Unit) {
    val courses = state.courses.filter { it.teacher.isNotBlank() }
    Page {
        item { Text(stringResource(R.string.teacher_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (courses.isEmpty()) item { EmptyCard() }
        items(courses, key = { it.id }) { c -> GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.primary)
                Text(c.teacher, Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = { edit(c) }) { Icon(Icons.Outlined.Edit, stringResource(R.string.edit)) }
            }
            Text(c.name, color = MaterialTheme.colorScheme.primary)
            if (c.email.isNotBlank()) Text(c.email)
            if (c.office.isNotBlank()) Text("${stringResource(R.string.office)}: ${c.office}")
        } }
    }
}
@Composable fun SearchScreen(state: StudyState, actions: RecordActions, editCourse: (Course) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim()
    val courses = if (q.isEmpty()) emptyList() else state.courses.filter { listOf(it.name, it.teacher, it.email, it.office).any { text -> text.contains(q, true) } }
    val records = if (q.isEmpty()) emptyList() else state.records.filter { r -> listOf(r.title, r.body, r.place, state.courses.find { it.id == r.courseId }?.name ?: "").any { it.contains(q, true) } }
    Page {
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.search_hint)) }, singleLine = true, leadingIcon = { Icon(Icons.Outlined.Search, null) }) }
        if (q.isEmpty()) item { Text(stringResource(R.string.search_start)) }
        else if (courses.isEmpty() && records.isEmpty()) item { Text(stringResource(R.string.search_empty)) }
        items(courses, key = { "c${it.id}" }) { c -> GlassCard {
            Text(c.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(c.teacher)
            TextButton(onClick = { editCourse(c) }) { Text(stringResource(R.string.edit)) }
        } }
        items(records, key = { "r${it.id}" }) { r ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(kindLabel(Kind.valueOf(r.kind)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                RecordView(r, state, actions)
            }
        }
    }
}
