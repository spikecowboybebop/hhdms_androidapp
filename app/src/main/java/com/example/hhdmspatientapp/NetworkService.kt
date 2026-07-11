package com.example.hhdmspatientapp // Make sure this matches your project's actual package name

import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.Path

// =============================================================================
// 1. DATA MODELS MATCHING YOUR NESTJS API CONTRACTS
// =============================================================================

// Login requests/responses
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val access_token: String,
    val user: UserInfo
)

data class UserInfo(
    val email: String,
    val first_name_en: String,
    val role: String
)

// Mobile registration requests/responses
data class SignupRequest(
    val first_name_en: String,
    val last_name_en: String,
    val phone_number: String,
    val email: String,
    val password: String,
    val role_name: String = "MOBILE_USER"
)

data class SignupResponse(
    val message: String,
    val userId: String? = null,
    val patientId: String? = null,
    val access_token: String? = null,
)

// FCM token registration
data class RegisterTokenRequest(
    val token: String,
    val device_type: String = "android",
)

data class TicketDetail(
    val id: String? = null,
    val additional_meta: Map<String, Any?>? = null,
)

data class ProviderSummary(
    val id: String,
    val first_name_en: String? = null,
    val last_name_en: String? = null,
    val specialization: String? = null,
)

data class Ticket(
    val id: String,
    val ticket_no: String,
    val service_type: String,
    val scheduled_date: String? = null,
    val scheduled_time_slot: String? = null,
    val assigned_provider_id: String? = null,
    val price: String? = null,
    val status: String,
    val details: TicketDetail? = null,
    val provider: ProviderSummary? = null,
)

data class PatientSummary(
    val id: String,
    val first_name_en: String? = null,
    val last_name_en: String? = null,
    val phone_number: String? = null,
)

data class BookingSessionResponse(
    val id: String,
    val patient_id: String,
    val booked_by: String? = null,
    val agent_id: String? = null,
    val total_amount: String? = null,
    val status: String,
    val created_at: String? = null,
    val updated_at: String? = null,
    val patient: PatientSummary? = null,
    val tickets: List<Ticket> = emptyList(),
)

data class TicketSummary(
    val id: String,
    val ticket_no: String,
    val service_type: String,
    val scheduled_date: String? = null,
    val scheduled_time_slot: String? = null,
    val status: String,
    val price: String? = null,
)

data class SessionSummary(
    val id: String,
    val patient_id: String,
    val booked_by: String? = null,
    val total_amount: String? = null,
    val status: String,
    val created_at: String? = null,
    val patient: PatientSummary? = null,
    val tickets: List<TicketSummary> = emptyList(),
)

// MBBS module models
data class DoctorProfileResponse(
    val first_name_en: String,
    val last_name_en: String,
    val bmdc_registration: String? = null,
    val specialization: String? = null,
    val qualification: String? = null,
    val signature_url: String? = null,
)

data class MbbsPatientSummary(
    val id: String,
    val mrn: String,
    val first_name_en: String,
    val last_name_en: String,
    val phone_number: String? = null,
    val date_of_birth: String? = null,
    val sex: String? = null,
    val blood_group: String? = null,
    val has_emergency_flag: Boolean? = null,
    val email: String? = null,
    val address_line1: String? = null,
    val address_line2: String? = null,
    val district: String? = null,
    val emergency_contact: String? = null,
    val known_allergies: String? = null,
    val current_medications: String? = null,
    val past_medical_history: String? = null,
    val family_history: String? = null,
)

data class MbbsPatientProfileResponse(
    val patient: MbbsPatientSummary,
    val vitals: List<VitalSignsRecord> = emptyList(),
    val diagnoses: List<DiagnosisRecord> = emptyList(),
    val prescriptions: List<PrescriptionRecord> = emptyList(),
    val test_orders: List<TestOrder> = emptyList(),
    val referrals: List<MbbsReferral> = emptyList(),
    val documents: List<PatientDocument> = emptyList(),
    val previous_appointments: List<PreviousAppointment> = emptyList(),
    val referral_chain: List<ReferralChainEvent> = emptyList(),
)

data class PreviousAppointment(
    val doctor: PreviousAppointmentDoctor? = null,
    val assigned_at: String? = null,
    val appointment_activity: String? = null,
)

data class PreviousAppointmentDoctor(
    val user: PreviousAppointmentUser? = null,
)

data class PreviousAppointmentUser(
    val firstNameEn: String? = null,
    val lastNameEn: String? = null,
)

data class ReferralChainEvent(
    val step_label: String? = null,
    val actor_role: String? = null,
    val actor_name: String? = null,
    val created_at: String? = null,
)

data class TeleconsultSessionResponse(
    val id: String? = null,
    val referral_id: String? = null,
    val patient_id: String? = null,
    val specialist_id: String? = null,
    val status: String? = null,
    val room_name: String? = null,
    val started_at: String? = null,
    val ended_at: String? = null,
    val created_at: String? = null,
)

data class PatientDocument(
    val id: String? = null,
    val patient_id: String? = null,
    val file_name: String? = null,
    val file_type: String? = null,
    val file_size: Long? = null,
    val file_url: String? = null,
    val uploaded_at: String? = null,
)

data class VitalSignsRecord(
    val id: String,
    val recorded_at: String? = null,
    val blood_pressure_systolic: Int? = null,
    val blood_pressure_diastolic: Int? = null,
    val heart_rate: Int? = null,
    val temperature: Double? = null,
    val oxygen_saturation: Double? = null,
    val notes: String? = null,
    val is_abnormal: Boolean = false,
    val systolic_bp: Int? = null,
    val diastolic_bp: Int? = null,
    val pulse_bpm: Int? = null,
    val spo2_pct: Int? = null,
    val temperature_c: Double? = null,
    val respiratory_rate: Int? = null,
    val weight_kg: Double? = null,
    val height_cm: Double? = null,
    val bmi: Double? = null,
)

data class DiagnosisIcd10Ref(
    val code: String? = null,
    val description: String? = null,
)

data class DiagnosisRecord(
    val id: String,
    val diagnosis: String,
    val icd10_code: String? = null,
    val diagnosis_type: String? = null,
    val notes: String? = null,
    val created_at: String? = null,
    val is_primary: Boolean = false,
    val diagnosed_at: String? = null,
    val preliminary_diagnosis: String? = null,
    val chief_complaint: String? = null,
    val icd10: DiagnosisIcd10Ref? = null,
)

data class PatientReport(
    val id: String,
    val report_type: String,
    val file_url: String? = null,
    val file_name: String? = null,
    val file_size: Int? = null,
    val generated_at: String? = null,
)

data class PendingNotification(
    val id: String,
    val title: String,
    val body: String,
    val session_id: String? = null,
    val type: String? = null,
    val patient_id: String? = null,
    val created_at: String? = null,
)

data class PrescriptionMedication(
    val generic_name: String,
    val brand_name: String? = null,
    val dosage: String? = null,
    val frequency: String? = null,
    val duration_days: Int? = null,
    val route: String? = null,
    val special_instructions: String? = null,
)

data class PrescriptionRecord(
    val id: String,
    val medication_name: String = "",
    val dosage: String? = null,
    val frequency: String? = null,
    val duration: String? = null,
    val instructions: String? = null,
    val created_at: String? = null,
    val issued_at: String? = null,
    val status: String? = null,
    val digital_signature_url: String? = null,
    val medications: List<PrescriptionMedication> = emptyList(),
    val notes: String? = null,
)

// Caregiver models
data class CaregiverProfileResponse(
    val user: CaregiverUser,
)

data class CaregiverUser(
    val firstNameEn: String? = null,
    val lastNameEn: String? = null,
)

data class CaregiverPatient(
    val id: String,
    val mrn: String? = null,
    val first_name_en: String,
    val last_name_en: String? = null,
    val phone_number: String? = null,
    val date_of_birth: String? = null,
    val sex: String? = null,
    val blood_group: String? = null,
    val service_type: String? = null,
    val patient_type: String? = null,
    val district: String? = null,
)

data class ActivityLog(
    val id: String,
    val activity_type: String,
    val notes: String? = null,
    val created_at: String? = null,
    val patient: ActivityLogPatient? = null,
)

data class ActivityLogPatient(
    val first_name_en: String? = null,
    val last_name_en: String? = null,
)

data class ConditionReport(
    val id: String,
    val report_type: String,
    val description: String = "",
    val severity: String = "MODERATE",
    val created_at: String? = null,
    val patient: ConditionReportPatient? = null,
    val alert_sent_to_nurse: Boolean = false,
    val alert_sent_to_doctor: Boolean = false,
)

data class ConditionReportPatient(
    val first_name_en: String? = null,
    val last_name_en: String? = null,
)

data class CreateActivityLogRequest(
    val patient_id: String,
    val activity_type: String,
    val notes: String? = null,
)

data class CreateConditionReportRequest(
    val patient_id: String,
    val report_type: String,
    val description: String,
    val severity: String = "MODERATE",
)

// ── CG-006: GPS Check-In/Out models ──
data class CaregiverCheckInOut(
    val id: String,
    val patient_id: String,
    val check_in_time: String? = null,
    val check_out_time: String? = null,
    val check_in_latitude: Double? = null,
    val check_in_longitude: Double? = null,
    val check_out_latitude: Double? = null,
    val check_out_longitude: Double? = null,
    val distance_meters: Double? = null,
    val status: String = "CHECKED_IN",
    val patient: CaregiverCheckInOutPatient? = null,
)

data class CaregiverCheckInOutPatient(
    val first_name_en: String? = null,
    val last_name_en: String? = null,
    val address_line1: String? = null,
    val district: String? = null,
)

data class CaregiverCheckInRequest(
    val patient_id: String,
    val latitude: Double,
    val longitude: Double,
)

data class CaregiverCheckOutRequest(
    val latitude: Double,
    val longitude: Double,
)

// ── CG-008: Timesheet models ──
data class CaregiverTimesheet(
    val id: String,
    val shift_date: String,
    val check_in_time: String? = null,
    val check_out_time: String? = null,
    val total_hours: Double? = null,
    val service_type: String? = null,
    val status: String = "ACTIVE",
    val patient: CaregiverTimesheetPatient? = null,
)

data class CaregiverTimesheetPatient(
    val first_name_en: String? = null,
    val last_name_en: String? = null,
)

// ICD-10
data class Icd10Code(
    val code: String,
    val description: String,
)

data class CreateDiagnosisRequest(
    val icd10_code: String? = null,
    val chief_complaint: String? = null,
    val history_of_present_illness: String? = null,
    val review_of_systems: String? = null,
    val examination_findings: String? = null,
    val preliminary_diagnosis: String,
    val is_primary: Boolean = true,
)

data class CreateVitalsRequest(
    val systolic_bp: Int? = null,
    val diastolic_bp: Int? = null,
    val pulse_bpm: Int? = null,
    val temperature_c: Double? = null,
    val spo2_pct: Int? = null,
    val respiratory_rate: Int? = null,
    val weight_kg: Double? = null,
    val height_cm: Double? = null,
    val notes: String? = null,
)

data class CreateMedicationRequest(
    val generic_name: String,
    val brand_name: String? = null,
    val dosage: String,
    val frequency: String,
    val duration_days: Int,
    val route: String = "Oral",
    val special_instructions: String? = null,
)

data class CreatePrescriptionRequest(
    val notes: String? = null,
    val medications: List<CreateMedicationRequest>,
)

data class TestCatalogItem(
    val id: String,
    val test_name: String,
    val test_code: String,
    val category: String,
    val turnaround_hours: Int? = null,
    val is_active: Boolean = true,
)

data class TestOrderTestRef(
    val test_name: String? = null,
    val test_code: String? = null,
    val category: String? = null,
)

data class TestOrderResult(
    val result_value: String? = null,
    val is_critical: Boolean = false,
    val is_abnormal: Boolean = false,
)

data class TestOrder(
    val id: String,
    val status: String? = null,
    val ordered_at: String? = null,
    val clinical_notes: String? = null,
    val test: TestOrderTestRef? = null,
    val results: List<TestOrderResult>? = null,
)

data class OrderTestsRequest(
    val test_ids: List<String>,
    val clinical_notes: String? = null,
)

data class MbbsReferral(
    val id: String,
    val specialty_code: String? = null,
    val referral_reason: String? = null,
    val clinical_summary: String? = null,
    val is_emergency: Boolean = false,
    val status: String? = null,
    val created_at: String? = null,
)

data class CreateReferralRequest(
    val specialty_code: String,
    val referral_reason: String,
    val clinical_summary: String? = null,
    val is_emergency: Boolean? = null,
)

// Patient self-info matching call center module fields
data class PatientInfoRequest(
    val full_name_en: String? = null,
    val full_name_bn: String? = null,
    val date_of_birth: String? = null,
    val sex: String? = null,
    val blood_group: String? = null,
    val primary_phone: String? = null,
    val alternative_phone: String? = null,
    val emergency_contact_name: String? = null,
    val emergency_contact_relation: String? = null,
    val emergency_contact_phone: String? = null,
    val division: String? = null,
    val district: String? = null,
    val thana: String? = null,
    val address_detail: String? = null,
)

data class PatientInfoResponse(
    val id: String? = null,
    val mrn: String? = null,
    val first_name_en: String? = null,
    val last_name_en: String? = null,
    val first_name_bn: String? = null,
    val last_name_bn: String? = null,
    val date_of_birth: String? = null,
    val sex: String? = null,
    val blood_group: String? = null,
    val phone_number: String? = null,
    val email: String? = null,
    val address_line1: String? = null,
    val address_line2: String? = null,
    val district: String? = null,
    val emergency_contact: String? = null,
    val emergency_contact_name: String? = null,
    val emergency_contact_relation: String? = null,
    val alternative_phone: String? = null,
)

// =============================================================================
// 2. RETROFIT ENDPOINT INTERFACE DEFINITION
// =============================================================================
interface AuthApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/mobile_signup")
    suspend fun signup(@Body request: SignupRequest): SignupResponse

    @POST("notifications/register-token")
    suspend fun registerToken(@Body request: RegisterTokenRequest): Map<String, Any?>

    @GET("notifications/pending")
    suspend fun getPendingNotifications(): List<PendingNotification>

    @GET("bookings/session/{id}")
    suspend fun getBookingSession(@Path("id") sessionId: String): BookingSessionResponse

    @GET("bookings/my-sessions")
    suspend fun getMySessions(): List<SessionSummary>

    // ── MBBS Doctor Endpoints ──
    @GET("mbbs/doctor-profile")
    suspend fun getMbbsDoctorProfile(): DoctorProfileResponse

    @GET("mbbs/patients")
    suspend fun getMbbsPatients(): List<MbbsPatientSummary>

    @GET("mbbs/patients/{id}")
    suspend fun getMbbsPatientProfile(@Path("id") patientId: String): MbbsPatientProfileResponse

    @POST("mbbs/patients/{id}/start-visit")
    suspend fun startPatientVisit(@Path("id") patientId: String): Map<String, Any?>

    @POST("mbbs/patients/{id}/mark-arrived")
    suspend fun markArrived(@Path("id") patientId: String): Map<String, Any?>

    @GET("mbbs/patients/{id}/vitals")
    suspend fun getMbbsPatientVitals(@Path("id") patientId: String): List<VitalSignsRecord>

    @GET("mbbs/patients/{id}/diagnoses")
    suspend fun getMbbsPatientDiagnoses(@Path("id") patientId: String): List<DiagnosisRecord>

    @GET("mbbs/patients/{id}/prescriptions")
    suspend fun getMbbsPatientPrescriptions(@Path("id") patientId: String): List<PrescriptionRecord>

    // Consent
    @POST("mbbs/patients/{id}/respond-consent")
    suspend fun respondConsent(@Path("id") patientId: String, @Body request: Map<String, String>): Map<String, Any?>

    // ── MBBS Clinical Endpoints ──
    @POST("mbbs/patients/{id}/vitals")
    suspend fun createMbbsVitals(@Path("id") patientId: String, @Body request: CreateVitalsRequest): Map<String, Any?>

    @POST("mbbs/patients/{id}/diagnoses")
    suspend fun createMbbsDiagnosis(@Path("id") patientId: String, @Body request: CreateDiagnosisRequest): Map<String, Any?>

    @GET("mbbs/icd10/search")
    suspend fun searchIcd10(@retrofit2.http.Query("q") query: String): List<Icd10Code>

    @POST("mbbs/patients/{id}/prescriptions")
    suspend fun createMbbsPrescription(@Path("id") patientId: String, @Body request: CreatePrescriptionRequest): Map<String, Any?>

    @GET("mbbs/patients/{id}/test-orders")
    suspend fun getTestOrders(@Path("id") patientId: String): List<TestOrder>

    @GET("mbbs/test-catalog")
    suspend fun getTestCatalog(): List<TestCatalogItem>

    @POST("mbbs/patients/{id}/order-tests")
    suspend fun orderTests(@Path("id") patientId: String, @Body request: OrderTestsRequest): Map<String, Any?>

    @GET("mbbs/patients/{id}/referrals")
    suspend fun getMbbsReferrals(@Path("id") patientId: String): List<MbbsReferral>

    @POST("mbbs/patients/{id}/referrals")
    suspend fun createMbbsReferral(@Path("id") patientId: String, @Body request: CreateReferralRequest): Map<String, Any?>

    // ── Caregiver Endpoints ──
    @GET("caregiver/profile")
    suspend fun getCaregiverProfile(): CaregiverProfileResponse

    @GET("caregiver/patients")
    suspend fun getCaregiverPatients(): List<CaregiverPatient>

    @GET("caregiver/activities")
    suspend fun getCaregiverActivities(@retrofit2.http.Query("patient_id") patientId: String?): List<ActivityLog>

    @GET("caregiver/condition-reports")
    suspend fun getCaregiverConditionReports(@retrofit2.http.Query("patient_id") patientId: String?): List<ConditionReport>

    @POST("caregiver/activities")
    suspend fun createCaregiverActivity(@Body request: CreateActivityLogRequest): Map<String, Any?>

    @POST("caregiver/condition-reports")
    suspend fun createConditionReport(@Body request: CreateConditionReportRequest): Map<String, Any?>

    @POST("caregiver/condition-reports/{id}/alert")
    suspend fun sendCaregiverAlert(@Path("id") reportId: String, @Body target: Map<String, String>): Map<String, Any?>

    // ── CG-006: GPS Check-In/Out Endpoints ──
    @POST("caregiver/check-in")
    suspend fun caregiverCheckIn(@Body request: CaregiverCheckInRequest): Map<String, Any?>

    @POST("caregiver/check-out/{id}")
    suspend fun caregiverCheckOut(@Path("id") recordId: String, @Body request: CaregiverCheckOutRequest): Map<String, Any?>

    @GET("caregiver/check-in-out")
    suspend fun getCaregiverCheckInOuts(@retrofit2.http.Query("patient_id") patientId: String?): List<CaregiverCheckInOut>

    @GET("caregiver/check-in-out/today")
    suspend fun getTodayCheckInOut(): CaregiverCheckInOut?

    // ── CG-008: Timesheet Endpoints ──
    @GET("caregiver/timesheets")
    suspend fun getCaregiverTimesheets(@retrofit2.http.Query("month") month: String?): List<CaregiverTimesheet>

    // ── Patient Self-Info Endpoints ──
    @GET("patients/self")
    suspend fun getSelfPatientInfo(): PatientInfoResponse

    @PATCH("patients/self")
    suspend fun submitPatientInfo(@Body request: PatientInfoRequest)

    // ── Patient Document Endpoints ──
    @GET("patients/self/documents")
    suspend fun getPatientDocuments(): List<PatientDocument>

    @Multipart
    @POST("patients/self/documents")
    suspend fun uploadPatientDocument(@Part file: MultipartBody.Part): PatientDocument

    @GET("patients/self/reports")
    suspend fun getSelfReports(): List<PatientReport>

    // ── Teleconsult Endpoints ──
    @POST("api/teleconsult/sessions")
    suspend fun createTeleconsultSession(@Body request: Map<String, String>): TeleconsultSessionResponse

    @GET("api/teleconsult/sessions/by-referral/{referralId}")
    suspend fun getTeleconsultSessionByReferral(@Path("referralId") referralId: String): TeleconsultSessionResponse

    @PATCH("api/teleconsult/sessions/{id}/status")
    suspend fun updateTeleconsultSessionStatus(
        @Path("id") id: String,
        @Body request: Map<String, String>,
    ): TeleconsultSessionResponse
}

// =============================================================================
// 3. SINGLETON CLIENT INSTANCE (Emulating Gateway Connection)
// =============================================================================
object RetrofitClient {
    // 10.0.2.2 automatically bridges out to your host development computer's localhost:3000
    private const val BASE_URL = "http://192.168.0.105:3001"

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val token = TokenManager.getToken()
            val request = if (token != null) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                original
            }
            chain.proceed(request)
        }
        .build()

    val apiService: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}