package com.example.inventory.application

import com.example.events.avro.PaymentCompletedEvent
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.specific.SpecificDatumWriter
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

object PaymentCompletedEventCodec {
    private val reader = SpecificDatumReader(PaymentCompletedEvent::class.java)
    private val writer = SpecificDatumWriter(PaymentCompletedEvent::class.java)

    fun decode(json: String): PaymentCompletedEvent {
        val decoder = DecoderFactory.get().jsonDecoder(PaymentCompletedEvent.getClassSchema(), json)
        return reader.read(null, decoder)
    }

    fun encode(event: PaymentCompletedEvent): String {
        val output = ByteArrayOutputStream()
        val encoder = EncoderFactory.get().jsonEncoder(PaymentCompletedEvent.getClassSchema(), output)
        writer.write(event, encoder)
        encoder.flush()
        return output.toString(StandardCharsets.UTF_8)
    }
}
