package com.example.payments.application

import com.example.events.avro.OrderCreatedEvent
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.specific.SpecificDatumWriter
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

object OrderCreatedEventCodec {
    private val reader = SpecificDatumReader(OrderCreatedEvent::class.java)
    private val writer = SpecificDatumWriter(OrderCreatedEvent::class.java)

    fun decode(json: String): OrderCreatedEvent {
        val decoder = DecoderFactory.get().jsonDecoder(OrderCreatedEvent.getClassSchema(), json)
        return reader.read(null, decoder)
    }

    fun encode(event: OrderCreatedEvent): String {
        val output = ByteArrayOutputStream()
        val encoder = EncoderFactory.get().jsonEncoder(OrderCreatedEvent.getClassSchema(), output)
        writer.write(event, encoder)
        encoder.flush()
        return output.toString(StandardCharsets.UTF_8)
    }
}
