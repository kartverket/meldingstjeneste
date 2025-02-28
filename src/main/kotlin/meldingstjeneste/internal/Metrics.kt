package meldingstjeneste.internal

import io.micrometer.core.instrument.Counter
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

object Metrics {
    lateinit var meldingerCounter: Counter

    fun init(registry: PrometheusMeterRegistry) {
        meldingerCounter = registry.counter("meldinger_total_sent")
    }
}
