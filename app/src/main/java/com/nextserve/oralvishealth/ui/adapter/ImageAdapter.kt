package com.nextserve.oralvishealth.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nextserve.oralvishealth.databinding.ItemImageBinding
import java.io.File

class ImageAdapter(
    private val images: List<File>,
    private val onImageClick: (File) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    inner class ImageViewHolder(private val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(imageFile: File) {
            Glide.with(binding.root.context)
                .load(imageFile)
                .centerCrop()
                .into(binding.ivImage)
            
            binding.root.setOnClickListener {
                onImageClick(imageFile)
            }
        }
    }
}
