package com.drishti360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drishti360.app.data.models.NGO
import com.drishti360.app.ui.components.NgoCardItem
import com.drishti360.app.ui.theme.LightBackground
import com.drishti360.app.ui.theme.LightSurface
import com.drishti360.app.ui.theme.PrimaryBlue
import com.drishti360.app.ui.theme.TextDarkPrimary
import com.drishti360.app.ui.theme.TextDarkSecondary
import com.drishti360.app.ui.viewmodels.MainViewModel

@Composable
fun NgoListScreen(
    viewModel: MainViewModel,
    isAdmin: Boolean = false,
    onBack: () -> Unit = {},
    onAddNgo: () -> Unit = {},
    onSelectNgo: (NGO) -> Unit
) {
    val ngos by viewModel.ngos.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LightSurface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextDarkPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "NGOs Monitored",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
                Text(
                    text = "${ngos.size} Organizations registered in system",
                    fontSize = 11.sp,
                    color = TextDarkSecondary
                )
            }
            if (isAdmin) {
                androidx.compose.material3.TextButton(onClick = onAddNgo) {
                    Text("+ Add NGO", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            items(ngos, key = { it.id }) { ngo ->
                NgoCardItem(
                    ngo = ngo,
                    onClick = {
                        onSelectNgo(ngo)
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
