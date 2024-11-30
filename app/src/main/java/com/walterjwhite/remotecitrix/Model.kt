package com.walterjwhite.remotecitrix

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Model : ViewModel() {
    val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token

    val _status = MutableStateFlow("Token must be 6 digits")
    val status: StateFlow<String> = _status

    val _error = MutableStateFlow(true)
    val error: StateFlow<Boolean> = _error

    val _waitingFromServer = MutableStateFlow(false)
    val waitingFromServer: StateFlow<Boolean> = _waitingFromServer

    fun updateToken(entered: String) {
        if (entered.length <= 6) {
            _token.value = entered
        }

        if (_token.value.length == 6) {
            _error.value = false
            _status.value = "Token is valid"
        } else {
            _error.value = true
            _status.value = "Token must be 6 digits"
        }
    }
}