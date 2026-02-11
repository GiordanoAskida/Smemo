package com.pwdmgr

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var passwordManager: PasswordManager
    private lateinit var biometricHelper: BiometricHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        passwordManager = PasswordManager(this)
        biometricHelper = BiometricHelper(this)
        
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val bioButton = findViewById<Button>(R.id.bioButton)
        
        if (passwordManager.isFirstRun()) {
            showSetPasswordDialog()
        }
        
        if (biometricHelper.isBiometricAvailable()) {
            bioButton.visibility = android.view.View.VISIBLE
            bioButton.setOnClickListener {
                biometricHelper.showBiometricPrompt(
                    onSuccess = { openPasswordList() },
                    onError = { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
                )
            }
        }
        
        loginButton.setOnClickListener {
            val pwd = passwordInput.text.toString()
            if (pwd.isEmpty()) {
                Toast.makeText(this, "Inserisci password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (passwordManager.verifyMasterPassword(pwd)) {
                openPasswordList()
            } else {
                Toast.makeText(this, "Password errata", Toast.LENGTH_SHORT).show()
                passwordInput.text.clear()
            }
        }
    }
    
    private fun showSetPasswordDialog() {
        val input1 = EditText(this)
        input1.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        val input2 = EditText(this)
        input2.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 20)
        layout.addView(android.widget.TextView(this).apply { text = "Password:" })
        layout.addView(input1)
        layout.addView(android.widget.TextView(this).apply { text = "Conferma:" })
        layout.addView(input2)
        
        AlertDialog.Builder(this)
            .setTitle("Imposta Password Master")
            .setMessage("Crea password per proteggere i tuoi dati")
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton("Salva") { _, _ ->
                val pwd1 = input1.text.toString()
                val pwd2 = input2.text.toString()
                
                when {
                    pwd1.isEmpty() || pwd2.isEmpty() -> {
                        Toast.makeText(this, "Compila entrambi i campi", Toast.LENGTH_SHORT).show()
                        showSetPasswordDialog()
                    }
                    pwd1 != pwd2 -> {
                        Toast.makeText(this, "Le password non corrispondono", Toast.LENGTH_SHORT).show()
                        showSetPasswordDialog()
                    }
                    pwd1.length < 4 -> {
                        Toast.makeText(this, "Minimo 4 caratteri", Toast.LENGTH_SHORT).show()
                        showSetPasswordDialog()
                    }
                    else -> {
                        passwordManager.setMasterPassword(pwd1)
                        Toast.makeText(this, "Password impostata!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }
    
    private fun openPasswordList() {
        startActivity(Intent(this, PasswordListActivity::class.java))
        findViewById<EditText>(R.id.passwordInput).text.clear()
    }
}
