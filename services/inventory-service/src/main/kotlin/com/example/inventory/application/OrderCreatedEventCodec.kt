package com.example.inventory.application

import com.example.events.avro.OrderCreatedEvent
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader

object OrderCreatedEventCodec {
    private val reader = SpecificDatumReader(OrderCreatedEvent::class.java)

    fun decode(json: String): OrderCreatedEvent {
        val decoder = DecoderFactory.get().jsonDecoder(OrderCreatedEvent.getClassSchema(), json)
        return reader.read(null, decoder)
    }
}
