package io.nekohasekai.sfa.ui.transaction

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.ui.transaction.model.OrderResponse

class OrdersAdapter(
    private val orders: List<OrderResponse>,
    private val onDetailClick: (OrderResponse) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val symbolTextView: TextView = itemView.findViewById(R.id.orderSymbol)
        private val actionTextView: TextView = itemView.findViewById(R.id.orderAction)
        private val quantityTextView: TextView = itemView.findViewById(R.id.orderQuantity)
        private val lmtPriceTextView: TextView = itemView.findViewById(R.id.orderLmtPrice)
        private val statusTextView: TextView = itemView.findViewById(R.id.orderStatus)
        private val detailButton: Button = itemView.findViewById(R.id.orderDetailButton)

        fun bind(order: OrderResponse, onDetailClick: (OrderResponse) -> Unit) {
            Log.d("OrdersAdapter", "Binding order: ${order.contract.m_symbol}, action: ${order.order.m_action}, quantity: ${order.order.m_totalQuantity}, status: ${order.orderState.m_status}")
            symbolTextView.text = order.contract.m_symbol
            actionTextView.text = order.order.m_action
            quantityTextView.text = String.format("%.0f", order.order.m_totalQuantity)
            lmtPriceTextView.text = String.format("%.2f", order.order.m_lmtPrice)
            statusTextView.text = order.orderState.m_status
            
            // 设置详情按钮点击事件
            detailButton.setOnClickListener {
                onDetailClick(order)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        Log.d("OrdersAdapter", "Creating ViewHolder")
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return OrderViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        Log.d("OrdersAdapter", "onBindViewHolder called for position: $position")
        holder.bind(orders[position], onDetailClick)
    }

    override fun getItemCount(): Int {
        Log.d("OrdersAdapter", "getItemCount: ${orders.size}")
        return orders.size
    }
}