package io.nekohasekai.sfa.ui.transaction.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchlistResponse(
    val conid: Long,
    val symbol: String,
    val secType: String,
    @SerialName("lastTradeDateOrContractMonth")
    val lastTradeDateOrContractMonth: String? = null,
    @SerialName("priceHolder")
    val priceHolder: PriceHolder = PriceHolder(0.0, 0.0)
)

@Serializable
data class PriceHolder(
    @SerialName("bid")
    val bid: Double = 0.0,
    @SerialName("ask")
    val ask: Double = 0.0
)