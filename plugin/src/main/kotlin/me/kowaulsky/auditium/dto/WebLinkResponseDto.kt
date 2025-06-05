package me.kowaulsky.auditium.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Data Transfer Object for the response received after requesting a web link.
 * This structure will be deserialized from the JSON response from the web panel's /api/generate-link endpoint.
 *
 * @property link The temporary URL to access the logs.
 * @property error An optional error message if link generation failed.
 */
data class WebLinkResponseDto(
    @JsonProperty("link") val link: String?,
    @JsonProperty("error") val error: String?
)
    