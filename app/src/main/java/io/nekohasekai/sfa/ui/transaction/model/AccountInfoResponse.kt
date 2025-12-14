package io.nekohasekai.sfa.ui.transaction.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountInfoResponse(
    val AccountOrGroup: String,
    val AccountType: String,
    val NetLiquidation: String,
    val TotalCashValue: String,
    val BuyingPower: String,
    val ExcessLiquidity: String,
    val AvailableFunds: String,
    val UnrealizedPnL: String,
    val RealizedPnL: String,
    val EquityWithLoanValue: String,
    val SMA: String,
    val InitMarginReq: String,
    val MaintMarginReq: String
)