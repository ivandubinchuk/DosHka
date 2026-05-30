package com.example.doshka.data.remote.api

import com.example.doshka.data.remote.dto.*
import kotlinx.serialization.SerialName
import retrofit2.Response
import retrofit2.http.*

/**
 * Головний API інтерфейс для сервера DoshKa
 */
interface DoshkaApi {

    // ==================== АУТЕНТИФІКАЦІЯ ====================

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<TokenResponse>

    @POST("api/v1/auth/register/invite")
    suspend fun registerWithInvite(@Body request: RegisterRequest): Response<TokenResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<TokenResponse>

    @POST("api/v1/auth/password-reset")
    suspend fun requestPasswordReset(@Body request: PasswordResetRequest): Response<Unit>

    @POST("api/v1/auth/password-change")
    suspend fun changePassword(@Body request: PasswordChangeRequest): Response<Unit>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    // ==================== КОРИСТУВАЧІ ====================
    @POST("/api/v1/teams/add-by-email")
    suspend fun addExecutor(@Query("email") email: String)

    @GET("api/v1/users/me")
    suspend fun getCurrentUser(): Response<UserDto>

    @PUT("api/v1/users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserDto>

    @GET("api/v1/users/{userId}")
    suspend fun getUserById(@Path("userId") userId: String): Response<UserDto>

    // ==================== КОМАНДИ ====================

    @GET("api/v1/teams/{teamId}")
    suspend fun getTeam(@Path("teamId") teamId: String): Response<TeamDto>

    @POST("api/v1/teams")
    suspend fun createTeam(@Body request: CreateTeamRequest): Response<TeamDto>

    @PUT("api/v1/teams/{teamId}")
    suspend fun updateTeam(
        @Path("teamId") teamId: String,
        @Body request: CreateTeamRequest
    ): Response<TeamDto>

    @GET("api/v1/teams/{teamId}/members")
    suspend fun getTeamMembers(@Path("teamId") teamId: String): Response<List<UserDto>>

    @POST("api/v1/teams/{teamId}/invite/qr")
    suspend fun generateInviteQR(@Path("teamId") teamId: String): Response<InviteQRResponse>

    // ==================== ДОШКИ ====================

    @GET("api/v1/boards/{boardId}")
    suspend fun getBoard(@Path("boardId") boardId: String): Response<BoardDto>

    @GET("api/v1/boards")
    suspend fun getBoardsByTeam(@Query("team_id") teamId: String): Response<List<BoardDto>>

    @POST("api/v1/boards")
    suspend fun createBoard(@Body request: CreateBoardRequest): Response<BoardDto>

    @PUT("api/v1/boards/{boardId}")
    suspend fun updateBoard(
        @Path("boardId") boardId: String,
        @Body request: CreateBoardRequest
    ): Response<BoardDto>

    @DELETE("api/v1/boards/{boardId}")
    suspend fun deleteBoard(@Path("boardId") boardId: String): Response<Unit>

    // ==================== КОЛОНКИ ====================

    @GET("api/v1/boards/{boardId}/columns")
    suspend fun getColumnsByBoard(@Path("boardId") boardId: String): Response<List<ColumnDto>>

    @POST("api/v1/boards/columns")
    suspend fun createColumn(@Body request: CreateColumnRequest): Response<ColumnDto>

    @PUT("api/v1/boards/columns/{columnId}")
    suspend fun updateColumn(
        @Path("columnId") columnId: String,
        @Body request: CreateColumnRequest
    ): Response<ColumnDto>

    @DELETE("api/v1/boards/columns/{columnId}")
    suspend fun deleteColumn(@Path("columnId") columnId: String): Response<Unit>

    // ==================== ЗАДАЧІ ====================

    @GET("api/v1/tasks/{taskId}")
    suspend fun getTask(@Path("taskId") taskId: String): Response<TaskDto>

    @GET("api/v1/tasks")
    suspend fun getTasksByColumn(@Query("column_id") columnId: String): Response<List<TaskDto>>

    @GET("api/v1/tasks/me/tasks")
    suspend fun getMyTasks(): Response<List<TaskDto>>

    @POST("api/v1/tasks")
    suspend fun createTask(@Body request: CreateTaskRequest): Response<TaskDto>

    @PUT("api/v1/tasks/{taskId}")
    suspend fun updateTask(
        @Path("taskId") taskId: String,
        @Body request: UpdateTaskRequest
    ): Response<TaskDto>

    @POST("api/v1/tasks/{taskId}/move")
    suspend fun moveTask(
        @Path("taskId") taskId: String,
        @Body request: MoveTaskRequest
    ): Response<TaskDto>

    @DELETE("api/v1/tasks/{taskId}")
    suspend fun deleteTask(@Path("taskId") taskId: String): Response<Unit>

    @GET("api/v1/tasks/export/csv")
    suspend fun exportTasksCsv(@Query("team_id") teamId: String): Response<String>

    // ==================== МІТКИ ====================

    @GET("api/v1/tasks/labels")
    suspend fun getLabels(@Query("team_id") teamId: String): Response<List<LabelDto>>

    @POST("api/v1/tasks/labels")
    suspend fun createLabel(@Body request: CreateLabelRequest): Response<LabelDto>

    @DELETE("api/v1/tasks/labels/{labelId}")
    suspend fun deleteLabel(@Path("labelId") labelId: String): Response<Unit>

    // ==================== ПОВІДОМЛЕННЯ ====================

    @GET("api/v1/messages/{taskId}")
    suspend fun getMessages(@Path("taskId") taskId: String): Response<List<MessageDto>>

    @POST("api/v1/messages")
    suspend fun sendMessage(@Body request: SendMessageRequest): Response<MessageDto>

    @POST("api/v1/messages/{taskId}/read")
    suspend fun markMessagesAsRead(@Path("taskId") taskId: String): Response<Unit>

    // ==================== ВКЛАДЕННЯ ====================

    @GET("api/v1/attachments/{taskId}")
    suspend fun getAttachments(@Path("taskId") taskId: String): Response<List<AttachmentDto>>

    @Multipart
    @POST("api/v1/attachments")
    suspend fun uploadAttachment(
        @Part("task_id") taskId: okhttp3.RequestBody,
        @Part file: okhttp3.MultipartBody.Part
    ): Response<AttachmentDto>

    @DELETE("api/v1/attachments/{attachmentId}")
    suspend fun deleteAttachment(@Path("attachmentId") attachmentId: String): Response<Unit>

    // ==================== АНАЛІТИКА ====================

    @GET("api/v1/analytics/dashboard")
    suspend fun getDashboardStats(@Query("team_id") teamId: String): Response<DashboardStatsDto>

    @GET("api/v1/analytics/velocity")
    suspend fun getVelocity(
        @Query("team_id") teamId: String,
        @Query("weeks") weeks: Int = 8
    ): Response<List<VelocityDto>>

    @GET("api/v1/analytics/cycle-time")
    suspend fun getCycleTime(@Query("team_id") teamId: String): Response<List<CycleTimeDto>>

    @GET("api/v1/analytics/workload")
    suspend fun getWorkload(@Query("team_id") teamId: String): Response<List<WorkloadDto>>

    @GET("api/v1/analytics/efficiency")
    suspend fun getEfficiency(@Query("team_id") teamId: String): Response<EfficiencyDto>
}

/**
 * Відповідь з QR-кодом запрошення
 */
@kotlinx.serialization.Serializable
data class InviteQRResponse(
    @SerialName("qr_code")
    val qrCode: String, // Base64 зображення
    @SerialName("invite_token")
    val inviteToken: String,
    @SerialName("expires_at")
    val expiresAt: String
)

/**
 * DTO статистики дашборду
 */
@kotlinx.serialization.Serializable
data class DashboardStatsDto(
    @SerialName("total_tasks")
    val totalTasks: Int,
    @SerialName("completed_tasks")
    val completedTasks: Int,
    @SerialName("in_progress_tasks")
    val inProgressTasks: Int,
    @SerialName("overdue_tasks")
    val overdueTasks: Int,
    @SerialName("completion_rate")
    val completionRate: Float,
    @SerialName("average_cycle_time")
    val averageCycleTime: Long?
)

/**
 * DTO velocity
 */
@kotlinx.serialization.Serializable
data class VelocityDto(
    @SerialName("week_number")
    val weekNumber: Int,
    val year: Int,
    @SerialName("completed_tasks")
    val completedTasks: Int,
    @SerialName("start_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String
)

/**
 * DTO cycle time
 */
@kotlinx.serialization.Serializable
data class CycleTimeDto(
    @SerialName("user_id")
    val userId: String,
    @SerialName("user_name")
    val userName: String,
    @SerialName("average_cycle_time_hours")
    val averageCycleTimeHours: Float,
    @SerialName("tasks_completed")
    val tasksCompleted: Int
)

/**
 * DTO навантаження виконавців
 */
@kotlinx.serialization.Serializable
data class WorkloadDto(
    @SerialName("user_id")
    val userId: String,
    @SerialName("user_name")
    val userName: String,
    @SerialName("active_tasks")
    val activeTasks: Int,
    @SerialName("completed_tasks")
    val completedTasks: Int,
    @SerialName("overdue_tasks")
    val overdueTasks: Int
)

/**
 * DTO розподілу по статусах
 */
@kotlinx.serialization.Serializable
data class StatusDistributionDto(
    @SerialName("column_name")
    val columnName: String,
    @SerialName("task_count")
    val taskCount: Int
)

/**
 * DTO ефективності команди
 */
@kotlinx.serialization.Serializable
data class EfficiencyDto(
    @SerialName("on_time_completion_rate")
    val onTimeCompletionRate: Float,
    @SerialName("on_time_tasks")
    val onTimeTasks: Int,
    @SerialName("total_with_deadline")
    val totalWithDeadline: Int,
    @SerialName("estimation_accuracy")
    val estimationAccuracy: Float?,
    @SerialName("total_estimated_hours")
    val totalEstimatedHours: Float,
    @SerialName("total_actual_hours")
    val totalActualHours: Float,
    @SerialName("tasks_with_estimation")
    val tasksWithEstimation: Int,
    @SerialName("status_distribution")
    val statusDistribution: List<StatusDistributionDto>
)
