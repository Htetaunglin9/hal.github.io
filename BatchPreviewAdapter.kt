package com.hal.carlistmanager

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BatchPreviewAdapter(
    private val entries: List<CarEntry>,
    private val inMemoryDuplicates: Set<String>,
    private val existingCarNos: Set<String>
) : RecyclerView.Adapter<BatchPreviewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCarNo: TextView = view.findViewById(R.id.tvCarNo)
        val tvWheels: TextView = view.findViewById(R.id.tvWheels)
        val tvFee: TextView = view.findViewById(R.id.tvFee)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_batch_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.tvCarNo.text = entry.carNo
        holder.tvWheels.text = "${entry.wheels} ဘီး"
        holder.tvFee.text = "${entry.fee} Ks"

        // Highlight in RED if carNo is duplicate or already in DB
        if (entry.carNo in inMemoryDuplicates || entry.carNo in existingCarNos) {
            holder.tvCarNo.setTextColor(Color.RED)
        } else {
            holder.tvCarNo.setTextColor(Color.BLACK)
        }
    }

    override fun getItemCount(): Int = entries.size
}
