package com.juanmaav.platform.logger

import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class JsonStructuredLogger(
    private val serviceName: String,
    private val traceProvider: TraceProvider? = null,
) : StructuredLogger {
    private val slf4j = LoggerFactory.getLogger(serviceName)
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    override fun fatal(
        step: String,
        message: String,
        attributes: Map<String, Any?>,
    ) {
        if (!slf4j.isErrorEnabled) return
        slf4j.error(buildJson("FATAL", step, message, attributes))
    }

    override fun error(
        step: String,
        message: String,
        error: Throwable?,
        attributes: Map<String, Any?>,
    ) {
        if (!slf4j.isErrorEnabled) return
        val attrs =
            if (error != null) {
                attributes +
                    mapOf(
                        "error_type" to error::class.simpleName,
                        "error_message" to error.message,
                    )
            } else {
                attributes
            }
        slf4j.error(buildJson("ERROR", step, message, attrs))
    }

    override fun warn(
        step: String,
        message: String,
        attributes: Map<String, Any?>,
    ) {
        if (!slf4j.isWarnEnabled) return
        slf4j.warn(buildJson("WARN", step, message, attributes))
    }

    override fun info(
        step: String,
        message: String,
        attributes: Map<String, Any?>,
    ) {
        if (!slf4j.isInfoEnabled) return
        slf4j.info(buildJson("INFO", step, message, attributes))
    }

    override fun debug(
        step: String,
        message: String,
        attributes: Map<String, Any?>,
    ) {
        if (!slf4j.isDebugEnabled) return
        slf4j.debug(buildJson("DEBUG", step, message, attributes))
    }

    private fun buildJson(
        level: String,
        step: String,
        message: String,
        attributes: Map<String, Any?>,
    ): String {
        val sb = StringBuilder(256)
        sb.append("{")
        sb.appendField("time", OffsetDateTime.now(ZoneOffset.UTC).format(formatter))
        sb.append(",").appendField("level", level)
        sb.append(",").appendField("service", serviceName)
        sb.append(",").appendField("step", step)
        sb.append(",").appendField("message", message)

        val trace = traceProvider?.currentTrace()
        if (trace != null) {
            sb.append(",").appendField("trace_id", trace.traceId)
            sb.append(",").appendField("span_id", trace.spanId)
        }

        // Promote traceId from attributes to root level if not already set by TraceProvider
        val traceIdFromAttr = attributes["traceId"]
        if (trace == null && traceIdFromAttr != null) {
            sb.append(",").appendField("trace_id", traceIdFromAttr)
        }

        // All extra fields are flat at root — consistent with Go slog output for Grafana/DD indexing
        val remainingAttrs = attributes.filterKeys { it != "traceId" }
        for ((key, value) in remainingAttrs) {
            sb.append(",").appendField(key, value)
        }

        sb.append("}")
        return sb.toString()
    }

    private fun StringBuilder.appendField(
        key: String,
        value: Any?,
    ): StringBuilder {
        append("\"").append(escapeJson(key)).append("\":")
        when (value) {
            null -> append("null")
            is String -> append("\"").append(escapeJson(value)).append("\"")
            is Number, is Boolean -> append(value)
            else -> append("\"").append(escapeJson(value.toString())).append("\"")
        }
        return this
    }

    private fun escapeJson(text: String): String {
        if (text.none { it == '"' || it == '\\' || it == '\n' || it == '\r' || it == '\t' }) return text
        return buildString(text.length + 16) {
            for (ch in text) {
                when (ch) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }
    }
}
