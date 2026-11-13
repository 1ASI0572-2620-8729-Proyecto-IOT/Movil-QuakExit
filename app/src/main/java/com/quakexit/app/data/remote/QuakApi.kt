package com.quakexit.app.data.remote

import com.quakexit.app.data.model.*
import retrofit2.http.*

/** Todas las rutas son relativas a /api/v1/. */
interface QuakApi {
    @POST("auth/login") suspend fun login(@Body body: LoginRequest): AuthResponse
    @POST("auth/register") suspend fun register(@Body body: RegisterRequest): AuthResponse
    @GET("system/health") suspend fun health(): SystemHealth

    @GET("earthquakes/recent") suspend fun earthquakes(): List<Earthquake>
    @GET("home/status") suspend fun homeStatus(): HomeStatus
    @POST("emergencies/readings") suspend fun submitReading(@Body body: ReadingRequest): ReadingResult
    @POST("emergencies/{id}/false-alarm") suspend fun markFalseAlarm(@Path("id") id: String): EmergencyEvent

    @GET("devices") suspend fun devices(): List<Device>
    @POST("devices") suspend fun linkDevice(@Body body: LinkDeviceRequest): Device
    @GET("devices/{id}") suspend fun device(@Path("id") id: String): Device
    @POST("devices/{id}/unlock") suspend fun unlockDoor(@Path("id") id: String): Device
    @PUT("devices/{id}/deep-sleep") suspend fun setDeepSleep(@Path("id") id: String, @Body body: DeepSleepRequest): Device

    @GET("home") suspend fun homeProfile(): HomeProfile
    @PUT("home") suspend fun updateHome(@Body body: HomeProfile): HomeProfile
    @POST("drills") suspend fun startDrill(@Body body: DrillRequest): DrillResult

    @GET("security/battery-summary") suspend fun batterySummary(): BatterySummary
    @GET("contacts") suspend fun contacts(): List<Contact>
    @POST("contacts") suspend fun addContact(@Body body: Contact): Contact
    @DELETE("contacts/{id}") suspend fun deleteContact(@Path("id") id: String)

    @GET("notifications/preferences") suspend fun preferences(): NotificationPrefs
    @PUT("notifications/preferences") suspend fun updatePreferences(@Body body: NotificationPrefs): NotificationPrefs
    @POST("notifications/push-tokens") suspend fun registerPushToken(@Body body: PushTokenRequest)
    @HTTP(method = "DELETE", path = "notifications/push-tokens", hasBody = true)
    suspend fun deletePushToken(@Body body: PushTokenRequest)
    @GET("notifications") suspend fun notifications(): List<AppNotification>

    @GET("reports/{type}") suspend fun report(
        @Path("type") type: String,
        @Query("from") from: Long?,
        @Query("to") to: Long?,
        @Query("page") page: Int,
        @Query("size") size: Int = 10,
    ): PageResponse<ReportItem>

    @GET("maintenance/alerts") suspend fun maintenance(): List<MaintenanceAlert>
    @POST("maintenance/alerts/{id}/ack") suspend fun ackMaintenance(@Path("id") id: String): MaintenanceAlert
    @POST("maintenance/alerts/{id}/resolve") suspend fun resolveMaintenance(@Path("id") id: String): MaintenanceAlert

    @GET("admin/buildings") suspend fun buildings(): List<Building>
    @POST("admin/buildings") suspend fun createBuilding(@Body body: BuildingRequest): Building
    @POST("admin/buildings/{id}/units") suspend fun addUnit(@Path("id") id: String, @Body body: UnitRequest): Building
    @POST("admin/devices/batch") suspend fun registerBatch(@Body body: BatchDevicesRequest): BatchResult
    @POST("admin/buildings/{id}/common-alarm") suspend fun triggerCommonAlarm(@Path("id") id: String): Ack
    @GET("subscriptions/current") suspend fun subscription(): Subscription
    @PUT("subscriptions/current") suspend fun activatePlan(@Body body: PlanRequest): Subscription
    @GET("admin/audit") suspend fun audit(): List<AuditEntry>
}
