package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.AppSettings
import com.example.data.model.ClassSchedule
import com.example.data.model.ClassWithDays
import com.example.data.model.Exam
import com.example.data.model.Homework
import com.example.data.model.ReminderItem
import com.example.data.model.ScheduleDay
import com.example.data.model.StudentProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class StudyBellRepository(private val database: AppDatabase) {

    val allClasses: Flow<List<ClassWithDays>> = database.classScheduleDao().getAllClassesWithDays()
    val allHomework: Flow<List<Homework>> = database.homeworkDao().getAllHomework()
    val allExams: Flow<List<Exam>> = database.examDao().getAllExams()
    val allReminders: Flow<List<ReminderItem>> = database.reminderDao().getAllReminders()
    val appSettings: Flow<AppSettings?> = database.appSettingsDao().getSettings()
    val studentProfile: Flow<StudentProfile?> = database.appSettingsDao().getProfile()

    suspend fun saveClass(classSchedule: ClassSchedule, days: List<ScheduleDay>): Long =
        withContext(Dispatchers.IO) {
            database.classScheduleDao().saveClassWithDays(classSchedule, days)
        }

    suspend fun updateClass(classSchedule: ClassSchedule, days: List<ScheduleDay>) =
        withContext(Dispatchers.IO) {
            database.classScheduleDao().updateClassWithDays(classSchedule, days)
        }

    suspend fun setClassEnabled(id: Long, enabled: Boolean) =
        withContext(Dispatchers.IO) {
            database.classScheduleDao().setClassEnabled(id, enabled)
        }

    suspend fun deleteClass(id: Long) =
        withContext(Dispatchers.IO) {
            database.classScheduleDao().deleteDaysForClass(id)
            database.classScheduleDao().deleteClassById(id)
        }

    suspend fun saveHomework(homework: Homework): Long =
        withContext(Dispatchers.IO) {
            database.homeworkDao().insertHomework(homework)
        }

    suspend fun updateHomework(homework: Homework) =
        withContext(Dispatchers.IO) {
            database.homeworkDao().updateHomework(homework)
        }

    suspend fun deleteHomework(id: Long) =
        withContext(Dispatchers.IO) {
            database.homeworkDao().deleteHomeworkById(id)
        }

    suspend fun setHomeworkEnabled(id: Long, enabled: Boolean) =
        withContext(Dispatchers.IO) {
            database.homeworkDao().setHomeworkEnabled(id, enabled)
        }

    suspend fun saveExam(exam: Exam): Long =
        withContext(Dispatchers.IO) {
            database.examDao().insertExam(exam)
        }

    suspend fun updateExam(exam: Exam) =
        withContext(Dispatchers.IO) {
            database.examDao().updateExam(exam)
        }

    suspend fun deleteExam(id: Long) =
        withContext(Dispatchers.IO) {
            database.examDao().deleteExamById(id)
        }

    suspend fun setExamEnabled(id: Long, enabled: Boolean) =
        withContext(Dispatchers.IO) {
            database.examDao().setExamEnabled(id, enabled)
        }

    suspend fun saveReminder(reminder: ReminderItem): Long =
        withContext(Dispatchers.IO) {
            database.reminderDao().insertReminder(reminder)
        }

    suspend fun updateReminder(reminder: ReminderItem) =
        withContext(Dispatchers.IO) {
            database.reminderDao().updateReminder(reminder)
        }

    suspend fun deleteReminder(id: Long) =
        withContext(Dispatchers.IO) {
            database.reminderDao().deleteReminderById(id)
        }

    suspend fun setReminderEnabled(id: Long, enabled: Boolean) =
        withContext(Dispatchers.IO) {
            database.reminderDao().setReminderEnabled(id, enabled)
        }

    suspend fun setReminderCompleted(id: Long, completed: Boolean) =
        withContext(Dispatchers.IO) {
            database.reminderDao().setReminderCompleted(id, completed)
        }

    suspend fun updateSettings(settings: AppSettings) =
        withContext(Dispatchers.IO) {
            database.appSettingsDao().insertOrUpdateSettings(settings)
        }

    suspend fun updateProfile(profile: StudentProfile) =
        withContext(Dispatchers.IO) {
            database.appSettingsDao().insertOrUpdateProfile(profile)
        }

    suspend fun clearAllData() =
        withContext(Dispatchers.IO) {
            database.clearAllTables()
        }

    suspend fun resetToSampleData() =
        withContext(Dispatchers.IO) {
            database.clearAllTables()
            AppDatabase.populateInitialData(database)
        }

    suspend fun exportDataJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val classes = database.classScheduleDao().getAllClassesWithDays().firstOrNull() ?: emptyList()
        val homework = database.homeworkDao().getAllHomework().firstOrNull() ?: emptyList()
        val exams = database.examDao().getAllExams().firstOrNull() ?: emptyList()
        val reminders = database.reminderDao().getAllReminders().firstOrNull() ?: emptyList()

        val classesArr = JSONArray()
        for (c in classes) {
            val cObj = JSONObject()
            cObj.put("subject", c.classSchedule.subject)
            cObj.put("teacher", c.classSchedule.teacher)
            cObj.put("room", c.classSchedule.room)
            cObj.put("notes", c.classSchedule.notes)
            cObj.put("startDate", c.classSchedule.startDate)
            cObj.put("endDate", c.classSchedule.endDate)
            cObj.put("notifyClassEnd", c.classSchedule.notifyClassEnd)
            cObj.put("colorHex", c.classSchedule.colorHex)

            val daysArr = JSONArray()
            for (d in c.days) {
                val dObj = JSONObject()
                dObj.put("dayOfWeek", d.dayOfWeek)
                dObj.put("startTime", d.startTime)
                dObj.put("endTime", d.endTime)
                dObj.put("reminderMinutesBefore", d.reminderMinutesBefore)
                daysArr.put(dObj)
            }
            cObj.put("days", daysArr)
            classesArr.put(cObj)
        }
        root.put("classes", classesArr)

        val hwArr = JSONArray()
        for (h in homework) {
            val hObj = JSONObject()
            hObj.put("title", h.title)
            hObj.put("subject", h.subject)
            hObj.put("dueDate", h.dueDate)
            hObj.put("dueTime", h.dueTime)
            hObj.put("description", h.description)
            hObj.put("teacher", h.teacher)
            hObj.put("status", h.status.name)
            hwArr.put(hObj)
        }
        root.put("homework", hwArr)

        val examsArr = JSONArray()
        for (e in exams) {
            val eObj = JSONObject()
            eObj.put("examName", e.examName)
            eObj.put("subject", e.subject)
            eObj.put("date", e.date)
            eObj.put("time", e.time)
            eObj.put("room", e.room)
            eObj.put("teacher", e.teacher)
            examsArr.put(eObj)
        }
        root.put("exams", examsArr)

        val remArr = JSONArray()
        for (r in reminders) {
            val rObj = JSONObject()
            rObj.put("title", r.title)
            rObj.put("category", r.category.name)
            rObj.put("time", r.time)
            rObj.put("repeatType", r.repeatType.name)
            rObj.put("repeatDays", r.repeatDays)
            remArr.put(rObj)
        }
        root.put("reminders", remArr)

        root.toString(2)
    }

    suspend fun importDataJson(jsonString: String) = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonString)
        if (root.has("classes")) {
            val classesArr = root.getJSONArray("classes")
            for (i in 0 until classesArr.length()) {
                val cObj = classesArr.getJSONObject(i)
                val c = ClassSchedule(
                    subject = cObj.optString("subject", "Class"),
                    teacher = cObj.optString("teacher", ""),
                    room = cObj.optString("room", ""),
                    notes = cObj.optString("notes", ""),
                    startDate = cObj.optString("startDate", ""),
                    endDate = cObj.optString("endDate", ""),
                    notifyClassEnd = cObj.optBoolean("notifyClassEnd", true),
                    colorHex = cObj.optString("colorHex", "#6366F1")
                )
                val days = mutableListOf<ScheduleDay>()
                if (cObj.has("days")) {
                    val daysArr = cObj.getJSONArray("days")
                    for (j in 0 until daysArr.length()) {
                        val dObj = daysArr.getJSONObject(j)
                        days.add(
                            ScheduleDay(
                                classScheduleId = 0,
                                dayOfWeek = dObj.optInt("dayOfWeek", 1),
                                startTime = dObj.optString("startTime", "08:00"),
                                endTime = dObj.optString("endTime", "09:00"),
                                reminderMinutesBefore = dObj.optString("reminderMinutesBefore", "15")
                            )
                        )
                    }
                }
                saveClass(c, days)
            }
        }
    }
}
