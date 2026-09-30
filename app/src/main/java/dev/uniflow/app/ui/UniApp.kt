package dev.uniflow.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import dev.uniflow.app.R
import dev.uniflow.app.data.*
import dev.uniflow.app.notifications.Reminders
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun UniApp(state: StudyState, vm: StudyViewModel, initialRoute: String) {
    val now by produceState(LocalDateTime.now()) { while (true) { delay(30000); value = LocalDateTime.now() } }
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: initialRoute
    var editorKind by rememberSaveable { mutableStateOf<String?>(null) }
    var editorId by rememberSaveable { mutableStateOf<Long?>(null) }
    var courseEditor by rememberSaveable { mutableStateOf(false) }
    var courseEditorId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editorDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteCourseId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val storageError = stringResource(R.string.error_storage)
    LaunchedEffect(vm, storageError) { vm.errors.collect { snackbar.showSnackbar(storageError) } }
    val context = LocalContext.current.applicationContext
    val reminderRecords = state.records.filter { it.kind in listOf("TASK", "EXAM") }
    LaunchedEffect(reminderRecords, state.settings.reminders, state.loading, now.toLocalDate()) {
        if (!state.loading) Reminders.sync(context, reminderRecords, state.settings.reminders)
    }
    fun navigate(target: String) {
        if (state.loading) return
        nav.navigate(target) { launchSingleTop = true }
    }
    fun edit(r: StudyRecord) { editorKind = r.kind; editorId = r.id; editorDay = r.day }
    fun add(kind: Kind, day: Long = LocalDate.now().toEpochDay()) { editorKind = kind.name; editorId = null; editorDay = day }
    fun recordDelete(r: StudyRecord) { deleteId = r.id }
    val actions = RecordActions(::edit, ::recordDelete, { r -> vm.save(if (r.kind == "ATTENDANCE") r.copy(present = !r.present) else r.copy(done = !r.done)) })
    val title = when (route) {
        "home" -> R.string.home; "schedule" -> R.string.schedule; "tasks" -> R.string.tasks; "courses" -> R.string.courses
        "calendar" -> R.string.calendar; "grades" -> R.string.grades; "attendance" -> R.string.attendance
        "notes" -> R.string.notes; "teachers" -> R.string.teachers; "search" -> R.string.search; "settings" -> R.string.settings
        else -> R.string.quick_access
    }
    CompositionLocalProvider(LocalNow provides now) { AmbientBackground {
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(title = { Text(if (route == "home") "UniFlow" else stringResource(title)) },
                    navigationIcon = { if (route !in tabs.map { it.route }) IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.close)) } },
                    actions = {
                        if (!state.loading && route != "search") IconButton(onClick = { navigate("search") }) { Icon(Icons.Outlined.Search, stringResource(R.string.search)) }
                        if (!state.loading && route != "settings") IconButton(onClick = { navigate("settings") }) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings)) }
                    }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent))
            },
            bottomBar = { if (!state.loading) GlassTabBar(if (route in tabs.map { it.route }) route else "more") { target ->
                // Tabs always open their overview; a nested screen must not trap the More tab.
                nav.navigate(target) { popUpTo(nav.graph.startDestinationId); launchSingleTop = true }
            } },
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = {
                val kind = when (route) { "schedule" -> Kind.CLASS; "tasks" -> Kind.TASK; "calendar" -> Kind.EXAM; "grades" -> Kind.GRADE; "attendance" -> Kind.ATTENDANCE; "notes" -> Kind.NOTE; else -> null }
                if (!state.loading && (kind != null || route == "courses")) FloatingActionButton(onClick = { if (route == "courses") { courseEditorId = null; courseEditor = true } else kind?.let { add(it) } }) { Icon(Icons.Outlined.Add, stringResource(R.string.add)) }
            }) { padding ->
            if (state.loading) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Spacer(Modifier.height(18.dp)); Text(stringResource(R.string.loading)) }
            } else NavHost(nav, startDestination = initialRoute.takeIf { it in listOf("home", "tasks", "calendar") } ?: "home", modifier = Modifier.padding(padding)) {
                composable("home") { Dashboard(state, ::navigate, { add(Kind.TASK) }, actions) }
                composable("schedule") { ScheduleScreen(state, { day -> add(Kind.CLASS, day) }, actions) }
                composable("tasks") { TasksScreen(state, { add(Kind.TASK) }, actions) }
                composable("courses") { CoursesScreen(state, { c -> courseEditorId = c?.id; courseEditor = true }, { deleteCourseId = it.id }) }
                composable("more") { MoreScreen(::navigate) }
                composable("calendar") { CalendarScreen(state, { kind, day -> add(kind, day) }, actions) }
                composable("grades") { GradesScreen(state, { add(Kind.GRADE) }, actions) }
                composable("attendance") { AttendanceScreen(state, { add(Kind.ATTENDANCE) }, actions) }
                composable("notes") { NotesScreen(state, { add(Kind.NOTE) }, actions) }
                composable("teachers") { TeachersScreen(state) { c -> courseEditorId = c.id; courseEditor = true } }
                composable("search") { SearchScreen(state, actions) { c -> courseEditorId = c.id; courseEditor = true } }
                composable("settings") { SettingsScreen(state.settings, vm::settings, vm::reset, snackbar) }
            }
        }
    } }
    if (courseEditor) CourseEditor(state.courses.find { it.id == courseEditorId }, vm::save, { courseEditor = false })
    editorKind?.let { kind -> RecordEditor(Kind.valueOf(kind), state.records.find { it.id == editorId }, state.courses, editorDay, vm::save) { editorKind = null } }
    if (deleteId != null || deleteCourseId != null) AlertDialog(onDismissRequest = { deleteId = null; deleteCourseId = null },
        title = { Text(stringResource(R.string.delete_title)) }, text = { Text(stringResource(if (deleteCourseId != null) R.string.delete_course_warning else R.string.delete_warning)) },
        confirmButton = { TextButton(onClick = { state.records.find { it.id == deleteId }?.let(vm::delete); state.courses.find { it.id == deleteCourseId }?.let(vm::delete); deleteId = null; deleteCourseId = null }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { deleteId = null; deleteCourseId = null }) { Text(stringResource(R.string.cancel)) } })
}

data class RecordActions(val edit: (StudyRecord) -> Unit, val delete: (StudyRecord) -> Unit, val toggle: (StudyRecord) -> Unit)
