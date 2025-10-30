package com.example.mindfocus.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mindfocus.data.local.entities.UserEntity
import com.example.mindfocus.data.repository.UserRepository
import com.example.mindfocus.data.local.MindFocusDatabase
import kotlinx.coroutines.launch
import android.app.Application
import androidx.lifecycle.AndroidViewModel

class DatabaseTesterViewModel(app: Application) : AndroidViewModel(app) {
    private val db = MindFocusDatabase.getInstance(app)
    private val userRepo = UserRepository(db)

    suspend fun addDummyUser() {
        userRepo.upsert(
            UserEntity(email = "tester@mindfocus.com", displayName = "Tester")
        )
    }
}

@Composable
fun DatabaseTesterScreen(modifier: Modifier=Modifier) {
    val viewModel: DatabaseTesterViewModel = viewModel()
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("Click on button for data insert.") }

    Scaffold(
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = message, modifier = Modifier.padding(16.dp))
                    Button(onClick = {
                        scope.launch {
                            viewModel.addDummyUser()
                            message = "Insert user successfully"
                        }
                    }) {
                        Text("Add user for test")
                    }
                }
            }
        }
    )
}
