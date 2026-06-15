package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ImmunisationRepository(private val dao: ImmunisationDao) {

    val allChildProfiles: Flow<List<ChildProfile>> = dao.getAllChildProfiles()
    val allVaccinationRecords: Flow<List<VaccinationRecord>> = dao.getAllVaccinationRecords()
    val allAppointments: Flow<List<Appointment>> = dao.getAllAppointments()
    val allMessages: Flow<List<ClinicMessage>> = dao.getAllMessages()
    val allClinics: Flow<List<Clinic>> = dao.getAllClinics()
    val allDoctorsNurses: Flow<List<DoctorNurse>> = dao.getAllDoctorsNurses()
    val allUserFeedbacks: Flow<List<UserFeedback>> = dao.getAllUserFeedbacks()

    fun getChildProfileById(id: Int): Flow<ChildProfile?> = dao.getChildProfileById(id)
    
    fun getVaccinationRecordsByChild(childId: Int): Flow<List<VaccinationRecord>> = 
        dao.getVaccinationRecordsByChild(childId)

    fun getAppointmentsByChild(childId: Int): Flow<List<Appointment>> = 
        dao.getAppointmentsByChild(childId)

    fun getAppointmentsByClinic(clinicId: Int): Flow<List<Appointment>> = 
        dao.getAppointmentsByClinic(clinicId)

    // --- Write methods ---
    suspend fun insertChildProfile(profile: ChildProfile): Int {
        return dao.insertChildProfile(profile).toInt()
    }

    suspend fun updateChildProfile(profile: ChildProfile) {
        dao.updateChildProfile(profile)
    }

    suspend fun deleteChildProfile(profile: ChildProfile) {
        dao.deleteChildProfile(profile)
    }

    suspend fun insertDoctorNurse(person: DoctorNurse): Int {
        return dao.insertDoctorNurse(person).toInt()
    }

    suspend fun deleteDoctorNurse(person: DoctorNurse) {
        dao.deleteDoctorNurse(person)
    }

    suspend fun insertVaccinationRecord(record: VaccinationRecord) {
        dao.insertVaccinationRecord(record)
    }

    suspend fun updateVaccinationRecord(record: VaccinationRecord) {
        dao.updateVaccinationRecord(record)
    }

    suspend fun insertAppointment(appointment: Appointment) {
        dao.insertAppointment(appointment)
    }

    suspend fun updateAppointment(appointment: Appointment) {
        dao.updateAppointment(appointment)
    }

    suspend fun deleteAppointment(id: Int) {
        dao.deleteAppointmentById(id)
    }

    suspend fun insertMessage(message: ClinicMessage) {
        dao.insertMessage(message)
    }

    suspend fun updateClinic(clinic: Clinic) {
        dao.updateClinic(clinic)
    }

    suspend fun insertUserFeedback(feedback: UserFeedback): Int {
        return dao.insertUserFeedback(feedback).toInt()
    }

    suspend fun updateUserFeedback(feedback: UserFeedback) {
        dao.updateUserFeedback(feedback)
    }

    suspend fun deleteUserFeedback(feedback: UserFeedback) {
        dao.deleteUserFeedback(feedback)
    }

    // --- Offline Synchronisation ---
    suspend fun syncOfflineRecords(): Int {
        // Find records marked isSynced = false
        val allRecords = dao.getAllVaccinationRecords().first()
        val allApps = dao.getAllAppointments().first()
        
        var syncCount = 0
        
        for (record in allRecords) {
            if (!record.isSynced) {
                dao.updateVaccinationRecord(record.copy(isSynced = true))
                syncCount++
            }
        }
        
        for (app in allApps) {
            if (!app.isSynced) {
                dao.updateAppointment(app.copy(isSynced = true))
                syncCount++
            }
        }
        
        return syncCount
    }

    // --- EHR Interoperability (Simulating FHIR / HL7 Data Import/Export) ---
    suspend fun importFromEhrFhirJson(jsonString: String): Boolean {
        // Implements parsing of a standardized FHIR Child Profile + Immunisation resource
        // Under the Ministry of Health interoperability layer
        if (!jsonString.contains("resourceType") || !jsonString.contains("Patient")) {
            return false
        }
        
        // Simulating the ingestion of a paciente model
        val name = when {
            jsonString.contains("\"family\":") -> {
                val family = jsonString.substringAfter("\"family\": \"").substringBefore("\"")
                val given = jsonString.substringAfter("\"given\": [ \"").substringBefore("\"")
                "$given $family"
            }
            else -> "Simulated EHR Patient"
        }
        val birth = if (jsonString.contains("\"birthDate\":")) {
            jsonString.substringAfter("\"birthDate\": \"").substringBefore("\"")
        } else {
            "2025-08-14"
        }
        val pin = if (jsonString.contains("\"identifier\":")) {
            jsonString.substringAfter("\"value\": \"").substringBefore("\"")
        } else {
            "ZW-EHR-${(1000..9999).random()}"
        }

        val childId = insertChildProfile(
            ChildProfile(
                name = name,
                birthDate = birth,
                gender = "Boy",
                motherName = "EHR Automatic Import",
                contactPhone = "+26377111222",
                healthPin = pin,
                district = "Harare Central",
                clinicId = 1,
                complianceStatus = "Due"
            )
        )
        
        // Generate pre-populated vaccination schedule for imported baby
        createZEPISchedule(childId, birth)
        return true
    }

    fun exportToEhrFhirJson(child: ChildProfile, records: List<VaccinationRecord>): String {
        return """
        {
          "resourceType": "Bundle",
          "id": "mohc-zw-bundle-${child.healthPin}",
          "type": "transaction",
          "entry": [
            {
              "resource": {
                "resourceType": "Patient",
                "id": "pat-${child.id}",
                "identifier": [{ "system": "http://mohcc.gov.zw/identifiers", "value": "${child.healthPin}" }],
                "name": [{ "use": "official", "text": "${child.name}" }],
                "gender": "${if (child.gender == "Boy") "male" else "female"}",
                "birthDate": "${child.birthDate}",
                "contact": [{ "relationship": "mother", "name": "${child.motherName}", "telecom": [{ "system": "phone", "value": "${child.contactPhone}" }] }]
              },
              "request": { "method": "POST", "url": "Patient" }
            },
            ${records.joinToString(",") { rec ->
                """
                {
                  "resource": {
                    "resourceType": "Immunization",
                    "status": "${if (rec.administeredDate != null) "completed" else "not-done"}",
                    "vaccineCode": { "text": "${rec.vaccineName} Dose ${rec.doseNumber}" },
                    "patient": { "reference": "Patient/pat-${child.id}" },
                    "occurrenceDateTime": "${rec.administeredDate ?: rec.scheduledDate}",
                    "lotNumber": "${rec.batchNumber ?: "N/A"}",
                    "location": { "display": "${rec.facilityName ?: "N/A"}" }
                  }
                }
                """.trimIndent()
            }}
          ]
        }
        """.trimIndent()
    }

    // --- Prepopulation logic ---
    suspend fun verifyAndPrepopulate() {
        val existingClinics = dao.getAllClinics().first()
        if (existingClinics.isEmpty()) {
            val list = listOf(
                Clinic(name = "Parirenyatwa General Hospital Clinic", district = "Harare", totalChildren = 780, efficiencyRate = 96.2, hasIntermittentInternet = false),
                Clinic(name = "Mpilo Central Hospital Clinic", district = "Bulawayo", totalChildren = 640, efficiencyRate = 94.8, hasIntermittentInternet = true),
                Clinic(name = "Chitungwiza Central Clinic", district = "Chitungwiza", totalChildren = 520, efficiencyRate = 92.1, hasIntermittentInternet = false),
                Clinic(name = "Gweru Provincial General Clinic", district = "Gweru", totalChildren = 410, efficiencyRate = 89.4, hasIntermittentInternet = true),
                Clinic(name = "Mutare General Immunisation Wing", district = "Mutare", totalChildren = 490, efficiencyRate = 91.5, hasIntermittentInternet = true)
            )
            dao.insertClinics(list)
        }

        val existingDoctorsNurses = dao.getAllDoctorsNurses().first()
        if (existingDoctorsNurses.isEmpty()) {
            val list = listOf(
                DoctorNurse(name = "Dr. Sibusiso Sibanda", role = "Doctor", contactNo = "+263 77 111 2222", clinicId = 2, designatorCode = "MOH-DOC-4282"),
                DoctorNurse(name = "Nurse Farai Moyo", role = "Nurse", contactNo = "+263 78 222 3333", clinicId = 1, designatorCode = "MOH-NRS-9801"),
                DoctorNurse(name = "Nurse Chipo Nkomo", role = "Nurse", contactNo = "+263 77 333 4444", clinicId = 2, designatorCode = "MOH-NRS-5432"),
                DoctorNurse(name = "Dr. Sustus Sibanda", role = "Doctor", contactNo = "+263 71 444 5555", clinicId = 1, designatorCode = "MOH-DOC-1102")
            )
            dao.insertDoctorsNurses(list)
        }

        val existingChildren = dao.getAllChildProfiles().first()
        if (existingChildren.isEmpty()) {
            // First mock patient: Tinashe Moyo, Compliant (Birthdate 2025-05-15)
            val tinasheId = insertChildProfile(
                ChildProfile(
                    name = "Tinashe Moyo",
                    birthDate = "2025-05-15",
                    gender = "Boy",
                    motherName = "Ruvimbo Moyo",
                    contactPhone = "+263 77 123 4567",
                    healthPin = "ZW-MOH-2025-0421",
                    district = "Harare",
                    clinicId = 1,
                    complianceStatus = "Compliant"
                )
            ).toInt()
            createCompletedHistoryForTinashe(tinasheId)

            // Second mock patient: Ruvarashe Chigumba, Due/Overdue (Birthdate 2026-01-20)
            val ruvarasheId = insertChildProfile(
                ChildProfile(
                    name = "Ruvarashe Chigumba",
                    birthDate = "2026-01-20",
                    gender = "Girl",
                    motherName = "Chipo Chigumba",
                    contactPhone = "+263 78 456 7890",
                    healthPin = "ZW-MOH-2026-0814",
                    district = "Bulawayo",
                    clinicId = 2,
                    complianceStatus = "Due"
                )
            ).toInt()
            createZEPISchedule(ruvarasheId, "2026-01-20")
            
            // Mark PCV 1 & Pentavalent 1 as administered, others pending
            val ruvarasheRecords = dao.getVaccinationRecordsByChild(ruvarasheId).first()
            for (rec in ruvarasheRecords) {
                if (rec.vaccineName == "BCG" || (rec.vaccineName == "OPV" && rec.doseNumber == 0)) {
                    dao.updateVaccinationRecord(
                        rec.copy(
                            administeredDate = "2026-01-20",
                            administeredBy = "Nurse Sibanda, Mpilo",
                            batchNumber = EncryptionHelper.encrypt("BCG-2026"),
                            facilityName = "Mpilo Central Hospital Clinic",
                            isSynced = true
                        )
                    )
                }
            }

            // Create some sample appointments
            dao.insertAppointment(
                Appointment(
                    childId = ruvarasheId,
                    clinicId = 2,
                    vaccineNames = "OPV 1, Pentavalent 1, PCV 1, Rotavirus 1",
                    appointmentDate = "2026-06-25",
                    appointmentTime = "09:30 AM",
                    status = "Pending Approval",
                    isSynced = true
                )
            )

            dao.insertAppointment(
                Appointment(
                    childId = tinasheId,
                    clinicId = 1,
                    vaccineNames = "Measles-Rubella Booster",
                    appointmentDate = "2026-07-10",
                    appointmentTime = "11:00 AM",
                    status = "Approved",
                    isSynced = true
                )
            )

            // Create some secure messages demonstrating HIPAA follow-up and MoH queries
            dao.insertMessage(
                ClinicMessage(
                    childId = ruvarasheId,
                    senderName = "Chipo Chigumba",
                    senderRole = "Parent",
                    content = EncryptionHelper.encrypt("Greetings Dr, I wanted to confirm if the clinic will have Rotavirus doses available this Thursday? Please advise, thank you."),
                    timestamp = System.currentTimeMillis() - 86400000,
                    recipientRole = "Doctor"
                )
            )

            dao.insertMessage(
                ClinicMessage(
                    childId = ruvarasheId,
                    senderName = "Dr. Sibusiso Sibanda",
                    senderRole = "Doctor",
                    content = EncryptionHelper.encrypt("Hello Ms Chigumba. Yes, we just received a new shipment of Rotavirus vaccines under the MoH central supply. Feel free to bring Ruvarashe for her scheduled dose."),
                    timestamp = System.currentTimeMillis() - 72000000,
                    recipientRole = "Parent"
                )
            )
            
            dao.insertMessage(
                ClinicMessage(
                    childId = null,
                    senderName = "Ministerial Health Ops",
                    senderRole = "MoH Admin",
                    content = EncryptionHelper.encrypt("Attention Bulawayo region clinics: Cold chain logistics report completed, vaccine stock levels are verified compliant nationwide."),
                    timestamp = System.currentTimeMillis() - 36000000,
                    recipientRole = "Doctor"
                )
            )

            // Prepopulate some sample user feedbacks with problem ratings
            dao.insertUserFeedback(
                UserFeedback(
                    submitterName = "Chipo Chigumba",
                    submitterRole = "Parent / Guardian",
                    ratingsStars = 3,
                    problemTag = "Wait Times",
                    description = "The clinic vaccine queue on Thursday morning at Mpilo was extremely long. We waited for over 2 hours under the sun, though the nurses were exceptionally polite once we reached.",
                    timestamp = System.currentTimeMillis() - 172800000, // 2 days ago
                    resolutionStatus = "Under Investigation"
                )
            )

            dao.insertUserFeedback(
                UserFeedback(
                    submitterName = "Nurse Chipo Nkomo",
                    submitterRole = "Doctor / Clinic Staff",
                    ratingsStars = 2,
                    problemTag = "Vaccine Stockout",
                    description = "Temporary stock depletion of PCV Dose 2 experienced at Bulawayo wing. Urgent requisition dispatched to the MoH Central Cold Chain.",
                    timestamp = System.currentTimeMillis() - 86400000, // 1 day ago
                    resolutionStatus = "Pending Review"
                )
            )

            dao.insertUserFeedback(
                UserFeedback(
                    submitterName = "Ruvimbo Moyo",
                    submitterRole = "Parent / Guardian",
                    ratingsStars = 4,
                    problemTag = "App Bugs",
                    description = "The digital ZEPI schedule is an absolute lifesaver. I had a small visual glitch with alignment under offline mode, but after syncing, everything sorted itself out.",
                    timestamp = System.currentTimeMillis() - 43200000, // 12 hours ago
                    resolutionStatus = "Resolved"
                )
            )
        }
    }

    private suspend fun createCompletedHistoryForTinashe(childId: Int) {
        val schedule = generateScheduleList("2025-05-15")
        val records = schedule.mapIndexed { index, (vaccine, dose, dateStr) ->
            VaccinationRecord(
                childId = childId,
                vaccineName = vaccine,
                doseNumber = dose,
                scheduledDate = dateStr,
                administeredDate = if (index < 12) dateStr else null, // administered up to 9 months
                administeredBy = if (index < 12) "Nurse Mnangagwa, Parirenyatwa" else null,
                batchNumber = if (index < 12) EncryptionHelper.encrypt("LVX-${1000 + index}") else null,
                facilityName = if (index < 12) "Parirenyatwa General Hospital Clinic" else null,
                notes = if (index < 12) EncryptionHelper.encrypt("Dose well navigated, slight temperature resolved in 24 hours.") else null,
                isSynced = true
            )
        }
        dao.insertVaccinationRecords(records)
    }

    suspend fun createZEPISchedule(childId: Int, birthDate: String) {
        val schedule = generateScheduleList(birthDate)
        val records = schedule.map { (vaccine, dose, dateStr) ->
            VaccinationRecord(
                childId = childId,
                vaccineName = vaccine,
                doseNumber = dose,
                scheduledDate = dateStr,
                administeredDate = null,
                administeredBy = null,
                batchNumber = null,
                facilityName = null,
                notes = null,
                isSynced = true
            )
        }
        dao.insertVaccinationRecords(records)
    }

    /**
     * Helper to generate accurate ZEPI vaccine schedule based on birthdate.
     */
    private fun generateScheduleList(birthDate: String): List<Triple<String, Int, String>> {
        val sdf = SimpleDateFormat("YYYY-MM-DD", Locale.US)
        val calendar = Calendar.getInstance()
        
        val date = try {
            val parts = birthDate.split("-")
            calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            calendar.time
        } catch (e: Exception) {
            Date()
        }

        fun addDaysToBirth(days: Int): String {
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.DAY_OF_YEAR, days)
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)
            return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
        }

        return listOf(
            // At Birth
            Triple("BCG", 1, addDaysToBirth(0)),
            Triple("OPV", 0, addDaysToBirth(0)),
            
            // 6 Weeks
            Triple("OPV", 1, addDaysToBirth(42)),
            Triple("Rotavirus", 1, addDaysToBirth(42)),
            Triple("Pentavalent", 1, addDaysToBirth(42)), // DPT-HepB-Hib
            Triple("PCV", 1, addDaysToBirth(42)),       // Pneumococcal Conjugate
            
            // 10 Weeks
            Triple("OPV", 2, addDaysToBirth(70)),
            Triple("Rotavirus", 2, addDaysToBirth(70)),
            Triple("Pentavalent", 2, addDaysToBirth(70)),
            Triple("PCV", 2, addDaysToBirth(70)),
            
            // 14 Weeks
            Triple("OPV", 3, addDaysToBirth(98)),
            Triple("IPV", 1, addDaysToBirth(98)), // Inactivated Polio
            Triple("Pentavalent", 3, addDaysToBirth(98)),
            Triple("PCV", 3, addDaysToBirth(98)),
            
            // 9 Months
            Triple("Measles-Rubella", 1, addDaysToBirth(273)),
            Triple("TCV", 1, addDaysToBirth(273)), // Typhoid Conjugate
            
            // 18 Months
            Triple("Measles-Rubella", 2, addDaysToBirth(547))
        )
    }
}
