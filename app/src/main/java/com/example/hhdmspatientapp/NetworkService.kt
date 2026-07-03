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

data class Ticket(
    val id: String,
    val ticket_no: String,
    val service_type: String,
    val scheduled_date: String? = null,
    val scheduled_time_slot: String? = null,
    val assigned_provider_id: String? = null,
    val price: Double? = null,
    val status: String,
    val details: TicketDetail? = null,
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
    val total_amount: Double? = null,
    val status: String,
    val created_at: String? = null,
    val updated_at: String? = null,
    val patient: PatientSummary? = null,
    val tickets: List<Ticket> = emptyList(),
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

    @GET("bookings/session/{id}")
    suspend fun getBookingSession(@Path("id") sessionId: String): BookingSessionResponse
}

// =============================================================================
// 3. SINGLETON CLIENT INSTANCE (Emulating Gateway Connection)
// =============================================================================
object RetrofitClient {
    // 10.0.2.2 automatically bridges out to your host development computer's localhost:3000
    private const val BASE_URL = "http://192.168.0.100:3001/"

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