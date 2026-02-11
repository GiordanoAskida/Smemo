package com.pwdmgr

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AddEditPasswordActivity : AppCompatActivity() {
    
    private lateinit var passwordManager: PasswordManager
    private var editingId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)
        
        title = "Smemo"
        
        passwordManager = PasswordManager(this)
        
        val titleInput = findViewById<EditText>(R.id.titleInput)
        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val urlInput = findViewById<EditText>(R.id.urlInput)
        val notesInput = findViewById<EditText>(R.id.notesInput)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val generateButton = findViewById<Button>(R.id.generateButton)
        
        editingId = intent.getStringExtra("id")
        
        if (editingId != null) {
            val entry = passwordManager.loadPasswords().find { it.id == editingId }
            entry?.let {
                titleInput.setText(it.title)
                usernameInput.setText(it.username)
                passwordInput.setText(it.password)
                urlInput.setText(it.url)
                notesInput.setText(it.notes)
            }
        }
        
        generateButton.setOnClickListener {
            passwordInput.setText(generatePassword())
        }
        
        saveButton.setOnClickListener {
            val title = titleInput.text.toString().trim()
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val url = urlInput.text.toString().trim()
            val notes = notesInput.text.toString().trim()
            
            if (title.isEmpty()) {
                Toast.makeText(this, "Inserisci titolo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.isEmpty()) {
                Toast.makeText(this, "Inserisci password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val entry = PasswordEntry(
                id = editingId ?: java.util.UUID.randomUUID().toString(),
                title = title,
                username = username,
                password = password,
                url = url,
                notes = notes
            )
            
            if (editingId != null) {
                passwordManager.updatePassword(entry)
                Toast.makeText(this, "Aggiornato", Toast.LENGTH_SHORT).show()
            } else {
                passwordManager.addPassword(entry)
                Toast.makeText(this, "Salvato", Toast.LENGTH_SHORT).show()
            }
            
            finish()
        }
    }
    
    private fun generatePassword(length: Int = 16): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*"
        return (1..length).map { chars.random() }.joinToString("")
    }
}
