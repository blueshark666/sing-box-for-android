package io.nekohasekai.sfa.ui.transaction

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
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
import io.nekohasekai.sfa.ui.transaction.OrdersAdapter
import io.nekohasekai.sfa.ui.transaction.model.OrderResponse
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
        
        // 设置提交订单按钮点击事件
        binding.transactionsInfoCard.buttonSubmitOrder.setOnClickListener {
            showSubmitOrderDialog()
        }
        
        // 发起网络请求获取账户信息、持仓数据和交易记录
        fetchAccountInfo()
        fetchPositions()
        fetchOrders()
    }

    private fun fetchPositions() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val positions = repository.fetchPositions()
                Log.d("TransactionActivity", "Fetched ${positions.size} positions")
                
                // 更新UI
                withContext(Dispatchers.Main) {
                    Log.d("TransactionActivity", "Updating UI with ${positions.size} positions")
                    binding.positionsInfoCard.loadingText.visibility = View.GONE
                    binding.positionsInfoCard.positionsHeader.visibility = View.VISIBLE
                    binding.positionsInfoCard.positionsRecyclerView.visibility = View.VISIBLE
                    
                    // 设置RecyclerView
                    binding.positionsInfoCard.positionsRecyclerView.layoutManager = LinearLayoutManager(this@TransactionActivity)
                    binding.positionsInfoCard.positionsRecyclerView.adapter = PositionsAdapter(positions) {
                        showPositionDetailsDialog(it)
                    }
                    Log.d("TransactionActivity", "RecyclerView adapter set with ${positions.size} positions")
                }
            } catch (e: Exception) {
                Log.e("TransactionActivity", "Error fetching positions: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.positionsInfoCard.loadingText.text = "加载失败: ${e.message}"
                    binding.positionsInfoCard.loadingText.visibility = View.VISIBLE
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
                    binding.accountInfoCard.totalCashValue.text = accountInfo.TotalCashValue
                    binding.accountInfoCard.unrealizedPnL.text = accountInfo.UnrealizedPnL
                    binding.accountInfoCard.maintMarginReq.text = accountInfo.MaintMarginReq
                    
                    // 设置详情按钮点击事件
                    binding.accountInfoCard.detailButton.setOnClickListener {
                        showAccountDetailsDialog(accountInfo)
                    }
                }
            } catch (e: Exception) {
                Log.e("TransactionActivity", "Error fetching account info: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.accountInfoCard.accountLoadingText.text = "账户信息加载失败: ${e.message}"
                }
            }
        }
    }
    
    private fun showAccountDetailsDialog(accountInfo: AccountInfoResponse) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_account_details)
        dialog.setTitle("账户详情")
        dialog.setCancelable(true)
        
        // 设置对话框大小
        val window = dialog.window
        window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            (resources.displayMetrics.heightPixels * 0.8).toInt()
        )
        
        // 填充所有账户信息
        dialog.findViewById<TextView>(R.id.dialogAccountName)?.text = accountInfo.AccountOrGroup
        dialog.findViewById<TextView>(R.id.dialogAccountType)?.text = accountInfo.AccountType
        // AccountID字段在AccountInfoResponse中不存在，注释掉
        // dialog.findViewById<TextView>(R.id.dialogAccountId)?.text = accountInfo.AccountID
        // AccountStatus字段在AccountInfoResponse中不存在，注释掉
        // dialog.findViewById<TextView>(R.id.dialogAccountStatus)?.text = accountInfo.AccountStatus
        dialog.findViewById<TextView>(R.id.dialogAvailableFunds)?.text = accountInfo.AvailableFunds
        // TotalEquity字段在AccountInfoResponse中不存在，注释掉
        // dialog.findViewById<TextView>(R.id.dialogTotalEquity)?.text = accountInfo.TotalEquity
        dialog.findViewById<TextView>(R.id.dialogNetLiquidation)?.text = accountInfo.NetLiquidation
        dialog.findViewById<TextView>(R.id.dialogTotalCashValue)?.text = accountInfo.TotalCashValue
        dialog.findViewById<TextView>(R.id.dialogBuyingPower)?.text = accountInfo.BuyingPower
        dialog.findViewById<TextView>(R.id.dialogExcessLiquidity)?.text = accountInfo.ExcessLiquidity
        dialog.findViewById<TextView>(R.id.dialogRealizedPnL)?.text = accountInfo.RealizedPnL
        dialog.findViewById<TextView>(R.id.dialogUnrealizedPnL)?.text = accountInfo.UnrealizedPnL
        dialog.findViewById<TextView>(R.id.dialogEquityWithLoanValue)?.text = accountInfo.EquityWithLoanValue
        dialog.findViewById<TextView>(R.id.dialogSma)?.text = accountInfo.SMA
        dialog.findViewById<TextView>(R.id.dialogInitMarginReq)?.text = accountInfo.InitMarginReq
        dialog.findViewById<TextView>(R.id.dialogMaintMarginReq)?.text = accountInfo.MaintMarginReq
        
        // 显示对话框
        dialog.show()
    }

    private fun showPositionDetailsDialog(position: PositionResponse) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_position_details)

        // 设置弹窗宽度为屏幕宽度的80%
        val window = dialog.window
        window?.setLayout((resources.displayMetrics.widthPixels * 0.8).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        window?.setGravity(Gravity.CENTER)

        // 填充数据
        dialog.findViewById<TextView>(R.id.dialogConid)?.text = position.contract.m_conid.toString()
        dialog.findViewById<TextView>(R.id.dialogSymbol)?.text = position.contract.m_symbol
        dialog.findViewById<TextView>(R.id.dialogSecType)?.text = position.contract.m_secType
        dialog.findViewById<TextView>(R.id.dialogLastTradeDate)?.text = position.contract.m_lastTradeDateOrContractMonth
        dialog.findViewById<TextView>(R.id.dialogMultiplier)?.text = position.contract.m_multiplier
        dialog.findViewById<TextView>(R.id.dialogLocalSymbol)?.text = position.contract.m_localSymbol
        dialog.findViewById<TextView>(R.id.dialogQuantity)?.text = position.quantity.toString()
        dialog.findViewById<TextView>(R.id.dialogAvgPrice)?.text = position.avgPrice.toString()

        // 计算实际平均价格 (avgPrice / multiplier)
        val actualAvgPrice = try {
            val avgPrice = position.avgPrice
            val multiplier = position.contract.m_multiplier?.toDoubleOrNull() ?: 1.0
            avgPrice / multiplier
        } catch (e: Exception) {
            position.avgPrice // 如果计算失败，显示原始价格
        }
        dialog.findViewById<TextView>(R.id.dialogActualAvgPrice)?.text = String.format("%.2f", actualAvgPrice)

        // 显示弹窗
        dialog.show()
    }

    private fun fetchOrders() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val orders = repository.fetchOrders()
                Log.d("TransactionActivity", "Fetched ${orders.size} orders")
                
                // 更新UI
                withContext(Dispatchers.Main) {
                    Log.d("TransactionActivity", "Updating UI with ${orders.size} orders")
                    binding.transactionsInfoCard.transactionsLoadingText.visibility = View.GONE
                    binding.transactionsInfoCard.transactionsScrollView.visibility = View.VISIBLE
                    
                    // 设置RecyclerView
                    binding.transactionsInfoCard.transactionsRecyclerView.layoutManager = LinearLayoutManager(this@TransactionActivity)
                    binding.transactionsInfoCard.transactionsRecyclerView.adapter = OrdersAdapter(orders) {
                        showOrderDetailsDialog(it)
                    }
                    Log.d("TransactionActivity", "Orders RecyclerView adapter set with ${orders.size} items")
                }
            } catch (e: Exception) {
                Log.e("TransactionActivity", "Error fetching orders: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.transactionsInfoCard.transactionsLoadingText.text = "加载失败: ${e.message}"
                    binding.transactionsInfoCard.transactionsLoadingText.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showOrderDetailsDialog(order: OrderResponse) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_transaction_details)

        // 设置弹窗宽度为屏幕宽度的80%
        val window = dialog.window
        window?.setLayout((resources.displayMetrics.widthPixels * 0.8).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        window?.setGravity(Gravity.CENTER)

        // 填充数据
        dialog.findViewById<TextView>(R.id.dialogTransactionId)?.text = order.permId.toString()
        dialog.findViewById<TextView>(R.id.dialogDate)?.text = order.order.m_action
        dialog.findViewById<TextView>(R.id.dialogTime)?.text = order.order.m_orderType
        dialog.findViewById<TextView>(R.id.dialogSymbol)?.text = order.contract.m_symbol
        dialog.findViewById<TextView>(R.id.dialogQuantity)?.text = String.format("%.0f", order.order.m_totalQuantity)
        dialog.findViewById<TextView>(R.id.dialogPrice)?.text = String.format("%.2f", order.order.m_lmtPrice)
        dialog.findViewById<TextView>(R.id.dialogStatus)?.text = order.orderState.m_status

        // 显示弹窗
        dialog.show()
    }

    private fun showSubmitOrderDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_submit_order)
        dialog.setTitle("提交订单")
        dialog.setCancelable(true)

        // 设置对话框大小
        val window = dialog.window
        window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window?.setGravity(Gravity.CENTER)

        // 初始化交易方向选择器
        val spinnerAction = dialog.findViewById<Spinner>(R.id.spinnerAction)
        val actionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            arrayOf("BUY", "SELL")
        )
        actionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAction.adapter = actionAdapter

        // 设置取消按钮点击事件
        dialog.findViewById<Button>(R.id.buttonCancel)?.setOnClickListener {
            dialog.dismiss()
        }

        // 设置提交按钮点击事件
        dialog.findViewById<Button>(R.id.buttonSubmit)?.setOnClickListener {
            try {
                // 获取用户输入的参数
                val conid = dialog.findViewById<EditText>(R.id.editTextConid)?.text?.toString()?.toIntOrNull()
                val action = spinnerAction.selectedItem?.toString()
                val quantity = dialog.findViewById<EditText>(R.id.editTextQuantity)?.text?.toString()?.toIntOrNull()
                val price = dialog.findViewById<EditText>(R.id.editTextPrice)?.text?.toString()?.toDoubleOrNull()

                // 验证参数
                if (conid == null || action == null || quantity == null || price == null) {
                    showToast("请填写所有必填参数")
                    return@setOnClickListener
                }

                // 提交订单
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val success = repository.submitOrder(conid, action, quantity, price)
                        withContext(Dispatchers.Main) {
                            if (success) {
                                showToast("订单提交成功")
                                dialog.dismiss()
                                // 重新加载交易记录
                        fetchOrders()
                            } else {
                                showToast("订单提交失败")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("TransactionActivity", "Error submitting order: ${e.message}")
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            showToast("订单提交失败: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TransactionActivity", "Error in submit order dialog: ${e.message}")
                e.printStackTrace()
                showToast("发生错误: ${e.message}")
            }
        }

        // 显示对话框
        dialog.show()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.close()
    }
}