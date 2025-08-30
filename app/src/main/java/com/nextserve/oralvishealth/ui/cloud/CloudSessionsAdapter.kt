package com.nextserve.oralvishealth.ui.cloud

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nextserve.oralvishealth.data.model.CloudSession
import com.nextserve.oralvishealth.databinding.ItemCloudSessionBinding

class CloudSessionsAdapter(
    private val onItemClick: (CloudSession) -> Unit
) : ListAdapter<CloudSession, CloudSessionsAdapter.CloudSessionViewHolder>(CloudSessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CloudSessionViewHolder {
        val binding = ItemCloudSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CloudSessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CloudSessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CloudSessionViewHolder(
        private val binding: ItemCloudSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cloudSession: CloudSession) {
            binding.apply {
                tvSessionId.text = cloudSession.sessionId
                tvPatientName.text = cloudSession.name
                
                root.setOnClickListener {
                    onItemClick(cloudSession)
                }
            }
        }
    }

    private class CloudSessionDiffCallback : DiffUtil.ItemCallback<CloudSession>() {
        override fun areItemsTheSame(oldItem: CloudSession, newItem: CloudSession): Boolean {
            return oldItem.sessionId == newItem.sessionId
        }

        override fun areContentsTheSame(oldItem: CloudSession, newItem: CloudSession): Boolean {
            return oldItem == newItem
        }
    }
}
