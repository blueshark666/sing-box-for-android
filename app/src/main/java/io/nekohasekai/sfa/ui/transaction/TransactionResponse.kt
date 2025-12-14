package io.nekohasekai.sfa.ui.transaction

import kotlinx.serialization.Serializable

@Serializable
data class Contract(
    val m_conId: Int,
    val m_symbol: String,
    val m_secType: String,
    val m_currency: String
)

@Serializable
data class TransactionResponse(
    val transactionId: Int,
    val date: String,
    val time: String,
    val contract: Contract,
    val quantity: Int,
    val price: Double
)
