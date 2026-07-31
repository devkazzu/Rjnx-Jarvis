package com.example.accessibility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ScreenContentHolder {

    private val _currentScreenText = MutableStateFlow("")
    val currentScreenText: StateFlow<String> = _currentScreenText.asStateFlow()

    private val _currentPackageName = MutableStateFlow("")
    val currentPackageName: StateFlow<String> = _currentPackageName.asStateFlow()

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    fun updateScreenContent(text: String, packageName: String) {
        _currentScreenText.value = text
        _currentPackageName.value = packageName
    }

    fun setServiceActive(active: Boolean) {
        _isServiceActive.value = active
    }
}
