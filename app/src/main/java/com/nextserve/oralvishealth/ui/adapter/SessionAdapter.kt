package com.nextserve.oralvishealth.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nextserve.oralvishealth.data.entity.Session
import com.nextserve.oralvishealth.databinding.ItemSessionBinding
import java.text.SimpleDateFormat
import java.util.*

class SessionAdapter(
    private val onSessionClick: (Session) -> Unit,
    private val onUploadClick: (Session) -> Unit
) : ListAdapter<Session, SessionAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SessionViewHolder(private val binding: ItemSessionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(session: Session) {
            binding.tvSessionId.text = session.sessionId
            binding.tvPatientName.text = session.name
            
            val dateFormat = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
            binding.tvTimestamp.text = dateFormat.format(Date(session.timestamp))
            
            // Show/hide upload button based on upload status
            binding.btnUpload.visibility = if (session.isUploaded) View.GONE else View.VISIBLE
            
            binding.root.setOnClickListener {
                onSessionClick(session)
            }
            
            binding.btnUpload.setOnClickListener {
                onUploadClick(session)
            }
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<Session>() {
        override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean {
            return oldItem.sessionId == newItem.sessionId
        }

        override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean {
            return oldItem == newItem
        }
    }
}
