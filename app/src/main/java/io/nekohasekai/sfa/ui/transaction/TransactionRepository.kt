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
        Log.d("TransactionRepository", "API Response: $jsonString")
        
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
        return jsonParser.decodeFromString<AccountInfoResponse>(jsonString)
    }

    suspend fun fetchOrders(): List<OrderResponse> {
        val response = client.get("http://10.147.20.245:4080/ibkr/orders")
        val jsonString = response.bodyAsText()
        Log.d("TransactionRepository", "Orders API Response: $jsonString")
        
        // 添加调试日志，检查解析后的数据
        val orders = jsonParser.decodeFromString<List<OrderResponse>>(jsonString)
        Log.d("TransactionRepository", "Parsed orders: ${orders.size} items")
        for ((index, order) in orders.withIndex()) {
            Log.d("TransactionRepository", "Order $index: action=${order.order.m_action}, symbol=${order.contract.m_symbol}, status=${order.orderState.m_status}")
        }
        
        return orders
    }

    suspend fun submitOrder(conid: Int, action: String, quantity: Int, price: Double): Boolean {
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