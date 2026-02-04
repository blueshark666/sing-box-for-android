package io.nekohasekai.sfa.ui.transaction

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import io.nekohasekai.sfa.ui.transaction.model.AccountInfoResponse
import io.nekohasekai.sfa.ui.transaction.model.PositionResponse
import io.nekohasekai.sfa.ui.transaction.model.OrderResponse
import io.nekohasekai.sfa.ui.transaction.model.WatchlistResponse
import kotlinx.serialization.json.Json


class TransactionRepository {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    private val jsonParser = Json {
        ignoreUnknownKeys = true
    }

    suspend fun fetchPositions(): List<PositionResponse> {
        val response = client.get("http://10.147.20.245:4080/ibkr/positions")
        val jsonString = response.bodyAsText()
        Log.d("TransactionRepository", "API Response (Positions): $jsonString")
        
        // 添加调试日志，检查解析后的数据
        val positions = jsonParser.decodeFromString<List<PositionResponse>>(jsonString)
        Log.d("TransactionRepository", "Parsed positions: ${positions.size} items")
        for ((index, position) in positions.withIndex()) {
            Log.d("TransactionRepository", "Position $index: symbol=${position.contract.m_symbol}, quantity=${position.quantity}, avgPrice=${position.avgPrice}")
        }
        
        return positions
    }

    suspend fun fetchAccountInfo(accountId: String): AccountInfoResponse {
        val response = client.get("http://10.147.20.245:4080/ibkr/account/$accountId")
        val jsonString = response.bodyAsText()
        Log.d("TransactionRepository", "Account API Response: $jsonString")
        val accountInfo = jsonParser.decodeFromString<AccountInfoResponse>(jsonString)
        Log.d("TransactionRepository", "Parsed account info: $accountInfo")
        return accountInfo
    }

    suspend fun fetchOrders(): List<OrderResponse> {
        val response = client.get("http://10.147.20.245:4080/ibkr/orders")
        val jsonString = response.bodyAsText()
        Log.d("TransactionRepository", "Orders API Response: $jsonString")
        
        // 添加调试日志，检查解析后的数据
        val orders = jsonParser.decodeFromString<List<OrderResponse>>(jsonString)
        Log.d("TransactionRepository", "Parsed orders: ${orders.size} items")
        for ((index, order) in orders.withIndex()) {
            Log.d("TransactionRepository", "Order $index: symbol=${order.contract.m_symbol}, action=${order.order.m_action}, status=${order.orderState.m_status}, price=${order.order.m_lmtPrice}")
        }
        
        return orders
    }
    
    suspend fun fetchWatchlist(): List<WatchlistResponse> {
        val response = client.get("http://10.147.20.245:4080/ibkr/watchlist/contracts/lite")
        val jsonString = response.bodyAsText()
        
        // 打印完整的API响应，以便检查格式
        Log.d("TransactionRepository", "Full Watchlist API Response: $jsonString")
        
        // 添加调试日志，检查API响应的前500个字符
        Log.d("TransactionRepository", "Watchlist API Response (first 500 chars): ${jsonString.take(500)}")
        
        // 尝试解析数据
        return try {
            // 尝试解析为当前的数据模型格式
            val watchlist = jsonParser.decodeFromString<List<WatchlistResponse>>(jsonString)
            Log.d("TransactionRepository", "Parsed watchlist: ${watchlist.size} items")
            
            // 检查解析结果
            for ((index, item) in watchlist.withIndex()) {
                Log.d("TransactionRepository", "Watchlist item $index: symbol=${item.symbol}, secType=${item.secType}, bid=${item.priceHolder.bid}, ask=${item.priceHolder.ask}")
                
                // 检查priceHolder字段是否为默认值
                if (item.priceHolder.bid == 0.0 && item.priceHolder.ask == 0.0) {
                    Log.w("TransactionRepository", "Watchlist item $index has default price values, may indicate parsing issue")
                }
            }
            
            watchlist
        } catch (e: Exception) {
            Log.e("TransactionRepository", "Error parsing watchlist: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun submitOrder(conid: Long, action: String, quantity: Int, price: Double): Boolean {
        try {
            val url = "http://10.147.20.245:4080/ibkr/order?conid=$conid&action=$action&quantity=$quantity&price=$price"
            Log.d("TransactionRepository", "Submitting order to URL: $url")
            
            val response: HttpResponse = client.post {
                url(url)
            }
            
            Log.d("TransactionRepository", "Order response status: ${response.status}")
            
            // 返回200即算成功
            return response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            Log.e("TransactionRepository", "Error submitting order: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun close() {
        client.close()
    }
}