package io.nekohasekai.sfa.ui.transaction.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class WatchlistResponse {
    @SerialName("conid")
    var conid: Long = 0L
    
    @SerialName("symbol")
    var symbol: String = ""
    
    @SerialName("secType")
    var secType: String = ""
    
    @SerialName("lastTradeDateOrContractMonth")
    var lastTradeDateOrContractMonth: String? = null
    
    // 嵌套的价格字段格式
    @SerialName("priceHolder")
    var priceHolder: PriceHolder = PriceHolder()
    
    // 扁平的价格字段格式（直接在顶层）
    @SerialName("bid")
    var bid: Double = 0.0
    
    @SerialName("ask")
    var ask: Double = 0.0
    
    // 辅助属性，用于获取实际的bid价格
    val actualBid: Double
        get() = if (priceHolder.bid != 0.0) priceHolder.bid else bid
    
    // 辅助属性，用于获取实际的ask价格
    val actualAsk: Double
        get() = if (priceHolder.ask != 0.0) priceHolder.ask else ask
    
    // 构造函数，允许创建实例时设置所有属性
    constructor(
        conid: Long = 0L,
        symbol: String = "",
        secType: String = "",
        lastTradeDateOrContractMonth: String? = null,
        priceHolder: PriceHolder = PriceHolder()
    ) {
        this.conid = conid
        this.symbol = symbol
        this.secType = secType
        this.lastTradeDateOrContractMonth = lastTradeDateOrContractMonth
        this.priceHolder = priceHolder
    }
    
    // 空构造函数，用于序列化
    constructor() {}
}

@Serializable
data class PriceHolder(
    @SerialName("bid")
    val bid: Double = 0.0,
    @SerialName("ask")
    val ask: Double = 0.0
)