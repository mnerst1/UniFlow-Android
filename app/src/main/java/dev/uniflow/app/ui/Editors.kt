package dev.uniflow.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalConfiguration
import dev.uniflow.app.R
import dev.uniflow.app.data.*
import dev.uniflow.app.domain.*
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.TextStyle

@Composable fun EditorShell(title: String, dismiss: () -> Unit, save: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.safeDrawingPadding().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = dismiss) { Icon(Icons.Outlined.Close, stringResource(R.string.close)) }
                    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Button(onClick = save) { Text(stringResource(R.string.save)) }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp).widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
            }
        }
    }
}

@Composable fun FormText(label: Int, value: String, change: (String) -> Unit, number: Boolean = false, multiline: Boolean = false) {
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(stringResource(label)) },
        singleLine = !multiline, minLines = if (multiline) 4 else 1,
        keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text))
}

@Composable fun <T> SelectField(label: String, options: List<Pair<T, String>>, selected: T, select: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(options.find { it.first == selected }?.second ?: "—") }
            DropdownMenu(expanded, { expanded = false }) { options.forEach { (value, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { select(value); expanded = false }) } }
        }
    }
}

@Composable fun CourseEditor(existing: Course?, save: (Course) -> Unit, dismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var teacher by rememberSaveable { mutableStateOf(existing?.teacher ?: "") }
    var email by rememberSaveable { mutableStateOf(existing?.email ?: "") }
    var office by rememberSaveable { mutableStateOf(existing?.office ?: "") }
    var credits by rememberSaveable { mutableStateOf((existing?.credits ?: 3).toString()) }
    var color by rememberSaveable { mutableIntStateOf(existing?.color ?: 0) }
    var error by rememberSaveable { mutableIntStateOf(0) }
    EditorShell(stringResource(if (existing == null) R.string.add_course else R.string.edit), dismiss, {
        error = when { name.isBlank() -> R.string.error_title; credits.toIntOrNull() !in 1..30 -> R.string.error_credits; else -> 0 }
        if (error == 0) { save(Course(existing?.id ?: 0, name.trim(), teacher.trim(), email.trim(), office.trim(), credits.toInt(), color)); dismiss() }
    }) {
        if (error != 0) Text(stringResource(error), color = MaterialTheme.colorScheme.error)
        FormText(R.string.course_name, name, { name = it })
        FormText(R.string.teacher, teacher, { teacher = it })
        FormText(R.string.email, email, { email = it })
        FormText(R.string.office, office, { office = it })
        FormText(R.string.credits, credits, { credits = it }, number = true)
        Text(stringResource(R.string.color))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onSurfaceVariant).forEachIndexed { index, ink ->
                FilledTonalButton(onClick = { color = index }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = ink.copy(alpha = .16f), contentColor = ink)) { Text(if (color == index) "●" else "○") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun RecordEditor(kind: Kind, existing: StudyRecord?, courses: List<Course>, initialDay: Long, save: (StudyRecord) -> Unit, dismiss: () -> Unit) {
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var body by rememberSaveable { mutableStateOf(existing?.body ?: "") }
    var courseId by rememberSaveable { mutableStateOf(if (existing != null) existing.courseId else if (kind == Kind.NOTE) null else courses.firstOrNull()?.id) }
    var date by rememberSaveable { mutableStateOf(LocalDate.ofEpochDay(existing?.day ?: initialDay).toString()) }
    var weekday by rememberSaveable { mutableIntStateOf(existing?.weekday ?: LocalDate.ofEpochDay(initialDay).dayOfWeek.value) }
    var start by rememberSaveable { mutableStateOf(timeLabel(existing?.start ?: 540)) }
    var end by rememberSaveable { mutableStateOf(timeLabel(existing?.end ?: 630)) }
    var place by rememberSaveable { mutableStateOf(existing?.place ?: "") }
    var priority by rememberSaveable { mutableIntStateOf(existing?.priority ?: 1) }
    var score by rememberSaveable { mutableStateOf((existing?.score ?: 0.0).toString()) }
    var maximum by rememberSaveable { mutableStateOf((existing?.maximum ?: 100.0).toString()) }
    var weight by rememberSaveable { mutableStateOf((existing?.weight ?: 1.0).toString()) }
    var present by rememberSaveable { mutableStateOf(existing?.present ?: true) }
    var pinned by rememberSaveable { mutableStateOf(existing?.pinned ?: false) }
    var error by rememberSaveable { mutableIntStateOf(0) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    fun minutes(text: String): Int? {
        if (!Regex("\\d{2}:\\d{2}").matches(text)) return null
        val parts = text.split(':').map { it.toInt() }
        return if (parts[0] in 0..23 && parts[1] in 0..59) parts[0] * 60 + parts[1] else null
    }
    fun number(text: String) = text.replace(',', '.').toDoubleOrNull() ?: Double.NaN
    EditorShell("${stringResource(if (existing == null) R.string.add else R.string.edit)} · ${kindLabel(kind)}", dismiss, {
        val day = runCatching { LocalDate.parse(date).toEpochDay() }.getOrNull()
        val s = minutes(start); val e = minutes(end)
        val r = StudyRecord(id = existing?.id ?: 0, kind = kind.name, title = title.trim(), body = body.trim(), courseId = courseId,
            day = day ?: initialDay, weekday = weekday, start = s ?: -1, end = e ?: -1, place = place.trim(), priority = priority,
            done = existing?.done ?: false, score = number(score), maximum = number(maximum), weight = number(weight), present = present, pinned = pinned)
        error = when {
            kind != Kind.CLASS && day == null -> R.string.error_date
            kind in listOf(Kind.CLASS, Kind.EXAM) && (s == null || e == null || e <= s) -> R.string.error_time
            else -> when (validate(r)) {
                Validation.TITLE -> R.string.error_title; Validation.COURSE -> R.string.error_course
                Validation.TIME -> R.string.error_time; Validation.NUMBER -> R.string.error_number
                Validation.DATE -> R.string.error_date; null -> 0
            }
        }
        if (error == 0) { save(r); dismiss() }
    }) {
        if (error != 0) Text(stringResource(error), color = MaterialTheme.colorScheme.error)
        FormText(R.string.title, title, { title = it })
        SelectField(stringResource(R.string.course), buildList<Pair<Long?, String>> {
            if (kind == Kind.NOTE) add(null to stringResource(R.string.no_course))
            addAll(courses.map { it.id to it.name })
        }, courseId, { courseId = it })
        if (kind == Kind.CLASS) SelectField(stringResource(R.string.weekday), (1..7).map { it to DayOfWeek.of(it).getDisplayName(TextStyle.FULL, locale) }, weekday, { weekday = it })
        else {
            OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.date)) }, singleLine = true,
                trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Outlined.CalendarMonth, stringResource(R.string.choose_date)) } })
            Text(stringResource(R.string.date_hint), style = MaterialTheme.typography.bodySmall)
        }
        if (kind in listOf(Kind.CLASS, Kind.EXAM)) {
            FormText(R.string.start, start, { start = it }); FormText(R.string.end, end, { end = it }); FormText(R.string.place, place, { place = it })
        }
        if (kind == Kind.TASK) SelectField(stringResource(R.string.priority), listOf(0 to stringResource(R.string.low), 1 to stringResource(R.string.normal), 2 to stringResource(R.string.high)), priority, { priority = it })
        if (kind == Kind.GRADE) {
            FormText(R.string.score, score, { score = it }, number = true)
            FormText(R.string.maximum, maximum, { maximum = it }, number = true)
            FormText(R.string.weight, weight, { weight = it }, number = true)
        }
        if (kind == Kind.ATTENDANCE) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.present), Modifier.weight(1f)); Switch(present, { present = it })
        }
        if (kind == Kind.NOTE) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.pin), Modifier.weight(1f)); Switch(pinned, { pinned = it })
        }
        FormText(R.string.body, body, { body = it }, multiline = true)
    }
    if (showDatePicker) {
        val parsed = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.ofEpochDay(initialDay))
        val initial = parsed.withYear(parsed.year.coerceIn(1900, 2100))
        val picker = rememberDatePickerState(initialSelectedDateMillis = initial.toEpochDay() * 86400000L)
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { picker.selectedDateMillis?.let { date = LocalDate.ofEpochDay(Math.floorDiv(it, 86400000L)).toString() }; showDatePicker = false }) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } }) { DatePicker(picker) }
    }
}
