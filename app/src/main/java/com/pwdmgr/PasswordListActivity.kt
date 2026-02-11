package com.pwdmgr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class PasswordListActivity : AppCompatActivity() {
    
    private lateinit var passwordManager: PasswordManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var addButton: Button
    private lateinit var searchInput: EditText
    private lateinit var exportButton: Button
    private lateinit var importButton: Button
    private lateinit var sortButton: Button
    private lateinit var menuButton: Button
    private var allPasswords = mutableListOf<PasswordEntry>()
    private var currentSort = SortType.ALPHABETICAL
    
    enum class SortType {
        ALPHABETICAL, RECENT, OLDEST
    }
    
    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { importFromUri(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        
        title = "Smemo"
        
        passwordManager = PasswordManager(this)
        
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.emptyView)
        addButton = findViewById(R.id.addButton)
        searchInput = findViewById(R.id.searchInput)
        exportButton = findViewById(R.id.exportButton)
        importButton = findViewById(R.id.importButton)
        sortButton = findViewById(R.id.sortButton)
        menuButton = findViewById(R.id.menuButton)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        addButton.setOnClickListener {
            startActivity(Intent(this, AddEditPasswordActivity::class.java))
        }
        
        exportButton.setOnClickListener {
            exportPasswords()
        }
        
        importButton.setOnClickListener {
            pickFile.launch("*/*")
        }
        
        sortButton.setOnClickListener {
            showSortMenu()
        }
        
        menuButton.setOnClickListener {
            showMainMenu()
        }
        
        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterPasswords(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
        loadPasswords()
    }
    
    override fun onResume() {
        super.onResume()
        loadPasswords()
    }
    
    private fun loadPasswords() {
        allPasswords = passwordManager.loadPasswords()
        sortPasswords()
        filterPasswords(searchInput.text.toString())
    }
    
    private fun sortPasswords() {
        when (currentSort) {
            SortType.ALPHABETICAL -> allPasswords.sortBy { it.title.lowercase() }
            SortType.RECENT -> allPasswords.sortByDescending { it.id }
            SortType.OLDEST -> allPasswords.sortBy { it.id }
        }
    }
    
    private fun showSortMenu() {
        val options = arrayOf("Alfabetico", "Più recenti", "Più vecchie")
        AlertDialog.Builder(this)
            .setTitle("Ordina per")
            .setItems(options) { _, which ->
                currentSort = when (which) {
                    0 -> SortType.ALPHABETICAL
                    1 -> SortType.RECENT
                    else -> SortType.OLDEST
                }
                loadPasswords()
            }
            .show()
    }
    
    private fun showMainMenu() {
        val options = arrayOf("Cambia password master")
        AlertDialog.Builder(this)
            .setTitle("Menu")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> changeMasterPassword()
                }
            }
            .show()
    }
    
    private fun changeMasterPassword() {
        val input1 = EditText(this)
        input1.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        val input2 = EditText(this)
        input2.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        val input3 = EditText(this)
        input3.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 20)
        layout.addView(android.widget.TextView(this).apply { text = "Password attuale:" })
        layout.addView(input1)
        layout.addView(android.widget.TextView(this).apply { text = "Nuova password:" })
        layout.addView(input2)
        layout.addView(android.widget.TextView(this).apply { text = "Conferma nuova:" })
        layout.addView(input3)
        
        AlertDialog.Builder(this)
            .setTitle("Cambia Password Master")
            .setView(layout)
            .setPositiveButton("Cambia") { _, _ ->
                val current = input1.text.toString()
                val new1 = input2.text.toString()
                val new2 = input3.text.toString()
                
                when {
                    !passwordManager.verifyMasterPassword(current) -> {
                        Toast.makeText(this, "Password attuale errata", Toast.LENGTH_SHORT).show()
                    }
                    new1.isEmpty() || new2.isEmpty() -> {
                        Toast.makeText(this, "Compila tutti i campi", Toast.LENGTH_SHORT).show()
                    }
                    new1 != new2 -> {
                        Toast.makeText(this, "Le nuove password non corrispondono", Toast.LENGTH_SHORT).show()
                    }
                    new1.length < 4 -> {
                        Toast.makeText(this, "Minimo 4 caratteri", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        passwordManager.setMasterPassword(new1)
                        Toast.makeText(this, "Password cambiata!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    
    private fun filterPasswords(query: String) {
        val filtered = if (query.isEmpty()) {
            allPasswords
        } else {
            allPasswords.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.username.contains(query, ignoreCase = true) ||
                it.url.contains(query, ignoreCase = true)
            }
        }
        
        if (filtered.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            emptyView.text = if (query.isEmpty()) "Nessuna password\n\nPremi Aggiungi" else "Nessun risultato"
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            
            recyclerView.adapter = PasswordAdapter(
                filtered,
                onEdit = { startActivity(Intent(this, AddEditPasswordActivity::class.java).putExtra("id", it.id)) },
                onDelete = { showDeleteDialog(it) },
                onCopy = { text, label -> copyToClipboard(text, label) }
            )
        }
    }
    
    private fun exportPasswords() {
        val passwords = passwordManager.loadPasswords()
        if (passwords.isEmpty()) {
            Toast.makeText(this, "Nessuna password da salvare", Toast.LENGTH_SHORT).show()
            return
        }
        
        val csv = buildString {
            appendLine("Titolo,Username,Password,URL,Note")
            passwords.forEach {
                appendLine("\"${it.title}\",\"${it.username}\",\"${it.password}\",\"${it.url}\",\"${it.notes}\"")
            }
        }
        
        try {
            val documentsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Smemo")
            documentsDir.mkdirs()
            val file = File(documentsDir, "smemo_backup_${System.currentTimeMillis()}.csv")
            file.writeText(csv)
            
            AlertDialog.Builder(this)
                .setTitle("Backup Salvato")
                .setMessage("File salvato in:\nDocuments/Smemo/\n\n${file.name}")
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun importFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val text = inputStream?.bufferedReader()?.use { it.readText() }
            inputStream?.close()
            
            if (text.isNullOrEmpty()) {
                Toast.makeText(this, "File vuoto", Toast.LENGTH_SHORT).show()
                return
            }
            
            val lines = text.lines()
            if (lines.size < 2) {
                Toast.makeText(this, "File non valido", Toast.LENGTH_SHORT).show()
                return
            }
            
            var imported = 0
            lines.drop(1).forEach { line ->
                if (line.trim().isEmpty()) return@forEach
                
                val parts = line.split("\",\"")
                if (parts.size >= 3) {
                    val entry = PasswordEntry(
                        title = parts[0].removePrefix("\""),
                        username = parts[1],
                        password = parts[2],
                        url = if (parts.size > 3) parts[3] else "",
                        notes = if (parts.size > 4) parts[4].removeSuffix("\"") else ""
                    )
                    passwordManager.addPassword(entry)
                    imported++
                }
            }
            
            Toast.makeText(this, "Importate $imported password", Toast.LENGTH_SHORT).show()
            loadPasswords()
        } catch (e: Exception) {
            Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showDeleteDialog(entry: PasswordEntry) {
        AlertDialog.Builder(this)
            .setTitle("Elimina")
            .setMessage("Eliminare \"${entry.title}\"?")
            .setPositiveButton("Elimina") { _, _ ->
                passwordManager.deletePassword(entry.id)
                loadPasswords()
                Toast.makeText(this, "Eliminato", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    
    private fun copyToClipboard(text: String, label: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(this, "$label copiato", Toast.LENGTH_SHORT).show()
    }
}

class PasswordAdapter(
    private val passwords: List<PasswordEntry>,
    private val onEdit: (PasswordEntry) -> Unit,
    private val onDelete: (PasswordEntry) -> Unit,
    private val onCopy: (String, String) -> Unit
) : RecyclerView.Adapter<PasswordAdapter.ViewHolder>() {
    
    private val expandedItems = mutableSetOf<String>()
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.titleText)
        val detailsContainer: View = view.findViewById(R.id.detailsContainer)
        val username: TextView = view.findViewById(R.id.usernameText)
        val password: TextView = view.findViewById(R.id.passwordText)
        val urlContainer: View = view.findViewById(R.id.urlContainer)
        val url: TextView = view.findViewById(R.id.urlText)
        val notes: TextView = view.findViewById(R.id.notesText)
        val showButton: Button = view.findViewById(R.id.showButton)
        val copyUserButton: Button = view.findViewById(R.id.copyUserButton)
        val copyPassButton: Button = view.findViewById(R.id.copyPassButton)
        val copyUrlButton: Button = view.findViewById(R.id.copyUrlButton)
        val editButton: Button = view.findViewById(R.id.editButton)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_password, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = passwords[position]
        val isExpanded = expandedItems.contains(entry.id)
        
        holder.title.text = entry.title
        holder.detailsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
        
        holder.title.setOnClickListener {
            if (expandedItems.contains(entry.id)) {
                expandedItems.remove(entry.id)
            } else {
                expandedItems.add(entry.id)
            }
            notifyItemChanged(position)
        }
        
        holder.username.text = entry.username
        holder.password.text = "••••••••"
        
        if (entry.url.isNotEmpty()) {
            holder.urlContainer.visibility = View.VISIBLE
            holder.url.text = entry.url
        } else {
            holder.urlContainer.visibility = View.GONE
        }
        
        if (entry.notes.isNotEmpty()) {
            holder.notes.visibility = View.VISIBLE
            holder.notes.text = entry.notes
        } else {
            holder.notes.visibility = View.GONE
        }
        
        var shown = false
        holder.showButton.setOnClickListener {
            shown = !shown
            holder.password.text = if (shown) entry.password else "••••••••"
        }
        
        holder.copyUserButton.setOnClickListener { onCopy(entry.username, "Username") }
        holder.copyPassButton.setOnClickListener { onCopy(entry.password, "Password") }
        holder.copyUrlButton.setOnClickListener { onCopy(entry.url, "URL") }
        holder.editButton.setOnClickListener { onEdit(entry) }
        holder.deleteButton.setOnClickListener { onDelete(entry) }
    }
    
    override fun getItemCount() = passwords.size
}
