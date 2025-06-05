package me.kowaulsky.auditium.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.kowaulsky.auditium.Auditium
import me.kowaulsky.auditium.dto.LogEntryDto
import me.kowaulsky.auditium.dto.WebLinkRequestDto
import me.kowaulsky.auditium.dto.WebLinkResponseDto
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.bukkit.Bukkit
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

/**
 * Handles communication with the external web panel.
 *
 * @param plugin The main Auditium plugin instance.
 */
class WebSender(private val plugin: Auditium) {

    private val httpClient: OkHttpClient = OkHttpClient()
    private val objectMapper: ObjectMapper = jacksonObjectMapper() // Kotlin-friendly Jackson mapper
    private var webPanelUrl: String = "http://192.168.23.46:3000" // Default, will be updated from config

    init {
        // Load web panel URL from config
        plugin.config.getString("webpanel.url")?.let {
            webPanelUrl = it.removeSuffix("/") // Ensure no trailing slash
        }
        if (webPanelUrl != plugin.config.getString("webpanel.url")) {
            plugin.logger.info("Web panel URL loaded from config: $webPanelUrl")
        } else {
            plugin.logger.info("Using default web panel URL: $webPanelUrl. Consider setting 'webpanel.url' in config.yml.")
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-f".toMediaType()
    }

    /**
     * Asynchronously sends a request to the web panel to generate a temporary log viewing link.
     *
     * @param payload The data containing users and logs.
     * @return A CompletableFuture that will complete with the WebLinkResponseDto or null on failure.
     */
    fun generateWebLink(payload: WebLinkRequestDto): CompletableFuture<WebLinkResponseDto?> {
        val future = CompletableFuture<WebLinkResponseDto?>()
        val requestJson = objectMapper.writeValueAsString(payload)
        val body = requestJson.toRequestBody(JSON)

        val request = Request.Builder()
            .url("$webPanelUrl/api/generate-link")
            .post(body)
            .build()

        plugin.logger.info("Attempting to generate web link. Payload size: ${requestJson.length} bytes")

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                plugin.logger.log(Level.SEVERE, "Failed to generate web link: ${e.message}", e)
                future.complete(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        plugin.logger.severe("Failed to generate web link. Server responded with ${it.code}: ${it.body?.string()}")
                        future.complete(WebLinkResponseDto(null, "Server error: ${it.code}"))
                        return
                    }
                    try {
                        val responseBody = it.body?.string()
                        if (responseBody != null) {
                            val webResponse = objectMapper.readValue(responseBody, WebLinkResponseDto::class.java)
                            plugin.logger.info("Web link generated successfully: ${webResponse.link}")
                            future.complete(webResponse)
                        } else {
                            plugin.logger.severe("Failed to generate web link: Empty response body.")
                            future.complete(WebLinkResponseDto(null, "Empty response from server."))
                        }
                    } catch (e: Exception) {
                        plugin.logger.log(Level.SEVERE, "Error parsing web link response: ${e.message}", e)
                        future.complete(WebLinkResponseDto(null, "Error parsing server response."))
                    }
                }
            }
        })
        return future
    }

    /**
     * Asynchronously sends a single log entry to the web panel for live updates.
     * This is a fire-and-forget operation, errors are logged but not explicitly returned.
     *
     * @param logEntry The log entry to send.
     */
    fun sendLiveLog(logEntry: LogEntryDto) {
        // Run asynchronously to avoid blocking the thread that called this (e.g., event listener)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                val requestJson = objectMapper.writeValueAsString(logEntry)
                val body = requestJson.toRequestBody(JSON)

                val request = Request.Builder()
                    .url("$webPanelUrl/api/logs") // POST to /api/logs for live updates
                    .post(body)
                    .build()

                // plugin.logger.fine("Sending live log: $requestJson") // Log only if fine logging is enabled

                httpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        plugin.logger.log(Level.WARNING, "Failed to send live log to web panel: ${e.message}", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (!it.isSuccessful) {
                                plugin.logger.warning("Failed to send live log. Server responded with ${it.code}: ${it.body?.string()}")
                            } else {
                                // plugin.logger.fine("Live log sent successfully.") // Log only if fine logging is enabled
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Error preparing live log for sending: ${e.message}", e)
            }
        })
    }
}
    