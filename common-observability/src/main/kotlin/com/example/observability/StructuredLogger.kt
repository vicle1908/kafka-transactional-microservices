package com.example.observability

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Structured logging utility for consistent log formatting across microservices.
 * Provides methods to log events with consistent structure.
 */
class StructuredLogger(
    clazz: Class<*>,
) {
    private val logger: Logger = LoggerFactory.getLogger(clazz)

    /**
     * Log an INFO level message with structured data
     */
    fun info(
        message: String,
        vararg keyValuePairs: Pair<String, Any?>,
    ) {
        logger.info(formatMessage(message, *keyValuePairs))
    }

    /**
     * Log a WARN level message with structured data
     */
    fun warn(
        message: String,
        vararg keyValuePairs: Pair<String, Any?>,
    ) {
        logger.warn(formatMessage(message, *keyValuePairs))
    }

    /**
     * Log an ERROR level message with structured data
     */
    fun error(
        message: String,
        vararg keyValuePairs: Pair<String, Any?>,
    ) {
        logger.error(formatMessage(message, *keyValuePairs))
    }

    /**
     * Log a DEBUG level message with structured data
     */
    fun debug(
        message: String,
        vararg keyValuePairs: Pair<String, Any?>,
    ) {
        logger.debug(formatMessage(message, *keyValuePairs))
    }

    /**
     * Create a structured log entry with consistent fields
     */
    private fun formatMessage(
        message: String,
        vararg keyValuePairs: Pair<String, Any?>,
    ): String {
        val payload =
            buildJsonObject {
                put("timestamp", JsonPrimitive(Instant.now().toString()))
                put("message", JsonPrimitive(message))
                keyValuePairs.forEach { (key, value) ->
                    put(key, value.toJsonElement())
                }
            }

        return payload.toString()
    }

    companion object {
        /**
         * Factory method to create a StructuredLogger for a given class
         */
        fun getLogger(clazz: Class<*>): StructuredLogger = StructuredLogger(clazz)
    }

    private fun Any?.toJsonElement(): JsonElement =
        when (this) {
            null -> JsonNull
            is JsonElement -> this
            is String -> JsonPrimitive(this)
            is Number -> JsonPrimitive(this)
            is Boolean -> JsonPrimitive(this)
            is Instant -> JsonPrimitive(this.toString())
            is Enum<*> -> JsonPrimitive(this.name)
            is Iterable<*> -> this.toJsonArray()
            is Array<*> -> this.toJsonArray()
            is Map<*, *> -> this.toJsonObject()
            else -> JsonPrimitive(this.toString())
        }

    private fun Iterable<*>.toJsonArray(): JsonElement =
        buildJsonArray {
            forEach { add(it.toJsonElement()) }
        }

    private fun Array<*>.toJsonArray(): JsonElement =
        buildJsonArray {
            forEach { add(it.toJsonElement()) }
        }

    private fun Map<*, *>.toJsonObject(): JsonElement =
        buildJsonObject {
            forEach { (key, value) ->
                put(key?.toString() ?: "null", value.toJsonElement())
            }
        }
}
