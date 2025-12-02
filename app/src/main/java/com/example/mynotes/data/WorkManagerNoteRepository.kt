package com.example.mynotes.data

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import worker.NoteReminderWorker
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
class WorkManagerNoteRepository(context: Context) : NotifyRepository {
    private val workManager = WorkManager.getInstance(context)

    override fun scheduleReminder(date: Long, hour: Int, minute: Int, context: String) {
        val now = Calendar.getInstance() // Cập nhật thời gian hiện tại
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        now.set(Calendar.SECOND, 0)

        if (calendar.before(now)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val delayMillis = calendar.timeInMillis - now.timeInMillis
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val nowFormatted = sdf.format(now.time)
        val calendarFormatted = sdf.format(calendar.time)

        Log.d("WorkManager", "Current time (now): $nowFormatted")
        Log.d("WorkManager", "Reminder time (calendar): $calendarFormatted")
        Log.d("WorkManager", "Scheduling reminder: $hour:$minute , Delay: $delayMillis ms")

        val inputData = Data.Builder()
            .putString("CONTEXT", context)
            .build()

        val notifyRequest = OneTimeWorkRequestBuilder<NoteReminderWorker>()
            .setInputData(inputData)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            "noteReminded_${context}_$hour:$minute",
            ExistingWorkPolicy.REPLACE,
            notifyRequest
        )
    }


}
