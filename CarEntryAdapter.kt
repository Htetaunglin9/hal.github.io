package com.hal.carlistmanager

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class CarEntryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var entries: List<CarEntry> = emptyList()
    private var totalFee: Int = 0

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_TOTAL = 2
    }

    fun submitList(newEntries: List<CarEntry>) {
        entries = newEntries
        totalFee = entries.sumOf { it.fee }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> VIEW_TYPE_HEADER
            itemCount - 1 -> VIEW_TYPE_TOTAL
            else -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_header_row, parent, false))
            VIEW_TYPE_TOTAL -> TotalViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_total_row, parent, false))
            else -> EntryViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_car_entry, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EntryViewHolder -> {
                val entry = entries[position - 1]
                holder.bind(entry, position)

                // Set background color for alternate rows
                val bgColor = if (position % 2 == 0) {
                    Color.WHITE
                } else {
                    Color.parseColor("#F5F5F5")
                }
                holder.itemView.setBackgroundColor(bgColor)

                // Make sure all TextViews are visible
                holder.tvNo.visibility = View.VISIBLE
                holder.tvDate.visibility = View.VISIBLE
                holder.tvCarNo.visibility = View.VISIBLE
                holder.tvWheels.visibility = View.VISIBLE
                holder.tvRoad.visibility = View.VISIBLE
                holder.tvGateFee.visibility = View.VISIBLE
            }
            is TotalViewHolder -> {
                val totalFee = entries.sumOf { it.fee }
                holder.bind(entries.size, totalFee)
            }
        }
    }

    override fun getItemCount(): Int = entries.size + 2

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view)

    inner class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNo: TextView = itemView.findViewById(R.id.tvNo)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvCarNo: TextView = itemView.findViewById(R.id.tvCarNo)
        val tvWheels: TextView = itemView.findViewById(R.id.tvWheels)
        val tvRoad: TextView = itemView.findViewById(R.id.tvRoad)
        val tvGateFee: TextView = itemView.findViewById(R.id.tvGateFee)

        fun bind(entry: CarEntry, position: Int) {
            tvNo.text = position.toString()
            tvDate.text = try {
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(entry.date) ?: entry.date
                )
            } catch (e: Exception) {
                entry.date
            }
            tvCarNo.text = entry.carNo
            tvWheels.text = entry.wheels.toString()
            tvRoad.text = entry.road
            tvGateFee.text = "${String.format(Locale.getDefault(), "%,d", entry.fee)} Ks"
        }
    }

    inner class TotalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTotalCount: TextView = itemView.findViewById(R.id.tvTotalCount)
        val tvTotalFee: TextView = itemView.findViewById(R.id.tvTotalFee)

        fun bind(count: Int, totalFee: Int) {
            tvTotalCount.text = count.toString()
            tvTotalFee.text = "${String.format(Locale.getDefault(), "%,d", totalFee)} Ks"
        }
    }
}