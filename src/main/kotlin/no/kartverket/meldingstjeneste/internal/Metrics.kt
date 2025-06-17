package no.kartverket.meldingstjeneste.internal

import io.micrometer.core.instrument.Counter
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

object Metrics {
    lateinit var antallOrdreBestilt: Counter


    fun init(registry: PrometheusMeterRegistry) {
        antallOrdreBestilt = registry.counter("number_of_orders_requested")
    }
}
