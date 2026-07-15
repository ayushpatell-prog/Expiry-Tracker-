package com.example.expirytracker1.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import android.widget.Toast
import java.io.File
import com.example.expirytracker1.ui.theme.DarkGreenPrimary
import com.example.expirytracker1.ui.theme.ExpiryTracker1Theme
import com.example.expirytracker1.ui.theme.SageGreenBackground
import com.example.expirytracker1.ui.theme.TextGray
import com.example.expirytracker1.auth.FirebaseAuthManager

@Composable
fun ProfileScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showImagePickerDialog by remember { mutableStateOf(false) }
    
    // Image Picking Logic
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            FirebaseAuthManager.updateProfile(photoUri = it, onSuccess = {
                Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
            }, onFailure = { err ->
                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
            })
        }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempImageUri?.let { uri ->
                FirebaseAuthManager.updateProfile(photoUri = uri, onSuccess = {
                    Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                }, onFailure = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                })
            }
        }
    }

    fun createImageUri(): Uri {
        val directory = File(context.externalCacheDir, "camera_images")
        if (!directory.exists()) directory.mkdirs()
        val file = File(directory, "profile_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "com.example.expirytracker1.fileprovider", file)
    }

    Scaffold(
        bottomBar = { ProfileBottomNavigation(onNavigate) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                ProfileHeaderCard(onEditPhotoClick = { showImagePickerDialog = true })
            }

            item {
                SettingsGroup(title = "ACCOUNT SETTINGS") {
                    SettingsItem(
                        icon = Icons.Outlined.Person, 
                        label = "Edit Profile",
                        onClick = { showEditNameDialog = true }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.Lock, 
                        label = "Change Password",
                        onClick = { showChangePasswordDialog = true }
                    )
                }
            }

            item {
                SettingsGroup(title = "APP PREFERENCES") {
                    SettingsItem(
                        icon = Icons.Outlined.Notifications, 
                        label = "Notification Settings", 
                        onClick = { onNavigate("SETTINGS") }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.DarkMode, 
                        label = "Dark Mode",
                        showArrow = false,
                        trailingContent = {
                            Switch(
                                checked = darkMode,
                                onCheckedChange = onDarkModeChange,
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = Color(0xFF388E3C)
                                )
                            )
                        }
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        FirebaseAuthManager.logout()
                        onNavigate("LOGIN")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (MaterialTheme.colorScheme.primary == SageGreenBackground) Color(0xFF3D1F1F) else Color(0xFFFFEBEE)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Out", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // --- Dialogs ---

    if (showEditNameDialog) {
        var newName by remember { mutableStateOf(FirebaseAuthManager.currentUser()?.displayName ?: "") }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Name") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Full Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    FirebaseAuthManager.updateProfile(fullName = newName, onSuccess = {
                        showEditNameDialog = false
                        Toast.makeText(context, "Name updated", Toast.LENGTH_SHORT).show()
                    }, onFailure = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    })
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showChangePasswordDialog) {
        var currentPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Change Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPassword.length < 6) {
                        Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    FirebaseAuthManager.changePassword(currentPassword, newPassword, onSuccess = {
                        showChangePasswordDialog = false
                        Toast.makeText(context, "Password changed successfully", Toast.LENGTH_SHORT).show()
                    }, onFailure = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    })
                }) { Text("Change") }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showImagePickerDialog) {
        AlertDialog(
            onDismissRequest = { showImagePickerDialog = false },
            title = { Text("Profile Photo") },
            text = { Text("Choose an option to update your photo.") },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showImagePickerDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Gallery")
                    }
                    Button(
                        onClick = {
                            showImagePickerDialog = false
                            val uri = createImageUri()
                            tempImageUri = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Camera")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showImagePickerDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProfileHeaderCard(onEditPhotoClick: () -> Unit = {}) {

    val user = FirebaseAuthManager.currentUser()

    val name = user?.displayName ?: "User"
    val email = user?.email ?: "No Email"
    val photoUrl = user?.photoUrl

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(contentAlignment = Alignment.BottomEnd) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF81C784).copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp),
                            tint = DarkGreenPrimary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DarkGreenPrimary)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { onEditPhotoClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )
        }
    }
}

@Composable
fun SettingsGroup(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF81C784),
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector, 
    label: String, 
    showArrow: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.onSurface, 
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingContent != null) {
                trailingContent()
                if (showArrow) Spacer(modifier = Modifier.width(12.dp))
            }
            if (showArrow) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextGray)
            }
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp), 
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}

@Composable
fun ProfileBottomNavigation(onNavigate: (String) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = false,
            onClick = { onNavigate("HOME") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Inventory2, contentDescription = "Inventory") },
            label = { Text("Inventory") },
            selected = false,
            onClick = { onNavigate("INVENTORY") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = "AI Assistant") },
            label = { Text("AI Assistant") },
            selected = false,
            onClick = { onNavigate("ASSISTANT") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = false,
            onClick = { onNavigate("SETTINGS") }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ExpiryTracker1Theme(dynamicColor = false) {
        ProfileScreen(darkMode = false, onDarkModeChange = {})
    }
}
