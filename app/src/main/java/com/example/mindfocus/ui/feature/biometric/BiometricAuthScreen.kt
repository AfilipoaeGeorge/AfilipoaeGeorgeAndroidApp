package com.example.mindfocus.ui.feature.biometric

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.mindfocus.R
import com.example.mindfocus.core.auth.BiometricAuthManager
import com.example.mindfocus.core.auth.BiometricResult
import com.example.mindfocus.core.datastore.AuthPreferencesManager
import com.example.mindfocus.data.local.MindFocusDatabase
import com.example.mindfocus.data.local.entities.UserEntity
import com.example.mindfocus.data.repository.UserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun BiometricAuthScreen(
    onAuthSuccess: () -> Unit,
    onUsePassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val authPreferencesManager = remember { AuthPreferencesManager(context) }
    val database = remember { MindFocusDatabase.getInstance(context.applicationContext) }
    val userRepository = remember { UserRepository(database) }
    val biometricAuthManager = remember { BiometricAuthManager(context) }
    
    var isAuthenticating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var allUsers by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
    var selectedUserId by remember { mutableStateOf<Long?>(null) }
    var showUserSelection by remember { mutableStateOf(false) }
    var currentUserId by remember { mutableStateOf<Long?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // Load users and current user ID
    LaunchedEffect(Unit) {
        currentUserId = authPreferencesManager.getCurrentUserId()
        allUsers = userRepository.observeAll().first()
        
        // If current user exists, authenticate directly with that user
        if (currentUserId != null && allUsers.any { it.id == currentUserId }) {
            selectedUserId = currentUserId
            showUserSelection = false
        } else if (allUsers.size == 1) {
            // If only one user, use it directly
            selectedUserId = allUsers.first().id
            showUserSelection = false
        } else if (allUsers.size > 1) {
            // If multiple users and no current user, show selection
            showUserSelection = true
            selectedUserId = allUsers.first().id
        } else {
            // No users, go to login
            onUsePassword()
        }
    }
    
    // Authenticate with selected user
    fun authenticateWithUser(userId: Long) {
        if (activity == null || !biometricAuthManager.isBiometricAvailable() || isAuthenticating) return
        
        isAuthenticating = true
        errorMessage = null
        
        coroutineScope.launch {
            try {
                val result = biometricAuthManager.authenticate(
                    activity = activity,
                    title = context.getString(R.string.biometric_auth_title),
                    subtitle = context.getString(
                        R.string.biometric_auth_authenticate_with,
                        allUsers.find { it.id == userId }?.displayName ?: ""
                    ),
                    negativeButtonText = context.getString(R.string.biometric_auth_cancel)
                )
                
                when (result) {
                    is BiometricResult.Success -> {
                        val user = userRepository.getById(userId)
                        if (user != null) {
                            authPreferencesManager.setLoggedIn(user.id)
                            onAuthSuccess()
                        } else {
                            errorMessage = context.getString(R.string.login_error_user_not_found)
                            isAuthenticating = false
                        }
                    }
                    is BiometricResult.Error -> {
                        if (!result.message.contains("cancel", ignoreCase = true) && 
                            !result.message.contains("negative", ignoreCase = true)) {
                            errorMessage = context.getString(R.string.biometric_auth_error, result.message)
                        }
                        isAuthenticating = false
                    }
                    is BiometricResult.Failed -> {
                        errorMessage = context.getString(R.string.biometric_auth_failed)
                        isAuthenticating = false
                    }
                }
            } catch (e: Exception) {
                errorMessage = context.getString(R.string.biometric_auth_error_generic, e.message ?: "")
                isAuthenticating = false
            }
        }
    }
    
    // Auto-authenticate if single user and not showing selection
    LaunchedEffect(selectedUserId, showUserSelection) {
        if (!showUserSelection && selectedUserId != null && !isAuthenticating) {
            authenticateWithUser(selectedUserId!!)
        }
    }
    
    if (!biometricAuthManager.isBiometricAvailable()) {
        // Biometric not available, go to login
        LaunchedEffect(Unit) {
            onUsePassword()
        }
        return
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colorResource(R.color.darkcharcoal),
                        colorResource(R.color.darkslategray)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (showUserSelection) Arrangement.Top else Arrangement.Center
        ) {
            if (showUserSelection) {
                // User selection screen
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = stringResource(R.string.biometric_auth_select_account),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.amber)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.biometric_auth_select_user),
                    fontSize = 14.sp,
                    color = colorResource(R.color.lightsteelblue),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.midnightblue).copy(alpha = 0.9f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(allUsers) { user ->
                            UserSelectionItem(
                                user = user,
                                isSelected = selectedUserId == user.id,
                                onClick = {
                                    selectedUserId = user.id
                                    showUserSelection = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = {
                        isAuthenticating = false
                        errorMessage = null
                        onUsePassword()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.biometric_auth_use_password),
                        color = colorResource(R.color.skyblue)
                    )
                }
            } else {
                // Authentication screen
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.midnightblue).copy(alpha = 0.9f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp),
                                color = colorResource(R.color.amber),
                                strokeWidth = 4.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = stringResource(R.string.biometric_auth_fingerprint_description),
                                modifier = Modifier.size(64.dp),
                                tint = colorResource(R.color.amber)
                            )
                        }
                        
                        Text(
                            text = stringResource(R.string.biometric_auth_welcome_back),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.amber)
                        )
                        
                        val selectedUser = allUsers.find { it.id == selectedUserId }
                        Text(
                            text = if (selectedUser != null) {
                                context.getString(R.string.biometric_auth_authenticate_with, selectedUser.displayName)
                            } else {
                                stringResource(R.string.biometric_auth_unlock_app)
                            },
                            fontSize = 16.sp,
                            color = colorResource(R.color.lightsteelblue),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                fontSize = 12.sp,
                                color = colorResource(R.color.coralred),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        
                        if (allUsers.size > 1 && !isAuthenticating) {
                            TextButton(
                                onClick = {
                                    isAuthenticating = false
                                    errorMessage = null
                                    showUserSelection = true
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.biometric_auth_select_account),
                                    color = colorResource(R.color.skyblue)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        TextButton(
                            onClick = {
                                isAuthenticating = false
                                errorMessage = null
                                onUsePassword()
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.biometric_auth_use_password),
                                color = colorResource(R.color.skyblue)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserSelectionItem(
    user: UserEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                colorResource(R.color.skyblue).copy(alpha = 0.2f)
            } else {
                colorResource(R.color.darkslateblue).copy(alpha = 0.5f)
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, colorResource(R.color.skyblue))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "User",
                tint = colorResource(R.color.amber),
                modifier = Modifier.size(32.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = user.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.amber)
                )
                if (user.email.isNotEmpty()) {
                    Text(
                        text = user.email,
                        fontSize = 12.sp,
                        color = colorResource(R.color.lightsteelblue)
                    )
                }
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Selected",
                    tint = colorResource(R.color.skyblue),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

