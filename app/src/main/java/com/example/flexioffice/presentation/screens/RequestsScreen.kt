package com.example.flexioffice.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.flexioffice.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.assignment_24px),
                contentDescription = "Buchungsanfragen Icon",
                modifier = Modifier.padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(text = "Buchungsanfragen", style = MaterialTheme.typography.headlineMedium)
        }

        Text(
            text = "Genehmigen oder lehnen Sie Buchungsanfragen ab",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        // Beispiel-Anfragen (später durch echte Daten ersetzen)
        val sampleRequests =
            listOf(
                "Max Mustermann - Meetingraum A - 28.07.2025",
                "Anna Schmidt - Telefonkabine 1 - 29.07.2025",
                "Tom Weber - Flexipler Platz - 30.07.2025",
            )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sampleRequests) { request ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = request,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = { /* TODO: Genehmigen */ },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                                Text("Genehmigen")
                            }

                            OutlinedButton(
                                onClick = { /* TODO: Ablehnen */ },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.cancel_24px),
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                                Text("Ablehnen")
                            }
                        }
                    }
                }
            }
        }
    }
}
