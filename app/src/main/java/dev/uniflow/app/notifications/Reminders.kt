package dev.uniflow.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.*
import dev.uniflow.app.MainActivity
import dev.uniflow.app.R
import dev.uniflow.app.UniFlowApplication
import dev.uniflow.app.data.StudyRecord
import kotlinx.coroutines.flow.first
import java.time.*
import java.util.Locale
import java.util.concurrent.TimeUnit

object Reminders {
    fun sync(context: Context, records: List<StudyRecord>, enabled: Boolean) {
        val manager = WorkManager.getInstance(context)
        // All future jobs are rebuilt from the current database; edited/deleted records cannot leave stale jobs.
        manager.cancelAllWorkByTag("uniflow-reminders")
        if (!enabled) return
        val today = LocalDate.now().toEpochDay()
        records.filter { it.kind in listOf("TASK", "EXAM") && !it.done && it.day >= today }.forEach { r ->
            val date = LocalDate.ofEpochDay(r.day)
            val due = if (r.kind == "EXAM") date.atStartOfDay().plusMinutes(r.start.toLong()) else date.atTime(23, 59)
            if (due.atZone(ZoneId.systemDefault()).toInstant().isBefore(Instant.now())) return@forEach
            val reminder = if (r.kind == "EXAM") due.minusHours(1) else date.minusDays(1).atTime(18, 0)
            val delay = (reminder.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis()).coerceAtLeast(1000)
            val request = OneTimeWorkRequestBuilder<DeadlineWorker>()
                .setInputData(workDataOf("id" to r.id, "day" to r.day)).setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("uniflow-reminders").build()
            manager.enqueueUniqueWork("deadline-${r.id}", ExistingWorkPolicy.REPLACE, request)
        }
    }
}

class DeadlineWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as UniFlowApplication
        val settings = app.preferences.settings.first()
        if (!settings.reminders) return Result.success()
        val r = app.database.dao().record(inputData.getLong("id", -1)) ?: return Result.success()
        if (r.done || r.day != inputData.getLong("day", -1) || r.day < LocalDate.now().toEpochDay()) return Result.success()
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val sent = app.getSharedPreferences("sent_reminders", Context.MODE_PRIVATE)
        val key = "${r.id}:${r.day}"
        if (sent.getBoolean(key, false)) return Result.success()
        val configuration = Configuration(app.resources.configuration)
        if (settings.language != "system") configuration.setLocale(Locale.forLanguageTag(settings.language))
        val localized = app.createConfigurationContext(configuration)
        val nm = app.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("deadlines", localized.getString(R.string.reminders), NotificationManager.IMPORTANCE_DEFAULT))
        val intent = Intent(app, MainActivity::class.java).putExtra("route", if (r.kind == "EXAM") "calendar" else "tasks")
        val pending = PendingIntent.getActivity(app, r.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        nm.notify(r.id.toInt(), NotificationCompat.Builder(app, "deadlines").setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(r.title).setContentText(localized.getString(R.string.deadline_notification, LocalDate.ofEpochDay(r.day).toString()))
            .setContentIntent(pending).setAutoCancel(true).build())
        sent.edit { putBoolean(key, true) }
        return Result.success()
    }
}
