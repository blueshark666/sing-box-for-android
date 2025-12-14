package io.nekohasekai.sfa.ui.transaction

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.ui.transaction.model.PositionResponse

class PositionsAdapter(
    private val positions: List<PositionResponse>,
    private val onDetailClick: (PositionResponse) -> Unit
) : RecyclerView.Adapter<PositionsAdapter.PositionViewHolder>() {

    class PositionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val symbolTextView: TextView = itemView.findViewById(R.id.symbol)
        private val quantityTextView: TextView = itemView.findViewById(R.id.quantity)
        private val avgPriceTextView: TextView = itemView.findViewById(R.id.avgPrice)
        private val detailButton: Button = itemView.findViewById(R.id.detailButton)

        fun bind(position: PositionResponse, onDetailClick: (PositionResponse) -> Unit) {
            Log.d("PositionsAdapter", "Binding position: ${position.contract.m_symbol}, quantity: ${position.quantity}")
            symbolTextView.text = position.contract.m_symbol
            quantityTextView.text = position.quantity.toString()
            
            // 计算实际平均价格 (avgPrice / multiplier)
            val actualAvgPrice = try {
                val avgPrice = position.avgPrice
                val multiplier = position.contract.m_multiplier?.toDoubleOrNull() ?: 1.0
                avgPrice / multiplier
            } catch (e: Exception) {
                Log.e("PositionsAdapter", "Error calculating actual avg price: ${e.message}")
                position.avgPrice // 如果计算失败，显示原始价格
            }
            
            avgPriceTextView.text = String.format("%.2f", actualAvgPrice)
            Log.d("PositionsAdapter", "Set values: symbol=${position.contract.m_symbol}, quantity=${position.quantity}, avgPrice=${actualAvgPrice}")
            
            // 设置详情按钮点击事件
            detailButton.setOnClickListener {
                onDetailClick(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PositionViewHolder {
        Log.d("PositionsAdapter", "Creating ViewHolder")
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_position, parent, false)
        return PositionViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PositionViewHolder, position: Int) {
        Log.d("PositionsAdapter", "onBindViewHolder called for position: $position")
        holder.bind(positions[position], onDetailClick)
    }

    override fun getItemCount(): Int {
        Log.d("PositionsAdapter", "getItemCount: ${positions.size}")
        return positions.size
    }
}