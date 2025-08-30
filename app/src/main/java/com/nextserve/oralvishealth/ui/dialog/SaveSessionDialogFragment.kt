package com.nextserve.oralvishealth.ui.dialog

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextserve.oralvishealth.R
import com.nextserve.oralvishealth.data.entity.Session
import com.nextserve.oralvishealth.databinding.DialogSaveSessionBinding
import com.nextserve.oralvishealth.ui.viewmodel.SessionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class SaveSessionDialogFragment : DialogFragment() {
    private var _binding: DialogSaveSessionBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sessionViewModel: SessionViewModel
    private var capturedImages: List<Uri> = emptyList()

    companion object {
        fun newInstance(images: List<Uri>): SaveSessionDialogFragment {
            val fragment = SaveSessionDialogFragment()
            val args = Bundle()
            args.putParcelableArrayList("images", ArrayList(images))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        capturedImages = arguments?.getParcelableArrayList<Uri>("images") ?: emptyList()
        sessionViewModel = ViewModelProvider(requireActivity())[SessionViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSaveSessionBinding.inflate(layoutInflater)
        
        generateSessionId()
        setupClickListeners()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnSave.setOnClickListener {
            if (validateInputs()) {
                saveSession()
            }
        }
    }

    private fun generateSessionId() {
        val timestamp = System.currentTimeMillis()
        val randomSuffix = (1000..9999).random()
        val sessionId = "OVH-${timestamp}-${randomSuffix}"
        binding.tvSessionId.text = sessionId
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        
        if (binding.etPatientName.text.toString().trim().isEmpty()) {
            binding.tilPatientName.error = getString(R.string.field_required)
            isValid = false
        } else {
            binding.tilPatientName.error = null
        }
        
        if (binding.etPatientAge.text.toString().trim().isEmpty()) {
            binding.tilPatientAge.error = getString(R.string.field_required)
            isValid = false
        } else {
            binding.tilPatientAge.error = null
        }
        
        return isValid
    }

    private fun saveSession() {
        val sessionId = binding.tvSessionId.text.toString().trim()
        val patientName = binding.etPatientName.text.toString().trim()
        val patientAge = binding.etPatientAge.text.toString().trim()
        
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    saveImagesToStorage(sessionId)
                }
                
                val session = Session(
                    sessionId = sessionId,
                    name = patientName,
                    age = patientAge
                )
                
                sessionViewModel.insertSession(session)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), getString(R.string.session_saved_successfully), Toast.LENGTH_SHORT).show()
                    requireActivity().finish()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error saving session: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun saveImagesToStorage(sessionId: String) {
        val sessionDir = File(requireContext().getExternalFilesDir(null), "Sessions/$sessionId")
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }
        
        capturedImages.forEachIndexed { index, uri ->
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(System.currentTimeMillis() + index)
            val destinationFile = File(sessionDir, "IMG_$timestamp.jpg")
            
            val sourceFile = File(uri.path!!)
            if (sourceFile.exists()) {
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
                sourceFile.delete()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
