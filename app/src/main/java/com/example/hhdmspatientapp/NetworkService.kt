package com.example.hhdmspatientapp // Make sure this matches your project's actual package name

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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

// ── Pending Notification ──
data class PendingNotification(
    val id: String,
    val title: String,
    val body: String,
    val session_id: String? = null,
    val type: String? = null,
    val created_at: String? = null,
)

// =============================================================================
// MBBS MODULE — Data Models
// =============================================================================

// ── Doctor Profile ──
data class DoctorProfileResponse(
    val first_name_en: String,
    val last_name_en: String,
    val bmdc_registration: String? = null,
    val specialization: String? = null,
    val qualification: String? = null,
    val signature_url: String? = null,
)

data class SignatureResponse(
    val signature_url: String? = null,
)

data class UpdateSignatureRequest(
    val signature_url: String,
)

// ── Patient ──
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
)

data class MbbsPatientFull(
    val id: String,
    val mrn: String,
    val first_name_en: String,
    val last_name_en: String,
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
    val known_allergies: String? = null,
    val current_medications: String? = null,
    val past_medical_history: String? = null,
    val family_history: String? = null,
    val height_cm: Double? = null,
    val weight_kg: Double? = null,
    val has_emergency_flag: Boolean = false,
    val created_at: String? = null,
    val updated_at: String? = null,
)

data class MbbsPatientProfileResponse(
    val patient: MbbsPatientFull,
    val vitals: List<VitalSignsRecord> = emptyList(),
    val diagnoses: List<DiagnosisRecord> = emptyList(),
    val prescriptions: List<PrescriptionRecord> = emptyList(),
    val referrals: List<MbbsReferral> = emptyList(),
    val emergency_flags: List<EmergencyFlag> = emptyList(),
    val referral_chain: List<ReferralChainEvent> = emptyList(),
    val test_orders: List<TestOrder> = emptyList(),
    val previous_appointments: List<PreviousAppointment> = emptyList(),
    val documents: List<PatientDocument> = emptyList(),
)

data class PreviousAppointment(
    val id: String,
    val doctor_id: String? = null,
    val patient_id: String? = null,
    val assigned_at: String? = null,
    val appointment_activity: String? = null,
    val doctor: PreviousAppointmentDoctor? = null,
)

data class PreviousAppointmentDoctor(
    val user: PreviousAppointmentUser? = null,
)

data class PreviousAppointmentUser(
    val firstNameEn: String? = null,
    val lastNameEn: String? = null,
)

// ── Vital Signs ──
data class VitalSignsRecord(
    val id: String,
    val patient_id: String? = null,
    val doctor_id: String? = null,
    val systolic_bp: Int? = null,
    val diastolic_bp: Int? = null,
    val pulse_bpm: Int? = null,
    val temperature_c: Double? = null,
    val spo2_pct: Int? = null,
    val respiratory_rate: Int? = null,
    val weight_kg: Double? = null,
    val height_cm: Double? = null,
    val bmi: Double? = null,
    val notes: String? = null,
    val is_abnormal: Boolean = false,
    val recorded_at: String? = null,
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

// ── ICD-10 ──
data class Icd10Code(
    val code: String,
    val description: String,
    val category: String? = null,
    val subcategory: String? = null,
)

// ── Diagnosis ──
data class DiagnosisRecord(
    val id: String,
    val patient_id: String? = null,
    val doctor_id: String? = null,
    val icd10_code: String? = null,
    val chief_complaint: String? = null,
    val history_of_present_illness: String? = null,
    val review_of_systems: String? = null,
    val examination_findings: String? = null,
    val preliminary_diagnosis: String? = null,
    val is_primary: Boolean = false,
    val diagnosed_at: String? = null,
    val icd10: Icd10Code? = null,
)

data class CreateDiagnosisRequest(
    val icd10_code: String,
    val chief_complaint: String? = null,
    val history_of_present_illness: String? = null,
    val review_of_systems: String? = null,
    val examination_findings: String? = null,
    val preliminary_diagnosis: String,
    val is_primary: Boolean? = null,
)

// ── Test Catalog ──
data class TestCatalogItem(
    val id: String,
    val test_name: String,
    val test_code: String,
    val category: String,
    val description: String? = null,
    val normal_range: String? = null,
    val unit: String? = null,
    val turnaround_hours: Int? = null,
    val is_active: Boolean = true,
)

data class OrderTestsRequest(
    val test_ids: List<String>,
    val clinical_notes: String? = null,
)

// ── Test Order ──
data class TestOrder(
    val id: String,
    val patient_id: String? = null,
    val doctor_id: String? = null,
    val test_id: String? = null,
    val status: String? = null,
    val clinical_notes: String? = null,
    val ordered_at: String? = null,
    val completed_at: String? = null,
    val test: TestCatalogItem? = null,
    val results: List<TestResult>? = null,
)

data class TestResult(
    val id: String,
    val order_id: String? = null,
    val result_value: String? = null,
    val result_numeric: Double? = null,
    val is_critical: Boolean = false,
    val is_abnormal: Boolean = false,
    val notes: String? = null,
    val lab_technician: String? = null,
    val resulted_at: String? = null,
)

// ── Prescription ──
data class PrescriptionRecord(
    val id: String,
    val patient_id: String? = null,
    val doctor_id: String? = null,
    val diagnosis_id: String? = null,
    val notes: String? = null,
    val digital_signature_url: String? = null,
    val status: String? = null,
    val issued_at: String? = null,
    val expires_at: String? = null,
    val medications: List<PrescriptionMedication> = emptyList(),
)

data class PrescriptionMedication(
    val id: String? = null,
    val prescription_id: String? = null,
    val generic_name: String,
    val brand_name: String? = null,
    val dosage: String,
    val frequency: String,
    val duration_days: Int,
    val route: String,
    val special_instructions: String? = null,
)

data class CreatePrescriptionRequest(
    val diagnosis_id: String? = null,
    val notes: String? = null,
    val medications: List<CreateMedicationRequest>,
)

data class CreateMedicationRequest(
    val generic_name: String,
    val brand_name: String? = null,
    val dosage: String,
    val frequency: String,
    val duration_days: Int,
    val route: String,
    val special_instructions: String? = null,
)

// ── Referral ──
data class MbbsReferral(
    val id: String,
    val patient_id: String? = null,
    val referring_doctor_id: String? = null,
    val specialty_code: String? = null,
    val referral_reason: String? = null,
    val clinical_summary: String? = null,
    val is_emergency: Boolean = false,
    val status: String? = null,
    val specialist_id: String? = null,
    val response_notes: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
)

data class CreateReferralRequest(
    val specialty_code: String,
    val referral_reason: String,
    val clinical_summary: String? = null,
    val is_emergency: Boolean? = null,
)

// ── Referral Chain ──
data class ReferralChainEvent(
    val id: String,
    val patient_id: String? = null,
    val step_type: String? = null,
    val step_id: String? = null,
    val step_label: String? = null,
    val actor_role: String? = null,
    val actor_name: String? = null,
    val notes: String? = null,
    val created_at: String? = null,
)

// ── Emergency Flag ──
data class EmergencyFlag(
    val id: String,
    val patient_id: String? = null,
    val flagged_by: String? = null,
    val reason: String? = null,
    val is_active: Boolean = false,
    val created_at: String? = null,
    val resolved_at: String? = null,
)

data class CreateEmergencyFlagRequest(
    val reason: String? = null,
)

// ── Patient Documents ──
data class PatientDocument(
    val id: String,
    val patient_id: String? = null,
    val file_name: String? = null,
    val file_type: String? = null,
    val file_size: Int? = null,
    val file_url: String? = null,
    val uploaded_at: String? = null,
)

data class DocumentTextResponse(
    val text: String,
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

    // Doctor Profile
    @GET("mbbs/doctor-profile")
    suspend fun getMbbsDoctorProfile(): DoctorProfileResponse

    @GET("mbbs/signature")
    suspend fun getMbbsSignature(): SignatureResponse

    @PATCH("mbbs/signature")
    suspend fun updateMbbsSignature(@Body request: UpdateSignatureRequest): Map<String, Any?>

    // Patients
    @GET("mbbs/patients")
    suspend fun getMbbsPatients(): List<MbbsPatientSummary>

    @GET("mbbs/patients/{id}")
    suspend fun getMbbsPatientProfile(@Path("id") patientId: String): MbbsPatientProfileResponse

    @POST("mbbs/patients/{id}/start-visit")
    suspend fun startPatientVisit(@Path("id") patientId: String): Map<String, Any?>

    // Vital Signs
    @POST("mbbs/patients/{id}/vitals")
    suspend fun createMbbsVitals(@Path("id") patientId: String, @Body request: CreateVitalsRequest): VitalSignsRecord

    @GET("mbbs/patients/{id}/vitals")
    suspend fun getMbbsPatientVitals(@Path("id") patientId: String): List<VitalSignsRecord>

    // ICD-10 Search
    @GET("mbbs/icd10/search")
    suspend fun searchIcd10(@Query("q") query: String): List<Icd10Code>

    // Diagnosis
    @POST("mbbs/patients/{id}/diagnoses")
    suspend fun createMbbsDiagnosis(@Path("id") patientId: String, @Body request: CreateDiagnosisRequest): DiagnosisRecord

    @GET("mbbs/patients/{id}/diagnoses")
    suspend fun getMbbsPatientDiagnoses(@Path("id") patientId: String): List<DiagnosisRecord>

    // Test Catalog
    @GET("mbbs/tests/catalog")
    suspend fun getTestCatalog(@Query("category") category: String? = null): List<TestCatalogItem>

    // Test Orders
    @POST("mbbs/patients/{id}/test-orders")
    suspend fun orderTests(@Path("id") patientId: String, @Body request: OrderTestsRequest): Map<String, Any?>

    @GET("mbbs/patients/{id}/test-orders")
    suspend fun getTestOrders(@Path("id") patientId: String): List<TestOrder>

    // Referrals
    @POST("mbbs/patients/{id}/referrals")
    suspend fun createMbbsReferral(@Path("id") patientId: String, @Body request: CreateReferralRequest): MbbsReferral

    @GET("mbbs/patients/{id}/referrals")
    suspend fun getMbbsReferrals(@Path("id") patientId: String): List<MbbsReferral>

    // Referral Chain
    @GET("mbbs/patients/{id}/referral-chain")
    suspend fun getReferralChain(@Path("id") patientId: String): List<ReferralChainEvent>

    // Prescriptions
    @POST("mbbs/patients/{id}/prescriptions")
    suspend fun createMbbsPrescription(@Path("id") patientId: String, @Body request: CreatePrescriptionRequest): Map<String, Any?>

    @GET("mbbs/patients/{id}/prescriptions")
    suspend fun getMbbsPatientPrescriptions(@Path("id") patientId: String): List<PrescriptionRecord>

    // Emergency Flags
    @POST("mbbs/patients/{id}/emergency")
    suspend fun setEmergencyFlag(@Path("id") patientId: String, @Body request: CreateEmergencyFlagRequest): Map<String, Any?>

    @GET("mbbs/patients/{id}/emergency")
    suspend fun getEmergencyFlags(@Path("id") patientId: String): List<EmergencyFlag>

    // Patient Documents
    @GET("mbbs/patients/{id}/documents")
    suspend fun getPatientDocuments(@Path("id") patientId: String): List<PatientDocument>

    @GET("mbbs/patients/{id}/documents/{docId}")
    suspend fun getDocumentDetails(@Path("id") patientId: String, @Path("docId") docId: String): PatientDocument

    @GET("mbbs/patients/{id}/documents/{docId}/text")
    suspend fun getDocumentText(@Path("id") patientId: String, @Path("docId") docId: String): DocumentTextResponse
}

// =============================================================================
// 3. SINGLETON CLIENT INSTANCE (Emulating Gateway Connection)
// =============================================================================
object RetrofitClient {
    // 10.0.2.2 automatically bridges out to your host development computer's localhost:3000
    private const val BASE_URL = "http://192.168.1.40:3001/"

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