package com.nextserve.oralvishealth.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.nextserve.oralvishealth.R
import com.nextserve.oralvishealth.databinding.FragmentSearchBinding
import com.nextserve.oralvishealth.ui.session.SessionDetailsActivity
import com.nextserve.oralvishealth.ui.viewmodel.SessionViewModel
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sessionViewModel: SessionViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnSearch.setOnClickListener {
            val sessionId = binding.etSearchSessionId.text.toString().trim()
            if (sessionId.isNotEmpty()) {
                searchSession(sessionId)
            } else {
                binding.tilSearchSessionId.error = getString(R.string.field_required)
            }
        }
        
        binding.etSearchSessionId.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tilSearchSessionId.error = null
            }
        }
    }

    private fun searchSession(sessionId: String) {
        lifecycleScope.launch {
            val session = sessionViewModel.getSessionById(sessionId)
            if (session != null) {
                val intent = Intent(requireContext(), SessionDetailsActivity::class.java)
                intent.putExtra("sessionId", sessionId)
                startActivity(intent)
            } else {
                Snackbar.make(binding.root, getString(R.string.session_not_found), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
