package io.cortex.terminal.data.api

import io.cortex.terminal.data.model.CommandRequest
import io.cortex.terminal.data.model.CommandResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AIService {
    @POST("v1/command/suggest")
    suspend fun suggestCommand(@Body request: CommandRequest): Response<CommandResponse>
}
