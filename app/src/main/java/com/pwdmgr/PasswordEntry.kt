package com.pwdmgr

data class PasswordEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val username: String,
    val password: String,
    val url: String = "",
    val notes: String = ""
)
