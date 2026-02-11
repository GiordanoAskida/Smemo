package com.pwdmgr

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.security.MessageDigest

class PasswordManager(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "pwd_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val gson = Gson()
    
    fun isFirstRun(): Boolean {
        return prefs.getBoolean("first_run", true)
    }
    
    fun setMasterPassword(password: String) {
        val hash = hashPassword(password)
        prefs.edit()
            .putString("master_pwd", hash)
            .putBoolean("first_run", false)
            .apply()
    }
    
    fun verifyMasterPassword(password: String): Boolean {
        val saved = prefs.getString("master_pwd", null)
        return saved == hashPassword(password)
    }
    
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    fun savePasswords(passwords: List<PasswordEntry>) {
        val json = gson.toJson(passwords)
        prefs.edit().putString("passwords", json).apply()
    }
    
    fun loadPasswords(): MutableList<PasswordEntry> {
        val json = prefs.getString("passwords", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<PasswordEntry>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }
    
    fun addPassword(entry: PasswordEntry) {
        val list = loadPasswords()
        list.add(entry)
        savePasswords(list)
    }
    
    fun updatePassword(entry: PasswordEntry) {
        val list = loadPasswords()
        val index = list.indexOfFirst { it.id == entry.id }
        if (index != -1) {
            list[index] = entry
            savePasswords(list)
        }
    }
    
    fun deletePassword(id: String) {
        val list = loadPasswords()
        list.removeAll { it.id == id }
        savePasswords(list)
    }
}
