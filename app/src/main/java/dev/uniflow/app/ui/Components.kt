package dev.uniflow.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.uniflow.app.R
import dev.uniflow.app.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalConfiguration

@Composable fun AmbientBackground(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(colors.background)) {
        // Blur the decorative light fields only. Text and interactive surfaces stay sharp.
        Canvas(Modifier.fillMaxSize().blur(72.dp)) {
            drawCircle(colors.primary.copy(alpha = .13f), size.width * .65f, androidx.compose.ui.geometry.Offset(size.width * .95f, size.height * .07f))
            drawCircle(colors.secondary.copy(alpha = .12f), size.width * .6f, androidx.compose.ui.geometry.Offset(0f, size.height * .63f))
        }
        content()
    }
}
@Composable fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp),
        color = colors.surface.copy(alpha = .94f), contentColor = colors.onSurface, tonalElevation = 1.dp,
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(colors.outline.copy(alpha = .22f), colors.primary.copy(alpha = .08f))))) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
data class Tab(val route: String, val label: Int, val icon: ImageVector)
val tabs = listOf(Tab("home", R.string.home, Icons.Outlined.Home), Tab("schedule", R.string.schedule, Icons.Outlined.CalendarMonth),
    Tab("tasks", R.string.tasks, Icons.Outlined.Checklist), Tab("courses", R.string.courses, Icons.Outlined.School), Tab("more", R.string.more, Icons.Outlined.GridView))
@Composable fun GlassTabBar(route: String?, navigate: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(Modifier.padding(horizontal = 14.dp, vertical = 8.dp).navigationBarsPadding().fillMaxWidth(),
        shape = RoundedCornerShape(32.dp), color = colors.surface.copy(alpha = .96f),
        shadowElevation = 12.dp, border = BorderStroke(1.dp, colors.outline.copy(alpha = .25f))) {
        Row(Modifier.padding(7.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            tabs.forEach { tab ->
                val selected = route == tab.route
                val fill by animateColorAsState(if (selected) colors.primary.copy(alpha = .16f) else Color.Transparent, label = "tab")
                val ink by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, label = "ink")
                Column(Modifier.weight(1f).clip(RoundedCornerShape(25.dp)).background(fill).clickable { navigate(tab.route) }.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(tab.icon, stringResource(tab.label), tint = ink, modifier = Modifier.size(24.dp))
                    Text(stringResource(tab.label), color = ink, fontSize = 10.sp, maxLines = 1, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
    }
}
@Composable fun SectionTitle(label: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}
@Composable fun EmptyCard(add: (() -> Unit)? = null) {
    GlassCard {
        Icon(Icons.AutoMirrored.Outlined.MenuBook, null, tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.empty), fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (add != null) FilledTonalButton(onClick = add) { Icon(Icons.Outlined.Add, null); Text(stringResource(R.string.add)) }
    }
}
@Composable fun dateLabel(day: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))
}
fun timeLabel(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)
@Composable fun kindLabel(kind: Kind) = stringResource(when (kind) {
    Kind.CLASS -> R.string.class_label; Kind.TASK -> R.string.task; Kind.EXAM -> R.string.exam
    Kind.GRADE -> R.string.grade; Kind.ATTENDANCE -> R.string.attendance; Kind.NOTE -> R.string.note
})
@Composable fun RecordCard(r: StudyRecord, courses: List<Course>, edit: () -> Unit, delete: () -> Unit, toggle: (() -> Unit)? = null) {
    val course = courses.find { it.id == r.courseId }
    val accent = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onSurfaceVariant)[(course?.color ?: 0).mod(4)]
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp, 38.dp).clip(RoundedCornerShape(3.dp)).background(accent))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(r.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                if (course != null) Text(course.name, style = MaterialTheme.typography.bodySmall, color = accent)
            }
            if (r.pinned) Icon(Icons.Outlined.PushPin, stringResource(R.string.pinned), tint = accent, modifier = Modifier.size(18.dp))
            IconButton(onClick = edit) { Icon(Icons.Outlined.Edit, stringResource(R.string.edit)) }
        }
        when (r.kind) {
            "CLASS" -> Text("${timeLabel(r.start)} – ${timeLabel(r.end)}  ·  ${r.place}", style = MaterialTheme.typography.bodyMedium)
            "TASK", "EXAM" -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Event, null, Modifier.size(18.dp), tint = accent)
                    Text(dateLabel(r.day), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    if (r.kind == "TASK") Text(stringResource(if (r.priority == 2) R.string.high else if (r.priority == 0) R.string.low else R.string.normal), color = if (r.priority == 2) MaterialTheme.colorScheme.error else accent, style = MaterialTheme.typography.labelMedium)
                }
                if (r.kind == "EXAM") Text("${timeLabel(r.start)} – ${timeLabel(r.end)}  ·  ${r.place}")
                if (!r.done && r.day < LocalDate.now().toEpochDay()) Text(stringResource(R.string.overdue), color = MaterialTheme.colorScheme.error)
            }
            "GRADE" -> Text("${"%.1f".format(r.score)} / ${"%.1f".format(r.maximum)}  ·  ${stringResource(R.string.weight)} ${r.weight}", fontWeight = FontWeight.Bold, color = accent)
            "ATTENDANCE" -> Text("${dateLabel(r.day)} · ${stringResource(if (r.present) R.string.present else R.string.absent)}", color = if (r.present) accent else MaterialTheme.colorScheme.error)
            "NOTE" -> Text(dateLabel(r.day), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (r.body.isNotBlank()) Text(r.body, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = if (r.kind == "NOTE") 8 else 3)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (toggle != null) TextButton(onClick = toggle) {
                Icon(if (r.done || (r.kind == "ATTENDANCE" && r.present)) Icons.Outlined.CheckCircle else Icons.Outlined.Check, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(if (r.kind == "ATTENDANCE") R.string.toggle_attendance else if (r.done) R.string.reopen else R.string.complete))
            } else Spacer(Modifier.weight(1f))
            IconButton(onClick = delete) { Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
