package io.nekohasekai.sfa.ui.transaction.model

import kotlinx.serialization.Serializable

@Serializable
data class TransactionResponse(
    val transactionId: String,
    val date: String,
    val time: String,
    val contract: Contract,
    val quantity: Double,
    val price: Double,
    val action: String, // BUY/SELL
    val orderType: String,
    val currency: String
)