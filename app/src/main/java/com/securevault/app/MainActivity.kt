package com.securevault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.securevault.app.security.CryptoManager
import com.securevault.app.ui.VaultItem
import com.securevault.app.ui.VaultViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF00BFA5))) {
                SecureVaultApp()
            }
        }
    }
}

@Composable
fun SecureVaultApp(vm: VaultViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    if (state.isLocked) {
        LockScreen(onUnlock = { password, salt -> vm.unlock(password, salt) }, error = state.error)
    } else {
        VaultScreen(vm)
    }
}

@Composable
fun LockScreen(onUnlock: (String, ByteArray) -> Unit, error: String?) {
    val salt = remember { "SecureVaultFixedSalt2024".toByteArray() }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF00BFA5), modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(16.dp))
        Text("SecureVault", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("خزنة كلمات السر", fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("كلمة السر الرئيسية") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = Color.Red, fontSize = 13.sp)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { if (password.length >= 4) onUnlock(password, salt) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("فتح الخزنة", fontSize = 16.sp)
        }
    }
}

@Composable
fun VaultScreen(vm: VaultViewModel) {
    val state by vm.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("خزنتي") },
                actions = {
                    IconButton(onClick = { vm.lock() }) {
                        Icon(Icons.Filled.Lock, contentDescription = "قفل")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF161B22))
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFF0D1117))) {
            if (state.entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد كلمات سر محفوظة", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.entries) { entry ->
                        VaultItemCard(entry, onDelete = { vm.deleteEntry(entry) })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPasswordDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, user, pass ->
                vm.addEntry(title, user, pass)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun VaultItemCard(entry: VaultItem, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(entry.username, color = Color.Gray, fontSize = 13.sp)
                Text("••••••••", color = Color(0xFF00BFA5), fontSize = 14.sp)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = Color.Red)
            }
        }
    }
}

@Composable
fun AddPasswordDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة كلمة سر") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("الاسم") }, singleLine = true)
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("اسم المستخدم") }, singleLine = true)
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("كلمة السر") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { password = CryptoManager.generatePassword(20) }) {
                    Text("🎲 توليد كلمة سر قوية")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank() && password.isNotBlank()) {
                    onSave(title, username, password)
                }
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
