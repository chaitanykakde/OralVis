package com.nextserve.oralvishealth.ui.cloud

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextserve.oralvishealth.databinding.FragmentCloudBinding
import com.nextserve.oralvishealth.data.model.CloudSession
import kotlinx.coroutines.launch
import com.nextserve.oralvishealth.ui.adapter.SessionAdapter
import com.nextserve.oralvishealth.ui.session.SessionDetailsActivity

class CloudFragment : Fragment() {
    
    private var _binding: FragmentCloudBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CloudViewModel by viewModels()
    private lateinit var sessionAdapter: SessionAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCloudBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
        
        // Load cloud sessions when fragment is created
        viewModel.loadCloudSessions()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh cloud sessions when returning to fragment
        viewModel.loadCloudSessions()
    }
    
    private fun setupRecyclerView() {
        sessionAdapter = SessionAdapter(
            onSessionClick = { session ->
                // Open cloud session details directly without saving to local database
                val intent = Intent(requireContext(), CloudSessionDetailsActivity::class.java)
                intent.putExtra("sessionId", session.sessionId)
                intent.putExtra("sessionName", session.name)
                intent.putExtra("sessionAge", session.age)
                intent.putExtra("sessionTimestamp", session.timestamp)
                startActivity(intent)
            },
            onUploadClick = { session ->
                // Hide upload button for cloud sessions since they're already uploaded
            }
        )
        
        binding.rvCloudSessions.apply {
            adapter = sessionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun observeViewModel() {
        viewModel.cloudSessions.observe(viewLifecycleOwner) { cloudSessions ->
            // Convert CloudSession to Session for adapter
            val sessions = cloudSessions.map { cloudSession ->
                com.nextserve.oralvishealth.data.entity.Session(
                    sessionId = cloudSession.sessionId,
                    name = cloudSession.name,
                    age = cloudSession.age,
                    timestamp = cloudSession.timestamp,
                    isUploaded = true
                )
            }
            sessionAdapter.submitList(sessions)
            updateUI(sessions.isEmpty(), false)
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            updateUI(sessionAdapter.itemCount == 0, isLoading)
        }
    }
    
    private fun updateUI(isEmpty: Boolean, isLoading: Boolean) {
        binding.apply {
            shimmerContainer.visibility = if (isLoading) View.VISIBLE else View.GONE
            emptyState.visibility = if (isEmpty && !isLoading) View.VISIBLE else View.GONE
            rvCloudSessions.visibility = if (!isEmpty && !isLoading) View.VISIBLE else View.GONE
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
