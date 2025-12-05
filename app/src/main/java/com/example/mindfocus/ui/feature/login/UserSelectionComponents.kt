package com.example.mindfocus.ui.feature.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindfocus.R
import com.example.mindfocus.data.local.entities.UserEntity

@Composable
fun UserSelectionList(
    users: List<UserEntity>,
    selectedUserId: Long?,
    onUserSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.biometric_auth_select_account),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colorResource(R.color.lightsteelblue),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.midnightblue).copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((users.size * 90).coerceAtMost(400).dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(users) { user ->
                    UserSelectionItem(
                        user = user,
                        isSelected = selectedUserId == user.id,
                        onClick = { onUserSelected(user.id) }
                    )
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
            BorderStroke(2.dp, colorResource(R.color.skyblue))
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

