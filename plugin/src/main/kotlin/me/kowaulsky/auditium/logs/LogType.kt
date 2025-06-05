package me.kowaulsky.auditium.logs

import java.util.UUID

interface LogType {
    val actionType: String // Maps to ENUM value in the logs table
    fun log(playerUUID: UUID, details: String)
    fun toJson(eventData: Map<String, Any>): String
    fun toSentence(jsonDetails: String): String
}