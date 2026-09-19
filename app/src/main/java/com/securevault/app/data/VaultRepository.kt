package com.securevault.app.data

import com.securevault.app.security.CryptoManager
import kotlinx.coroutines.flow.Flow

class VaultRepository(
    private val dao: VaultDao,
    private val encryptionKey: ByteArray
) {
    fun getAllEntries(): Flow<List<VaultEntry>> = dao.getAllEntries()

    fun searchEntries(query: String): Flow<List<VaultEntry>> = dao.searchEntries(query)

    suspend fun getDecryptedPassword(entry: VaultEntry): String {
        return try {
            CryptoManager.decrypt(entry.encryptedPassword, encryptionKey)
        } catch (e: Exception) {
            "خطأ في فك التشفير"
        }
    }

    suspend fun addEntry(title: String, username: String, password: String, url: String, notes: String, category: String) {
        val encrypted = CryptoManager.encrypt(password, encryptionKey)
        val entry = VaultEntry(
            title = title,
            username = username,
            encryptedPassword = encrypted,
            url = url,
            notes = notes,
            category = category
        )
        dao.insertEntry(entry)
    }

    suspend fun updateEntry(entry: VaultEntry, newPassword: String?) {
        val updated = if (newPassword != null) {
            entry.copy(
                encryptedPassword = CryptoManager.encrypt(newPassword, encryptionKey),
                updatedAt = System.currentTimeMillis()
            )
        } else {
            entry.copy(updatedAt = System.currentTimeMillis())
        }
        dao.updateEntry(updated)
    }

    suspend fun deleteEntry(entry: VaultEntry) = dao.deleteEntry(entry)
}
