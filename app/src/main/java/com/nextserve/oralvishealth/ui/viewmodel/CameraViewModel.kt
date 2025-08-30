package com.nextserve.oralvishealth.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraViewModel : ViewModel() {
    private val _capturedImages = MutableLiveData<MutableList<Uri>>(mutableListOf())
    val capturedImages: LiveData<MutableList<Uri>> = _capturedImages

    private val _imageCount = MutableLiveData(0)
    val imageCount: LiveData<Int> = _imageCount

    fun addCapturedImage(uri: Uri) {
        val currentList = _capturedImages.value ?: mutableListOf()
        currentList.add(uri)
        _capturedImages.value = currentList
        _imageCount.value = currentList.size
    }

    fun clearImages() {
        _capturedImages.value = mutableListOf()
        _imageCount.value = 0
    }

    fun removeCapturedImage(position: Int) {
        val currentList = _capturedImages.value ?: mutableListOf()
        if (position in 0 until currentList.size) {
            currentList.removeAt(position)
            _capturedImages.value = currentList
            _imageCount.value = currentList.size
        }
    }

    fun getCapturedImagesList(): List<Uri> {
        return _capturedImages.value ?: emptyList()
    }
}
