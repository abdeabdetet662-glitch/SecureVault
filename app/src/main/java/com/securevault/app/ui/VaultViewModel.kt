package com.securevault.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.app.security.CryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VaultItem(
    val id: Long,
    val title: String,
    val username: String,
    val encryptedPassword: String
)

data class VaultUiState(
    val entries: List<VaultItem> = emptyList(),
    val isLocked: Boolean = true,
    val error: String? = null
)

class VaultViewModel(app: Application) : AndroidViewModel(app) {

    private var encryptionKey: ByteArray? = null
    private var nextId = 1L
    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    fun unlock(masterPassword: String, salt: ByteArray) {
        viewModelScope.launch {
            try {
                encryptionKey = CryptoManager.deriveKey(masterPassword, salt)
                _uiState.update { it.copy(isLocked = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "خطأ: ${e.message}") }
            }
        }
    }

    fun lock() {
        encryptionKey = null
        _uiState.update { it.copy(isLocked = true, entries = emptyList()) }
    }

    fun addEntry(title: String, username: String, password: String) {
        val key = encryptionKey ?: return
        val encrypted = CryptoManager.encrypt(password, key)
        val newItem = VaultItem(nextId++, title, username, encrypted)
        _uiState.update { it.copy(entries = it.entries + newItem) }
    }

    fun deleteEntry(item: VaultItem) {
        _uiState.update { it.copy(entries = it.entries.filter { e -> e.id != item.id }) }
    }

    fun decrypt(encrypted: String): String {
        val key = encryptionKey ?: return ""
        return try { CryptoManager.decrypt(encrypted, key) } catch (e: Exception) { "" }
    }
}
