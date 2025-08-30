package com.nextserve.oralvishealth.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextserve.oralvishealth.databinding.ShimmerImageItemBinding

class ShimmerImageAdapter(private val itemCount: Int) : RecyclerView.Adapter<ShimmerImageAdapter.ShimmerViewHolder>() {

    class ShimmerViewHolder(private val binding: ShimmerImageItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShimmerViewHolder {
        val binding = ShimmerImageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShimmerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShimmerViewHolder, position: Int) {
        // No binding needed for shimmer placeholders
    }

    override fun getItemCount(): Int = itemCount
}
