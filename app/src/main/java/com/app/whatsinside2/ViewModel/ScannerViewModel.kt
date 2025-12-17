package com.app.whatsinside2

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScannerViewModel : ViewModel() {

    /**
     * Prüft, ob gescannt werden darf. Bei true wird nach Code gescannt,
     * bei false wurde ein Code gefunden der zur Zeit verarbeitet wird
     */
    private val _isScanning = MutableStateFlow(true)
    val isScanning = _isScanning.asStateFlow()

    fun onBarcodeFound() {
        _isScanning.value = false
    }

    fun startScanning() {
        _isScanning.value = true
    }
}