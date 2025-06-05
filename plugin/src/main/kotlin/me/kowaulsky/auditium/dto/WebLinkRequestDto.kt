package me.kowaulsky.auditium.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Data Transfer Object for the request to generate a web link.
 * This structure will be serialized to JSON and sent to the web panel's /api/generate-link endpoint.
 *
 * @property users A list of panel users with their credentials.
 * @property logs A list of log entries to be displayed.
 */
data class WebLinkRequestDto(
    @JsonProperty("users") val users: List<PanelUserDto>,
    @JsonProperty("logs") val logs: List<LogEntryDto>
)
    