package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ImmunisationDao {

    // --- Child Profiles ---
    @Query("SELECT * FROM child_profiles ORDER BY name ASC")
    fun getAllChildProfiles(): Flow<List<ChildProfile>>

    @Query("SELECT * FROM child_profiles WHERE id = :id")
    fun getChildProfileById(id: Int): Flow<ChildProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChildProfile(profile: ChildProfile): Long

    @Update
    suspend fun updateChildProfile(profile: ChildProfile)

    @Delete
    suspend fun deleteChildProfile(profile: ChildProfile)

    // --- Vaccination Records ---
    @Query("SELECT * FROM vaccination_records WHERE childId = :childId ORDER BY scheduledDate ASC")
    fun getVaccinationRecordsByChild(childId: Int): Flow<List<VaccinationRecord>>

    @Query("SELECT * FROM vaccination_records ORDER BY scheduledDate DESC")
    fun getAllVaccinationRecords(): Flow<List<VaccinationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccinationRecord(record: VaccinationRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccinationRecords(records: List<VaccinationRecord>)

    @Update
    suspend fun updateVaccinationRecord(record: VaccinationRecord)

    // --- Appointments ---
    @Query("SELECT * FROM appointments ORDER BY appointmentDate ASC")
    fun getAllAppointments(): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE childId = :childId ORDER BY appointmentDate ASC")
    fun getAppointmentsByChild(childId: Int): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE clinicId = :clinicId ORDER BY appointmentDate ASC")
    fun getAppointmentsByClinic(clinicId: Int): Flow<List<Appointment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: Appointment): Long

    @Update
    suspend fun updateAppointment(appointment: Appointment)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteAppointmentById(id: Int)

    // --- Secures Messaging ---
    @Query("SELECT * FROM clinic_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ClinicMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ClinicMessage): Long

    // --- Clinics ---
    @Query("SELECT * FROM clinics ORDER BY name ASC")
    fun getAllClinics(): Flow<List<Clinic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClinic(clinic: Clinic): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClinics(clinics: List<Clinic>)

    @Update
    suspend fun updateClinic(clinic: Clinic)

    // --- Doctors & Nurses ---
    @Query("SELECT * FROM doctors_nurses ORDER BY name ASC")
    fun getAllDoctorsNurses(): Flow<List<DoctorNurse>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctorNurse(person: DoctorNurse): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctorsNurses(people: List<DoctorNurse>)

    @Delete
    suspend fun deleteDoctorNurse(person: DoctorNurse)

    // --- User Feedback ---
    @Query("SELECT * FROM user_feedbacks ORDER BY timestamp DESC")
    fun getAllUserFeedbacks(): Flow<List<UserFeedback>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserFeedback(feedback: UserFeedback): Long

    @Update
    suspend fun updateUserFeedback(feedback: UserFeedback)

    @Delete
    suspend fun deleteUserFeedback(feedback: UserFeedback)
}
