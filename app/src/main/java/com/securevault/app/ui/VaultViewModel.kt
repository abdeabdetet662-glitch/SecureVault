package com.securevault.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.app.data.VaultDatabase
import com.securevault.app.data.VaultEntry
import com.securevault.app.data.VaultRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class VaultUiState(
    val entries: List<VaultEntry> = emptyList(),
    val searchQuery: String = "",
    val isLocked: Boolean = true,
    val error: String? = null
)

class VaultViewModel(app: Application) : AndroidViewModel(app) {

    private var repository: VaultRepository? = null
    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    fun unlock(masterPassword: String, salt: ByteArray) {
        viewModelScope.launch {
            try {
                val key = com.securevault.app.security.CryptoManager.deriveKey(masterPassword, salt)
                val db = VaultDatabase.getInstance(getApplication(), key)
                repository = VaultRepository(db.vaultDao(), key)
                _uiState.update { it.copy(isLocked = false, error = null) }
                loadEntries()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "كلمة السر غير صحيحة") }
            }
        }
    }

    fun lock() {
        VaultDatabase.close()
        repository = null
        _uiState.update { it.copy(isLocked = true, entries = emptyList()) }
    }

    private fun loadEntries() {
        viewModelScope.launch {
            repository?.getAllEntries()?.collect { list ->
                _uiState.update { it.copy(entries = list) }
            }
        }
    }

    fun onSearchChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            val flow = if (query.isBlank()) repository?.getAllEntries()
                       else repository?.searchEntries(query)
            flow?.collect { list -> _uiState.update { it.copy(entries = list) } }
        }
    }

    fun addEntry(title: String, username: String, password: String, url: String, notes: String, category: String) {
        viewModelScope.launch {
            try {
                repository?.addEntry(title, username, password, url, notes, category)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteEntry(entry: VaultEntry) {
        viewModelScope.launch { repository?.deleteEntry(entry) }
    }

    suspend fun getDecryptedPassword(entry: VaultEntry): String {
        return repository?.getDecryptedPassword(entry) ?: ""
    }
}
