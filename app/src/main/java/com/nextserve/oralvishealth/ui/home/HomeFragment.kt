package com.nextserve.oralvishealth.ui.home

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.material.snackbar.Snackbar
import com.nextserve.oralvishealth.R
import com.nextserve.oralvishealth.databinding.FragmentHomeBinding
import com.nextserve.oralvishealth.databinding.DialogUploadProgressBinding
import com.nextserve.oralvishealth.ui.adapter.SessionAdapter
import com.nextserve.oralvishealth.ui.camera.CameraActivity
import com.nextserve.oralvishealth.ui.session.SessionDetailsActivity
import com.nextserve.oralvishealth.ui.viewmodel.SessionViewModel
import com.nextserve.oralvishealth.worker.UploadWorker

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sessionViewModel: SessionViewModel
    private lateinit var sessionAdapter: SessionAdapter
    

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]
        
        setupRecyclerView()
        observeSessions()
        setupFab()
        setupSearch()
    }

    private fun setupRecyclerView() {
        sessionAdapter = SessionAdapter(
            onSessionClick = { session ->
                val intent = Intent(requireContext(), SessionDetailsActivity::class.java)
                intent.putExtra("sessionId", session.sessionId)
                startActivity(intent)
            },
            onUploadClick = { session ->
                showUploadConfirmationDialog(session)
            }
        )
        
        binding.rvSessions.apply {
            adapter = sessionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeSessions() {
        sessionViewModel.allSessions.observe(viewLifecycleOwner) { sessions ->
            sessionAdapter.submitList(sessions)
            
            if (sessions.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvSessions.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvSessions.visibility = View.VISIBLE
            }
        }
    }

    private fun setupFab() {
        binding.fabNewSession.setOnClickListener {
            val intent = Intent(requireContext(), CameraActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                sessionViewModel.searchSessions(query)
            }
        })
    }

    private fun showUploadConfirmationDialog(session: com.nextserve.oralvishealth.data.entity.Session) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.upload_to_cloud))
            .setMessage(getString(R.string.upload_session_question))
            .setPositiveButton(getString(R.string.upload)) { _, _ ->
                startUpload(session)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun startUpload(session: com.nextserve.oralvishealth.data.entity.Session) {
        // Show inline upload progress indicator
        binding.uploadProgressCard.visibility = View.VISIBLE
        binding.tvUploadStatus.text = "Uploading your session..."

        // Create WorkManager request
        val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(UploadWorker.KEY_SESSION_ID to session.sessionId))
            .build()

        // Start the work
        WorkManager.getInstance(requireContext()).enqueue(uploadRequest)

        // Observe work progress
        WorkManager.getInstance(requireContext())
            .getWorkInfoByIdLiveData(uploadRequest.id)
            .observe(viewLifecycleOwner) { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        val status = workInfo.progress.getString(UploadWorker.KEY_STATUS) ?: "Uploading..."
                        binding.tvUploadStatus.text = status
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        binding.uploadProgressCard.visibility = View.GONE
                        Snackbar.make(binding.root, getString(R.string.session_uploaded_successfully), Snackbar.LENGTH_LONG).show()
                        // Refresh sessions to update UI
                        sessionViewModel.refreshSessions()
                        
                        // Also refresh cloud sessions if user switches to cloud tab
                        android.util.Log.d("HomeFragment", "Upload completed - session should now appear in cloud")
                    }
                    WorkInfo.State.FAILED -> {
                        binding.uploadProgressCard.visibility = View.GONE
                        val error = workInfo.outputData.getString("error") ?: getString(R.string.upload_failed)
                        Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
