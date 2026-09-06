package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.AppSettings
import com.example.data.model.ClassSchedule
import com.example.data.model.ClassWithDays
import com.example.data.model.Exam
import com.example.data.model.Homework
import com.example.data.model.ReminderItem
import com.example.data.model.ScheduleDay
import com.example.data.model.StudentProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassScheduleDao {
    @Transaction
    @Query("SELECT * FROM class_schedules ORDER BY id DESC")
    fun getAllClassesWithDays(): Flow<List<ClassWithDays>>

    @Transaction
    @Query("SELECT * FROM class_schedules ORDER BY id DESC")
    suspend fun getAllClassesWithDaysDirect(): List<ClassWithDays>

    @Transaction
    @Query("SELECT * FROM class_schedules WHERE id = :id")
    suspend fun getClassById(id: Long): ClassWithDays?

    @Query("SELECT COUNT(*) FROM class_schedules WHERE isEnabled = 1")
    fun getEnabledClassesCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM class_schedules WHERE subject LIKE '%' || :query || '%' OR teacher LIKE '%' || :query || '%' OR room LIKE '%' || :query || '%'")
    fun searchClasses(query: String): Flow<List<ClassWithDays>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classSchedule: ClassSchedule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleDays(days: List<ScheduleDay>)

    @Query("DELETE FROM schedule_days WHERE classScheduleId = :classId")
    suspend fun deleteDaysForClass(classId: Long)

    @Update
    suspend fun updateClass(classSchedule: ClassSchedule)

    @Query("UPDATE class_schedules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setClassEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM class_schedules WHERE id = :id")
    suspend fun deleteClassById(id: Long)

    @Transaction
    suspend fun saveClassWithDays(classSchedule: ClassSchedule, days: List<ScheduleDay>): Long {
        val classId = insertClass(classSchedule)
        deleteDaysForClass(classId)
        val daysWithId = days.map { it.copy(classScheduleId = classId) }
        insertScheduleDays(daysWithId)
        return classId
    }

    @Transaction
    suspend fun updateClassWithDays(classSchedule: ClassSchedule, days: List<ScheduleDay>) {
        updateClass(classSchedule)
        deleteDaysForClass(classSchedule.id)
        val daysWithId = days.map { it.copy(classScheduleId = classSchedule.id) }
        insertScheduleDays(daysWithId)
    }
}

@Dao
interface HomeworkDao {
    @Query("SELECT * FROM homework ORDER BY dueDate ASC, dueTime ASC")
    fun getAllHomework(): Flow<List<Homework>>

    @Query("SELECT * FROM homework ORDER BY dueDate ASC, dueTime ASC")
    suspend fun getAllHomeworkDirect(): List<Homework>

    @Query("SELECT * FROM homework WHERE status != 'COMPLETED' ORDER BY dueDate ASC, dueTime ASC")
    fun getPendingHomework(): Flow<List<Homework>>

    @Query("SELECT * FROM homework WHERE status = 'COMPLETED' ORDER BY dueDate DESC, dueTime DESC")
    fun getCompletedHomework(): Flow<List<Homework>>

    @Query("SELECT * FROM homework WHERE id = :id")
    suspend fun getHomeworkById(id: Long): Homework?

    @Query("SELECT COUNT(*) FROM homework")
    fun getHomeworkCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM homework WHERE status != 'COMPLETED'")
    fun getPendingHomeworkCount(): Flow<Int>

    @Query("SELECT * FROM homework WHERE subject = :subject ORDER BY dueDate ASC")
    fun getHomeworkForSubject(subject: String): Flow<List<Homework>>

    @Query("SELECT * FROM homework WHERE title LIKE '%' || :query || '%' OR subject LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY dueDate ASC")
    fun searchHomework(query: String): Flow<List<Homework>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomework(homework: Homework): Long

    @Update
    suspend fun updateHomework(homework: Homework)

    @Query("UPDATE homework SET status = :status WHERE id = :id")
    suspend fun updateHomeworkStatus(id: Long, status: com.example.data.model.TaskStatus)

    @Query("DELETE FROM homework WHERE id = :id")
    suspend fun deleteHomeworkById(id: Long)

    @Query("UPDATE homework SET isEnabled = :enabled WHERE id = :id")
    suspend fun setHomeworkEnabled(id: Long, enabled: Boolean)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY date ASC, time ASC")
    fun getAllExams(): Flow<List<Exam>>

    @Query("SELECT * FROM exams ORDER BY date ASC, time ASC")
    suspend fun getAllExamsDirect(): List<Exam>

    @Query("SELECT * FROM exams WHERE date >= :fromDate ORDER BY date ASC, time ASC")
    fun getUpcomingExams(fromDate: String): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun getExamById(id: Long): Exam?

    @Query("SELECT COUNT(*) FROM exams")
    fun getExamsCount(): Flow<Int>

    @Query("SELECT * FROM exams WHERE examName LIKE '%' || :query || '%' OR subject LIKE '%' || :query || '%' OR room LIKE '%' || :query || '%' ORDER BY date ASC")
    fun searchExams(query: String): Flow<List<Exam>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: Exam): Long

    @Update
    suspend fun updateExam(exam: Exam)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExamById(id: Long)

    @Query("UPDATE exams SET isEnabled = :enabled WHERE id = :id")
    suspend fun setExamEnabled(id: Long, enabled: Boolean)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, time ASC")
    fun getAllReminders(): Flow<List<ReminderItem>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderItem): Long

    @Update
    suspend fun updateReminder(reminder: ReminderItem)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("UPDATE reminders SET isEnabled = :enabled WHERE id = :id")
    suspend fun setReminderEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE reminders SET isCompleted = :completed WHERE id = :id")
    suspend fun setReminderCompleted(id: Long, completed: Boolean)
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: AppSettings)

    @Query("SELECT * FROM student_profile WHERE id = 1")
    fun getProfile(): Flow<StudentProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: StudentProfile)
}
