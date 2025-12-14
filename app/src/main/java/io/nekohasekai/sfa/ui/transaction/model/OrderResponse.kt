package io.nekohasekai.sfa.ui.transaction.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderResponse(
    val permId: Long,
    val order: Order,
    val contract: Contract,
    val orderState: OrderState
)

@Serializable
data class Order(
    val m_clientId: Int,
    val m_orderId: Int,
    val m_permId: Long,
    val m_action: String,
    val m_totalQuantity: Double,
    val m_orderType: String,
    val m_lmtPrice: Double,
    val m_tif: String,
    val m_account: String
)



@Serializable
data class OrderState(
    val m_status: String
)
