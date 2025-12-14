package io.nekohasekai.sfa.ui.transaction.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PositionResponse(
    val contract: Contract,
    val quantity: Double,
    val avgPrice: Double
)

@Serializable
data class Contract(
    val m_conid: Long,
    val m_symbol: String,
    val m_secType: String,
    val m_lastTradeDateOrContractMonth: String,
    val m_strike: Double,
    val m_right: String?,
    val m_multiplier: String,
    val m_exchange: String?,
    val m_primaryExch: String?,
    val m_currency: String,
    val m_localSymbol: String,
    val m_tradingClass: String,
    val m_secIdType: String?,
    val m_secId: String?,
    val m_deltaNeutralContract: String?,
    val m_includeExpired: Boolean,
    val m_comboLegsDescrip: String?,
    val m_comboLegs: JsonElement,
    val combo: Boolean,
    val right: String?,
    val secType: String,
    val secIdType: String?
)