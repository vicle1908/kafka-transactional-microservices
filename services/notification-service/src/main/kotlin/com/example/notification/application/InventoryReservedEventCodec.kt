package com.example.notification.application

import com.example.events.avro.InventoryReservedEvent
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.specific.SpecificDatumWriter
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

object InventoryReservedEventCodec {
    private val reader = SpecificDatumReader(InventoryReservedEvent::class.java)
    private val writer = SpecificDatumWriter(InventoryReservedEvent::class.java)

    fun decode(json: String): InventoryReservedEvent {
        val decoder = DecoderFactory.get().jsonDecoder(InventoryReservedEvent.getClassSchema(), json)
        return reader.read(null, decoder)
    }

    fun encode(event: InventoryReservedEvent): String {
        val output = ByteArrayOutputStream()
        val encoder = EncoderFactory.get().jsonEncoder(InventoryReservedEvent.getClassSchema(), output)
        writer.write(event, encoder)
        encoder.flush()
        return output.toString(StandardCharsets.UTF_8)
    }
}
