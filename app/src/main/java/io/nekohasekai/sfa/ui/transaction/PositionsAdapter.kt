package io.nekohasekai.sfa.ui.transaction

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.ui.transaction.model.PositionResponse

class PositionsAdapter(private val positions: List<PositionResponse>) : RecyclerView.Adapter<PositionsAdapter.PositionViewHolder>() {

    class PositionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val conidTextView: TextView = itemView.findViewById(R.id.conid)
        private val symbolTextView: TextView = itemView.findViewById(R.id.symbol)
        private val secTypeTextView: TextView = itemView.findViewById(R.id.secType)
        private val lastTradeDateTextView: TextView = itemView.findViewById(R.id.lastTradeDate)
        private val multiplierTextView: TextView = itemView.findViewById(R.id.multiplier)
        private val localSymbolTextView: TextView = itemView.findViewById(R.id.localSymbol)
        private val quantityTextView: TextView = itemView.findViewById(R.id.quantity)
        private val avgPriceTextView: TextView = itemView.findViewById(R.id.avgPrice)

        fun bind(position: PositionResponse) {
            conidTextView.text = position.contract.m_conid.toString()
            symbolTextView.text = position.contract.m_symbol
            secTypeTextView.text = position.contract.m_secType
            lastTradeDateTextView.text = position.contract.m_lastTradeDateOrContractMonth
            multiplierTextView.text = position.contract.m_multiplier
            localSymbolTextView.text = position.contract.m_localSymbol
            quantityTextView.text = position.quantity.toString()
            
            // 计算实际平均价格 (avgPrice / multiplier)
            val actualAvgPrice = try {
                val avgPrice = position.avgPrice
                val multiplier = position.contract.m_multiplier.toDoubleOrNull() ?: 1.0
                avgPrice / multiplier
            } catch (e: Exception) {
                position.avgPrice // 如果计算失败，显示原始价格
            }
            
            avgPriceTextView.text = String.format("%.2f", actualAvgPrice)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PositionViewHolder {
        val itemView = createPositionItemView(parent.context)
        return PositionViewHolder(itemView)
    }

    private fun createPositionItemView(context: Context): View {
        // 直接创建垂直LinearLayout作为根布局，与account_info保持一致
        val mainLayout = LinearLayout(context)
        val mainParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        mainLayout.layoutParams = mainParams
        mainLayout.orientation = LinearLayout.VERTICAL
        
        // 添加合约ID
        mainLayout.addView(createRowView(context, "合约ID:", "conid"))
        mainLayout.addView(createDivider(context))
        
        // 添加代码
        mainLayout.addView(createRowView(context, "代码:", "symbol"))
        mainLayout.addView(createDivider(context))
        
        // 添加类型
        mainLayout.addView(createRowView(context, "类型:", "secType"))
        mainLayout.addView(createDivider(context))
        
        // 添加到期日
        mainLayout.addView(createRowView(context, "到期日:", "lastTradeDate"))
        mainLayout.addView(createDivider(context))
        
        // 添加乘数
        mainLayout.addView(createRowView(context, "乘数:", "multiplier"))
        mainLayout.addView(createDivider(context))
        
        // 添加本地代码
        mainLayout.addView(createRowView(context, "本地代码:", "localSymbol"))
        mainLayout.addView(createDivider(context))
        
        // 添加数量
        mainLayout.addView(createRowView(context, "数量:", "quantity"))
        mainLayout.addView(createDivider(context))
        
        // 添加平均价格
        mainLayout.addView(createRowView(context, "平均价格:", "avgPrice"))
        
        return mainLayout
    }

    private fun createRowView(context: Context, label: String, viewId: String): View {
        val rowLayout = LinearLayout(context)
        val rowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        rowLayout.layoutParams = rowParams
        rowLayout.orientation = LinearLayout.HORIZONTAL
        rowLayout.setPadding(8, 8, 8, 8)
        
        // 创建标签TextView
        val labelTextView = TextView(context)
        val labelParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        labelTextView.layoutParams = labelParams
        labelTextView.text = label
        labelTextView.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        
        // 创建值TextView
        val valueTextView = TextView(context)
        val valueParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        valueTextView.layoutParams = valueParams
        valueTextView.gravity = android.view.Gravity.END
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            valueTextView.setTextAppearance(com.google.android.material.R.attr.textAppearanceBodyMedium)
        } else {
            valueTextView.setTextAppearance(context, com.google.android.material.R.attr.textAppearanceBodyMedium)
        }
        
        // 设置ID
        when (viewId) {
            "conid" -> valueTextView.id = R.id.conid
            "symbol" -> valueTextView.id = R.id.symbol
            "secType" -> valueTextView.id = R.id.secType
            "lastTradeDate" -> valueTextView.id = R.id.lastTradeDate
            "multiplier" -> valueTextView.id = R.id.multiplier
            "localSymbol" -> valueTextView.id = R.id.localSymbol
            "quantity" -> valueTextView.id = R.id.quantity
            "avgPrice" -> valueTextView.id = R.id.avgPrice
        }
        
        // 将TextView添加到rowLayout
        rowLayout.addView(labelTextView)
        rowLayout.addView(valueTextView)
        
        return rowLayout
    }

    private fun createDivider(context: Context): View {
        val divider = View(context)
        val dividerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            1
        )
        divider.layoutParams = dividerParams
        divider.setBackgroundColor(0xFFE0E0E0.toInt())
        
        return divider
    }

    override fun onBindViewHolder(holder: PositionViewHolder, position: Int) {
        holder.bind(positions[position])
    }

    override fun getItemCount(): Int {
        return positions.size
    }
}