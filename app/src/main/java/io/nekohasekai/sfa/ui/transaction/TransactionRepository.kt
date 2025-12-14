package io.nekohasekai.sfa.ui.transaction

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import io.nekohasekai.sfa.ui.transaction.model.AccountInfoResponse
import io.nekohasekai.sfa.ui.transaction.model.PositionResponse
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
        return jsonParser.decodeFromString<List<PositionResponse>>(jsonString)
    }

    suspend fun fetchAccountInfo(accountId: String): AccountInfoResponse {
        val response = client.get("http://10.147.20.245:4080/ibkr/account/$accountId")
        val jsonString = response.bodyAsText()
        Log.d("TransactionRepository", "Account API Response: $jsonString")
        return jsonParser.decodeFromString<AccountInfoResponse>(jsonString)
    }

    fun close() {
        client.close()
    }
}