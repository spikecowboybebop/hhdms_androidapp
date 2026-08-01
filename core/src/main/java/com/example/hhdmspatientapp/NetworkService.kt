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
    @com.google.gson.annotations.SerializedName("user_id")
    val userId: String? = null,
    @com.google.gson.annotations.SerializedName("patient_id")
    val patientId: String? = null,
    @com.google.gson.annotations.SerializedName("access_token")
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
    val appointment_activity: String? = null,
    val patient_consent: String? = null,
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
    @com.google.gson.annotations.SerializedName("first_name_en")
    val firstNameEn: String? = null,
    @com.google.gson.annotations.SerializedName("last_name_en")
    val lastNameEn: String? = null,
)

data class ReferralChainEvent(
    val step_label: String? = null,
    val actor_role: String? = null,
    val actor_name: String? = null,
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
    @com.google.gson.annotations.SerializedName("first_name_en")
    val firstNameEn: String? = null,
    @com.google.gson.annotations.SerializedName("last_name_en")
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

// ── Nurse Module Models ──

data class NurseProfileResponse(
    val user: NurseUser,
    val nurse_type: String? = null,
    val specialization: String? = null,
    val license_number: String? = null,
)

data class NurseUser(
    @com.google.gson.annotations.SerializedName("first_name_en")
    val firstNameEn: String? = null,
    @com.google.gson.annotations.SerializedName("last_name_en")
    val lastNameEn: String? = null,
)

data class NursePatient(
    val id: String,
    val mrn: String? = null,
    val first_name_en: String,
    val last_name_en: String? = null,
    val phone_number: String? = null,
    val date_of_birth: String? = null,
    val sex: String? = null,
    val blood_group: String? = null,
    val patient_type: String? = null,
    val address_line1: String? = null,
    val district: String? = null,
    val age_years: Int? = null,
    val nurse_type: String? = null,
)

// NS-003: Assigned Patient Schedule
data class NurseScheduleEntry(
    val id: String,
    val patient_id: String,
    val scheduled_date: String? = null,
    val scheduled_time_slot: String? = null,
    val service_requirements: String? = null,
    val status: String = "PENDING",
    val patient: NursePatient? = null,
)

// NS-004: Vital Signs Recording (nurse-specific)
data class NurseVitalSigns(
    val id: String,
    val patient_id: String,
    val recorded_at: String? = null,
    val systolic_bp: Int? = null,
    val diastolic_bp: Int? = null,
    val pulse_bpm: Int? = null,
    val temperature_c: Double? = null,
    val spo2_pct: Int? = null,
    val respiratory_rate: Int? = null,
    val blood_glucose: Double? = null,
    val notes: String? = null,
    val is_abnormal: Boolean = false,
    val previous_vitals: NurseVitalSigns? = null,
)

data class NurseCreateVitalsRequest(
    val patient_id: String,
    val systolic_bp: Int? = null,
    val diastolic_bp: Int? = null,
    val pulse_bpm: Int? = null,
    val temperature_c: Double? = null,
    val spo2_pct: Int? = null,
    val respiratory_rate: Int? = null,
    val blood_glucose: Double? = null,
    val notes: String? = null,
)

// NS-005: Medication Administration Record
data class MedicationAdministration(
    val id: String,
    val patient_id: String,
    val drug_name: String,
    val dosage: String? = null,
    val route: String? = null,
    val administered_at: String? = null,
    val notes: String? = null,
    val nurse_name: String? = null,
)

data class MedicationAdministrationRecord(
    val patient_id: String,
    val patient_name: String? = null,
    val visit_date: String? = null,
    val entries: List<MedicationAdministration> = emptyList(),
)

data class NurseCreateMedicationAdminRequest(
    val patient_id: String,
    val drug_name: String,
    val dosage: String? = null,
    val route: String = "Oral",
    val notes: String? = null,
)

// NS-006: IV Fluid Monitoring
data class IvFluidRecord(
    val id: String,
    val patient_id: String,
    val fluid_type: String? = null,
    val rate_ml_hr: Double? = null,
    val volume_given_ml: Double? = null,
    val site_condition: String? = null,
    val started_at: String? = null,
    val stopped_at: String? = null,
    val hourly_balance: List<HourlyFluidBalance>? = null,
    val status: String = "ACTIVE",
)

data class HourlyFluidBalance(
    val hour: String? = null,
    val intake_ml: Double? = null,
    val output_ml: Double? = null,
    val balance_ml: Double? = null,
)

data class NurseCreateIvFluidRequest(
    val patient_id: String,
    val fluid_type: String,
    val rate_ml_hr: Double,
    val site_condition: String? = null,
)

data class NurseUpdateIvFluidRequest(
    val rate_ml_hr: Double? = null,
    val volume_given_ml: Double? = null,
    val site_condition: String? = null,
    val status: String? = null,
)

// NS-007: Wound Care Documentation
data class WoundCareRecord(
    val id: String,
    val patient_id: String,
    val wound_location: String? = null,
    val wound_measurements: String? = null,
    val wound_condition: String? = null,
    val dressing_applied: String? = null,
    val healing_progress: String? = null,
    val photo_url: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val recorded_at: String? = null,
    val previous_records: List<WoundCareRecord>? = null,
)

data class NurseCreateWoundCareRequest(
    val patient_id: String,
    val wound_location: String? = null,
    val wound_measurements: String? = null,
    val wound_condition: String? = null,
    val dressing_applied: String? = null,
    val healing_progress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

// NS-008: Nursing Care Report
data class NursingCareReport(
    val id: String,
    val patient_id: String,
    val visit_date: String? = null,
    val vitals_summary: String? = null,
    val medications_summary: String? = null,
    val procedures_performed: String? = null,
    val patient_response: String? = null,
    val handover_notes: String? = null,
    val generated_at: String? = null,
    val pdf_url: String? = null,
    val patient: NursePatient? = null,
)

// NS-010: Shift Handover Notes
data class ShiftHandover(
    val id: String,
    val shift_date: String? = null,
    val current_status: String? = null,
    val active_concerns: String? = null,
    val medications_due: String? = null,
    val physician_orders: String? = null,
    val patient_instructions: String? = null,
    val handed_over_by: String? = null,
    val handed_over_to: String? = null,
    val signed_at: String? = null,
    val patient: NursePatient? = null,
    val status: String = "DRAFT",
)

data class NurseCreateHandoverRequest(
    val patient_id: String,
    val current_status: String,
    val active_concerns: String? = null,
    val medications_due: String? = null,
    val physician_orders: String? = null,
    val patient_instructions: String? = null,
    val handed_over_to_email: String? = null,
)

// NS-012: Doctor Consultation Request
data class NurseConsultationRequest(
    val id: String,
    val patient_id: String,
    val concern_summary: String? = null,
    val current_vitals_snapshot: String? = null,
    val urgency_level: String = "NORMAL",
    val status: String = "PENDING",
    val created_at: String? = null,
    val doctor_response: String? = null,
    val responded_at: String? = null,
    val patient: NursePatient? = null,
)

data class NurseCreateConsultationRequest(
    val patient_id: String,
    val concern_summary: String,
    val urgency_level: String = "NORMAL",
)

// NS-013: Nursing Supply Tracking
data class SupplyUsageRecord(
    val id: String,
    val patient_id: String,
    val supply_name: String,
    val quantity_used: Int = 1,
    val unit: String? = null,
    val recorded_at: String? = null,
    val patient: NursePatient? = null,
)

data class NurseCreateSupplyUsageRequest(
    val patient_id: String,
    val supply_name: String,
    val quantity_used: Int = 1,
    val unit: String? = null,
)

// NS-014: Pediatric Specific Care
data class FeedingLog(
    val id: String,
    val patient_id: String,
    val feeding_type: String? = null,
    val volume_ml: Double? = null,
    val frequency: String? = null,
    val recorded_at: String? = null,
)

data class GrowthRecord(
    val id: String,
    val patient_id: String,
    val weight_kg: Double? = null,
    val height_cm: Double? = null,
    val head_circumference_cm: Double? = null,
    val recorded_at: String? = null,
    val percentile_weight: Double? = null,
    val percentile_height: Double? = null,
    val percentile_head: Double? = null,
)

data class VaccinationRecord(
    val id: String,
    val patient_id: String,
    val vaccine_name: String? = null,
    val dose_number: Int? = null,
    val administered_date: String? = null,
    val next_due_date: String? = null,
    val status: String = "COMPLETED",
)

data class NurseCreateFeedingLogRequest(
    val patient_id: String,
    val feeding_type: String,
    val volume_ml: Double? = null,
    val frequency: String? = null,
)

data class NurseCreateGrowthRecordRequest(
    val patient_id: String,
    val weight_kg: Double? = null,
    val height_cm: Double? = null,
    val head_circumference_cm: Double? = null,
)

data class NurseCreateVaccinationRequest(
    val patient_id: String,
    val vaccine_name: String,
    val dose_number: Int? = null,
    val next_due_date: String? = null,
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

data class PatientReport(
    val id: String,
    val report_type: String,
    val file_url: String? = null,
    val file_name: String? = null,
    val file_size: Int? = null,
    val generated_at: String? = null,
)

data class CreatePaymentRequest(
    val booking_session_id: String,
)

data class PaymentIntentResponse(
    val paymentId: String,
    val clientSecret: String,
    val publishableKey: String,
    val amount: Double,
    val serviceType: String,
)

data class ConfirmPaymentRequest(
    val payment_id: String,
)

data class PaymentStatusResponse(
    val paid: Boolean,
    val paymentId: String? = null,
)

data class ChatConversation(
    val id: String,
    val assignment_id: String,
    val doctor_id: String,
    val patient_id: String,
    val created_at: String? = null,
    val updated_at: String? = null,
    val assignment: ChatAssignment? = null,
    val doctor: ChatUser? = null,
    val patient: ChatPatient? = null,
    val messages: List<ChatMessage> = emptyList(),
)

data class ChatAssignment(
    val appointment_activity: String? = null,
    val patient_consent: String? = null,
)

data class ChatUser(
    val id: String,
    val firstNameEn: String? = null,
    val lastNameEn: String? = null,
)

data class ChatPatient(
    val id: String,
    val first_name_en: String? = null,
    val last_name_en: String? = null,
)

data class ChatMessage(
    val id: String,
    val conversation_id: String,
    val sender_id: String,
    val content: String,
    val read: Boolean = false,
    val created_at: String? = null,
)

data class SendMessageRequest(
    val content: String,
)

data class UnreadCount(
    val conversationId: String,
    val unreadCount: Int,
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

    @POST("caregiver/condition-reports/{id}/alert/{target}")
    suspend fun sendCaregiverAlert(@Path("id") reportId: String, @Path("target") target: String): Map<String, Any?>

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

    // ── Patient Reports ──
    @GET("patients/self/reports")
    suspend fun getSelfReports(): List<PatientReport>

    // ── Payments ──
    @POST("payments/create-intent")
    suspend fun createPaymentIntent(@Body request: CreatePaymentRequest): PaymentIntentResponse

    @POST("payments/confirm")
    suspend fun confirmPayment(@Body request: ConfirmPaymentRequest)

    @GET("payments/status/{sessionId}")
    suspend fun getPaymentStatus(@Path("sessionId") sessionId: String): PaymentStatusResponse

    // ── Chat ──
    @GET("chat/conversations")
    suspend fun getChatConversations(): List<ChatConversation>

    @POST("chat/start")
    suspend fun startChat(): ChatConversation

    @GET("chat/unread")
    suspend fun getChatUnreadCounts(): List<UnreadCount>

    @POST("chat/conversation/{assignmentId}")
    suspend fun getOrCreateConversation(@Path("assignmentId") assignmentId: String): ChatConversation

    @GET("chat/{conversationId}/messages")
    suspend fun getChatMessages(@Path("conversationId") conversationId: String): List<ChatMessage>

    @POST("chat/{conversationId}/messages")
    suspend fun sendChatMessage(
        @Path("conversationId") conversationId: String,
        @Body request: SendMessageRequest,
    ): ChatMessage

    @POST("chat/{conversationId}/read")
    suspend fun markChatRead(@Path("conversationId") conversationId: String)

    // ── Nurse Module Endpoints ──

    @GET("nurse/profile")
    suspend fun getNurseProfile(): NurseProfileResponse

    @GET("nurse/patients")
    suspend fun getNursePatients(): List<NursePatient>

    // NS-003: Schedule
    @GET("nurse/schedule")
    suspend fun getNurseSchedule(@retrofit2.http.Query("date") date: String?): List<NurseScheduleEntry>

    // NS-004: Vital Signs
    @GET("nurse/patients/{id}/vitals")
    suspend fun getNursePatientVitals(@Path("id") patientId: String): List<NurseVitalSigns>

    @POST("nurse/patients/{id}/vitals")
    suspend fun createNurseVitals(@Path("id") patientId: String, @Body request: NurseCreateVitalsRequest): Map<String, Any?>

    // NS-005: Medication Administration
    @GET("nurse/patients/{id}/medication-admin")
    suspend fun getMedicationAdministrations(@Path("id") patientId: String): MedicationAdministrationRecord

    @POST("nurse/patients/{id}/medication-admin")
    suspend fun createMedicationAdministration(@Path("id") patientId: String, @Body request: NurseCreateMedicationAdminRequest): Map<String, Any?>

    // NS-006: IV Fluid Monitoring
    @GET("nurse/patients/{id}/iv-fluids")
    suspend fun getIvFluidRecords(@Path("id") patientId: String): List<IvFluidRecord>

    @POST("nurse/patients/{id}/iv-fluids")
    suspend fun createIvFluidRecord(@Path("id") patientId: String, @Body request: NurseCreateIvFluidRequest): Map<String, Any?>

    @PATCH("nurse/iv-fluids/{id}")
    suspend fun updateIvFluidRecord(@Path("id") recordId: String, @Body request: NurseUpdateIvFluidRequest): Map<String, Any?>

    // NS-007: Wound Care
    @GET("nurse/patients/{id}/wound-care")
    suspend fun getWoundCareRecords(@Path("id") patientId: String): List<WoundCareRecord>

    @POST("nurse/patients/{id}/wound-care")
    suspend fun createWoundCareRecord(@Path("id") patientId: String, @Body request: NurseCreateWoundCareRequest): Map<String, Any?>

    @Multipart
    @POST("nurse/patients/{id}/wound-care/photo")
    suspend fun uploadWoundPhoto(@Path("id") patientId: String, @Part photo: MultipartBody.Part, @Part("wound_id") woundId: okhttp3.RequestBody?): PatientDocument

    // NS-008: Nursing Care Report
    @GET("nurse/patients/{id}/care-report")
    suspend fun getNursingCareReport(@Path("id") patientId: String): NursingCareReport?

    @POST("nurse/patients/{id}/care-report")
    suspend fun generateNursingCareReport(@Path("id") patientId: String): NursingCareReport

    // NS-010: Shift Handover
    @GET("nurse/handovers")
    suspend fun getNurseHandovers(@retrofit2.http.Query("patient_id") patientId: String?): List<ShiftHandover>

    @POST("nurse/handovers")
    suspend fun createHandover(@Body request: NurseCreateHandoverRequest): Map<String, Any?>

    @POST("nurse/handovers/{id}/sign")
    suspend fun signHandover(@Path("id") handoverId: String, @Body body: Map<String, String>): Map<String, Any?>

    // NS-012: Doctor Consultation Request
    @GET("nurse/consultation-requests")
    suspend fun getNurseConsultationRequests(@retrofit2.http.Query("patient_id") patientId: String?): List<NurseConsultationRequest>

    @POST("nurse/consultation-requests")
    suspend fun createConsultationRequest(@Body request: NurseCreateConsultationRequest): Map<String, Any?>

    // NS-013: Supply Tracking
    @GET("nurse/supply-usage")
    suspend fun getSupplyUsageRecords(@retrofit2.http.Query("patient_id") patientId: String?): List<SupplyUsageRecord>

    @POST("nurse/supply-usage")
    suspend fun createSupplyUsage(@Body request: NurseCreateSupplyUsageRequest): Map<String, Any?>

    // NS-014: Pediatric Care
    @GET("nurse/patients/{id}/feeding-logs")
    suspend fun getFeedingLogs(@Path("id") patientId: String): List<FeedingLog>

    @POST("nurse/patients/{id}/feeding-logs")
    suspend fun createFeedingLog(@Path("id") patientId: String, @Body request: NurseCreateFeedingLogRequest): Map<String, Any?>

    @GET("nurse/patients/{id}/growth-records")
    suspend fun getGrowthRecords(@Path("id") patientId: String): List<GrowthRecord>

    @POST("nurse/patients/{id}/growth-records")
    suspend fun createGrowthRecord(@Path("id") patientId: String, @Body request: NurseCreateGrowthRecordRequest): Map<String, Any?>

    @GET("nurse/patients/{id}/vaccinations")
    suspend fun getVaccinationRecords(@Path("id") patientId: String): List<VaccinationRecord>

    @POST("nurse/patients/{id}/vaccinations")
    suspend fun createVaccinationRecord(@Path("id") patientId: String, @Body request: NurseCreateVaccinationRequest): Map<String, Any?>

}

// =============================================================================
// 3. SINGLETON CLIENT INSTANCE (Emulating Gateway Connection)
// =============================================================================
object RetrofitClient {
    // Render production backend
    private const val BASE_URL = "https://hhdms-api.onrender.com"

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