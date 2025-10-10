package com.example.events

import org.apache.avro.Schema
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class SchemaContractTest {
    private fun loadSchema(name: String): Schema {
        val path = Paths.get("src/main/avro/$name")
        val raw = Files.readString(path)
        return Schema.Parser().parse(raw)
    }

    @Test
    fun `order created schema contains expected fields`() {
        val schema = loadSchema("order_created_event.avsc")
        assertEquals("OrderCreatedEvent", schema.name)
        assertUuidField(schema, "event_id")
        assertUuidField(schema, "aggregate_id")
        assertTimestampField(schema, "occurred_at")
        assertStringField(schema, "payload")
    }

    @Test
    fun `payment completed schema contains expected fields`() {
        val schema = loadSchema("payment_completed_event.avsc")
        assertEquals("PaymentCompletedEvent", schema.name)
        assertUuidField(schema, "event_id")
        assertUuidField(schema, "aggregate_id")
        assertTimestampField(schema, "occurred_at")
        assertStringField(schema, "payload")
    }

    @Test
    fun `payment failed schema contains expected fields`() {
        val schema = loadSchema("payment_failed_event.avsc")
        assertEquals("PaymentFailedEvent", schema.name)
        assertUuidField(schema, "event_id")
        assertUuidField(schema, "aggregate_id")
        assertTimestampField(schema, "occurred_at")
        assertStringField(schema, "payload")
    }

    @Test
    fun `payment refunded schema contains expected fields`() {
        val schema = loadSchema("payment_refunded_event.avsc")
        assertEquals("PaymentRefundedEvent", schema.name)
        assertUuidField(schema, "event_id")
        assertUuidField(schema, "aggregate_id")
        assertTimestampField(schema, "occurred_at")
        assertStringField(schema, "payload")
    }

    @Test
    fun `payment refund failed schema contains expected fields`() {
        val schema = loadSchema("payment_refund_failed_event.avsc")
        assertEquals("PaymentRefundFailedEvent", schema.name)
        assertUuidField(schema, "event_id")
        assertUuidField(schema, "aggregate_id")
        assertTimestampField(schema, "occurred_at")
        assertStringField(schema, "payload")
    }

    @Test
    fun `inventory reserved schema contains expected fields`() {
        val schema = loadSchema("inventory_reserved_event.avsc")
        assertEquals("InventoryReservedEvent", schema.name)
        assertUuidField(schema, "event_id")
        assertUuidField(schema, "aggregate_id")
        assertTimestampField(schema, "occurred_at")
        assertStringField(schema, "payload")
    }

    @Test
    fun `notification sent schema contains expected fields`() {
        val schema = loadSchema("notification_sent_event.avsc")
        assertEquals("NotificationSentEvent", schema.name)
        assertUuidField(schema, "event_id")
        assertUuidField(schema, "aggregate_id")
        assertTimestampField(schema, "occurred_at")
        assertStringField(schema, "payload")
    }

    private fun assertUuidField(
        schema: Schema,
        fieldName: String,
    ) {
        val field = schema.getField(fieldName)
        assertNotNull(
            field,
            "Expected field '$fieldName'",
        )
        assertEquals("uuid", field!!.schema().logicalType?.name)
    }

    private fun assertTimestampField(
        schema: Schema,
        fieldName: String,
    ) {
        val field = schema.getField(fieldName)
        assertNotNull(
            field,
            "Expected field '$fieldName'",
        )
        assertEquals("timestamp-millis", field!!.schema().logicalType?.name)
    }

    private fun assertStringField(
        schema: Schema,
        fieldName: String,
    ) {
        val field = schema.getField(fieldName)
        assertNotNull(
            field,
            "Expected field '$fieldName'",
        )
        val fieldSchema = field!!.schema()
        assertTrue(
            fieldSchema.type == Schema.Type.STRING || fieldSchema.type == Schema.Type.UNION,
            "Expected '$fieldName' to be a string or nullable string",
        )
    }
}
