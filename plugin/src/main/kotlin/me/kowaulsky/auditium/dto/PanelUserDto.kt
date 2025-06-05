package me.kowaulsky.auditium.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Data Transfer Object for panel user information.
 * Used for serializing user data to be sent to the web panel.
 *
 * @property username The username of the panel user.
 * @property passwordHash The hashed password of the panel user.
 */
data class PanelUserDto(
    @JsonProperty("username") val username: String,
    @JsonProperty("password_hash") val passwordHash: String // Ensure this matches the JSON property name expected by your Node.js server
)
    