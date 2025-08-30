package com.nextserve.oralvishealth.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nextserve.oralvishealth.databinding.ItemCapturedImageBinding
import java.io.File

class CapturedImageAdapter(
    private val images: MutableList<File>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<CapturedImageAdapter.CapturedImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CapturedImageViewHolder {
        val binding = ItemCapturedImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CapturedImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CapturedImageViewHolder, position: Int) {
        holder.bind(images[position], position)
    }

    override fun getItemCount(): Int = images.size

    fun addImage(imageFile: File) {
        images.add(imageFile)
        notifyItemInserted(images.size - 1)
    }
    
    fun getImages(): List<File> = images

    fun removeImage(position: Int) {
        if (position in 0 until images.size) {
            images.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, images.size)
        }
    }

    inner class CapturedImageViewHolder(
        private val binding: ItemCapturedImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(imageFile: File, position: Int) {
            // Load image thumbnail using Glide
            Glide.with(binding.root.context)
                .load(imageFile)
                .centerCrop()
                .into(binding.ivThumbnail)

            // Set remove click listener
            binding.cvRemove.setOnClickListener {
                onRemoveClick(position)
            }
        }
    }
}
