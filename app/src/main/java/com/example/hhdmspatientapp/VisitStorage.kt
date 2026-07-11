package com.example.hhdmspatientapp

import android.content.Context
import android.content.SharedPreferences

object VisitStorage {
    private const val PREFS_NAME = "visit_prefs"
    private const val KEY_DOCTOR_NAME = "visiting_doctor_name"
    private const val KEY_PATIENT_ID = "visiting_patient_id"
    private const val KEY_APPOINTMENT_DONE = "appointment_done"

    private lateinit var prefs: SharedPreferences

    var onVisitInfoChanged: ((String?) -> Unit)? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveVisitInfo(doctorName: String) {
        prefs.edit().putString(KEY_DOCTOR_NAME, doctorName).apply()
        clearAppointmentDone()
        onVisitInfoChanged?.invoke(doctorName)
    }

    fun getVisitDoctorName(): String? {
        return prefs.getString(KEY_DOCTOR_NAME, null)
    }

    fun clearVisit() {
        prefs.edit().remove(KEY_DOCTOR_NAME).apply()
        onVisitInfoChanged?.invoke(null)
    }

    fun saveVisitingPatientId(patientId: String) {
        prefs.edit().putString(KEY_PATIENT_ID, patientId).apply()
    }

    fun getVisitingPatientId(): String? {
        return prefs.getString(KEY_PATIENT_ID, null)
    }

    fun clearVisitingPatientId() {
        prefs.edit().remove(KEY_PATIENT_ID).apply()
    }

    fun saveAppointmentDone() {
        prefs.edit().putBoolean(KEY_APPOINTMENT_DONE, true).apply()
    }

    fun isAppointmentDone(): Boolean {
        return prefs.getBoolean(KEY_APPOINTMENT_DONE, false)
    }

    fun clearAppointmentDone() {
        prefs.edit().remove(KEY_APPOINTMENT_DONE).apply()
    }
}
