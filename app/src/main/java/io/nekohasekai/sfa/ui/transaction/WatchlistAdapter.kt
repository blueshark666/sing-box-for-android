package io.nekohasekai.sfa.ui.transaction

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.ui.transaction.model.WatchlistResponse

// 定义点击事件接口
typealias OnTradeButtonClickListener = (WatchlistResponse) -> Unit

class WatchlistAdapter(
    private val watchlist: List<WatchlistResponse>,
    private val onTradeButtonClick: OnTradeButtonClickListener? = null
) : RecyclerView.Adapter<WatchlistAdapter.WatchlistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchlistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_watchlist, parent, false)
        return WatchlistViewHolder(view)
    }

    override fun onBindViewHolder(holder: WatchlistViewHolder, position: Int) {
        val item = watchlist[position]
        holder.bind(item, onTradeButtonClick)
    }

    override fun getItemCount(): Int {
        return watchlist.size
    }

    inner class WatchlistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val symbolTextView: TextView = itemView.findViewById(R.id.watchlistSymbol)
        private val secTypeTextView: TextView = itemView.findViewById(R.id.watchlistSecType)
        private val bidTextView: TextView = itemView.findViewById(R.id.watchlistBid)
        private val askTextView: TextView = itemView.findViewById(R.id.watchlistAsk)
        private val tradeButton: Button = itemView.findViewById(R.id.watchlistTradeButton)

        fun bind(item: WatchlistResponse, onTradeButtonClick: OnTradeButtonClickListener?) {
            // 添加详细的调试日志
            Log.d("WatchlistAdapter", "Entering bind method for item: ${item}")
            Log.d("WatchlistAdapter", "Item symbol: ${item.symbol}")
            Log.d("WatchlistAdapter", "Item secType: ${item.secType}")
            Log.d("WatchlistAdapter", "Item conid: ${item.conid}")
            Log.d("WatchlistAdapter", "Item priceHolder: ${item.priceHolder}")
            Log.d("WatchlistAdapter", "Item priceHolder.bid: ${item.priceHolder.bid}")
            Log.d("WatchlistAdapter", "Item priceHolder.ask: ${item.priceHolder.ask}")
            
            // 设置股票代码
            symbolTextView.text = item.symbol
            Log.d("WatchlistAdapter", "Set symbolTextView.text to: ${symbolTextView.text}")

            // 设置证券类型
            secTypeTextView.text = item.secType
            Log.d("WatchlistAdapter", "Set secTypeTextView.text to: ${secTypeTextView.text}")

            // 设置买价
            val bidPrice = item.priceHolder.bid
            bidTextView.text = bidPrice.toString()
            Log.d("WatchlistAdapter", "Set bidTextView.text to: ${bidTextView.text}")
            Log.d("WatchlistAdapter", "bidTextView visibility: ${bidTextView.visibility}")

            // 设置卖价
            val askPrice = item.priceHolder.ask
            askTextView.text = askPrice.toString()
            Log.d("WatchlistAdapter", "Set askTextView.text to: ${askTextView.text}")
            Log.d("WatchlistAdapter", "askTextView visibility: ${askTextView.visibility}")

            // 设置交易按钮点击事件
            tradeButton.setOnClickListener {
                Log.d("WatchlistAdapter", "Trade button clicked for symbol: ${item.symbol}")
                onTradeButtonClick?.invoke(item)
            }
        }
    }
}