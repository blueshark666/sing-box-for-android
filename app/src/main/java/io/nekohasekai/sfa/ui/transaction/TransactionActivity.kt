package io.nekohasekai.sfa.ui.transaction

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.databinding.ActivityTransactionBinding
import io.nekohasekai.sfa.ui.transaction.model.AccountInfoResponse
import io.nekohasekai.sfa.ui.transaction.model.PositionResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class TransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionBinding
    private val repository = TransactionRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 设置标题
        title = getString(R.string.transaction)
        
        // 发起网络请求获取账户信息和持仓数据
        fetchAccountInfo()
        fetchPositions()
    }

    private fun fetchPositions() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val positions = repository.fetchPositions()
                
                // 更新UI
                withContext(Dispatchers.Main) {
                    binding.positionsInfoCard.loadingText.visibility = View.GONE
                    binding.positionsInfoCard.positionsRecyclerView.visibility = View.VISIBLE
                    
                    // 设置RecyclerView
                    binding.positionsInfoCard.positionsRecyclerView.layoutManager = LinearLayoutManager(this@TransactionActivity)
                    binding.positionsInfoCard.positionsRecyclerView.adapter = PositionsAdapter(positions)
                }
            } catch (e: Exception) {
                Log.e("TransactionActivity", "Error fetching positions: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.positionsInfoCard.loadingText.text = "加载失败: ${e.message}"
                }
            }
        }
    }

    private fun fetchAccountInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val accountInfo = repository.fetchAccountInfo("DU9965361")
                
                // 更新UI
                withContext(Dispatchers.Main) {
                    binding.accountInfoCard.accountLoadingText.visibility = View.GONE
                    binding.accountInfoCard.accountInfoContainer.visibility = View.VISIBLE
                    
                    // 直接设置账户信息
                    binding.accountInfoCard.accountName.text = accountInfo.AccountOrGroup
                    binding.accountInfoCard.accountType.text = accountInfo.AccountType
                    binding.accountInfoCard.netLiquidation.text = accountInfo.NetLiquidation
                    binding.accountInfoCard.totalCashValue.text = accountInfo.TotalCashValue
                    binding.accountInfoCard.buyingPower.text = accountInfo.BuyingPower
                    binding.accountInfoCard.excessLiquidity.text = accountInfo.ExcessLiquidity
                    binding.accountInfoCard.availableFunds.text = accountInfo.AvailableFunds
                    binding.accountInfoCard.unrealizedPnL.text = accountInfo.UnrealizedPnL
                    binding.accountInfoCard.realizedPnL.text = accountInfo.RealizedPnL
                    binding.accountInfoCard.equityWithLoanValue.text = accountInfo.EquityWithLoanValue
                    binding.accountInfoCard.sma.text = accountInfo.SMA
                    binding.accountInfoCard.initMarginReq.text = accountInfo.InitMarginReq
                    binding.accountInfoCard.maintMarginReq.text = accountInfo.MaintMarginReq
                }
            } catch (e: Exception) {
                Log.e("TransactionActivity", "Error fetching account info: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.accountInfoCard.accountLoadingText.text = "账户信息加载失败: ${e.message}"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.close()
    }
}