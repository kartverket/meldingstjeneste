package no.kartverket.meldingstjeneste.model
import kotlinx.serialization.Serializable

@Serializable
data class Jwk(
    val kty: String,
    val n: String,
    val e: String,
    val d: String,
    val p: String?,
    val q: String?,
    val dp: String?,
    val dq: String?,
    val qi: String?,
    val alg: String,
    val kid: String,
    val use: String,
)
