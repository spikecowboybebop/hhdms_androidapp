package com.example.hhdmspatientapp // Make sure this matches your project's actual package name

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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
)

data class MbbsPatientProfileResponse(
    val patient: MbbsPatientSummary,
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
)

data class DiagnosisRecord(
    val id: String,
    val diagnosis: String,
    val icd10_code: String? = null,
    val diagnosis_type: String? = null,
    val notes: String? = null,
    val created_at: String? = null,
)

data class PendingNotification(
    val id: String,
    val title: String,
    val body: String,
    val session_id: String? = null,
    val type: String? = null,
    val created_at: String? = null,
)

data class PrescriptionRecord(
    val id: String,
    val medication_name: String,
    val dosage: String? = null,
    val frequency: String? = null,
    val duration: String? = null,
    val instructions: String? = null,
    val created_at: String? = null,
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

    @GET("mbbs/patients/{id}/vitals")
    suspend fun getMbbsPatientVitals(@Path("id") patientId: String): List<VitalSignsRecord>

    @GET("mbbs/patients/{id}/diagnoses")
    suspend fun getMbbsPatientDiagnoses(@Path("id") patientId: String): List<DiagnosisRecord>

    @GET("mbbs/patients/{id}/prescriptions")
    suspend fun getMbbsPatientPrescriptions(@Path("id") patientId: String): List<PrescriptionRecord>
}

// =============================================================================
// 3. SINGLETON CLIENT INSTANCE (Emulating Gateway Connection)
// =============================================================================
object RetrofitClient {
    // 10.0.2.2 automatically bridges out to your host development computer's localhost:3000
    private const val BASE_URL = "http://192.168.0.101:3001/"

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