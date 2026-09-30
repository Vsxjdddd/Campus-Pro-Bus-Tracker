@file:OptIn(ExperimentalMaterial3Api::class)

package za.ac.dut.campuspro.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import za.ac.dut.campuspro.data.Direction
import za.ac.dut.campuspro.data.TripSession
import za.ac.dut.campuspro.data.User
import za.ac.dut.campuspro.domain.TripMath
import za.ac.dut.campuspro.ui.components.AlertRow
import za.ac.dut.campuspro.ui.components.ChoiceRow
import za.ac.dut.campuspro.ui.components.InfoTile
import za.ac.dut.campuspro.ui.components.LiveBadge
import za.ac.dut.campuspro.ui.components.MessageBanner
import za.ac.dut.campuspro.ui.components.ResidencePickerField
import za.ac.dut.campuspro.ui.components.SectionCard
import za.ac.dut.campuspro.ui.theme.Ink
import za.ac.dut.campuspro.ui.theme.Orange
import za.ac.dut.campuspro.ui.viewmodel.AppViewModelProvider
import za.ac.dut.campuspro.ui.viewmodel.StudentViewModel

@Composable
fun StudentHomeScreen(
    onTrack: (tripId: Long) -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: StudentViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val trips by viewModel.availableTrips.collectAsStateWithLifecycle()
    val otherTrips by viewModel.otherTrips.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val syncError by viewModel.syncError.collectAsStateWithLifecycle()
    var editingRoute by remember { mutableStateOf(false) }
    var routeError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Campus Pro", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Ink, titleContentColor = Color.White, actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.logout(); onLoggedOut() }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Log out")
                    }
                }
            )
        }
    ) { padding ->
        val student = user ?: return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WelcomeBanner(student)
            MessageBanner(syncError?.let { "Offline: $it Retrying…" }, isError = true)
            MessageBanner(routeError, isError = true)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoTile("Pickup point", student.direction?.pickup ?: "-", Modifier.weight(1f))
                InfoTile("Destination", student.direction?.destination ?: "-", Modifier.weight(1f))
            }
            SectionCard {
                Text("Your residence", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(student.residence ?: "-", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { editingRoute = true }, contentPadding = PaddingValues(0.dp)) {
                    Text("Change route", color = Orange, fontWeight = FontWeight.Bold)
                }
            }

            Text("Available transport", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            Text(
                "Only live trips for ${student.direction?.label ?: "your route"} at your residence are shown.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (trips.isEmpty()) {
                SectionCard {
                    Text("No active bus for your route yet.", fontWeight = FontWeight.Bold)
                    Text(
                        "When a driver starts a ${student.direction?.label ?: ""} trip for your residence it will appear here automatically.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                trips.forEach { trip -> TripRow(trip, now, onTrack = { onTrack(trip.id) }) }
            }

            if (otherTrips.isNotEmpty()) {
                Text("Other live buses", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                Text(
                    "Running now on other residences or directions. Tap Change route above if one of these is yours.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                otherTrips.forEach { trip -> TripRow(trip, now, onTrack = { onTrack(trip.id) }) }
            }

            Text("Transport notifications", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            SectionCard {
                if (alerts.isEmpty()) {
                    Text("No notifications yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    alerts.forEach { AlertRow(it) }
                }
            }
        }

        if (editingRoute) {
            ChangeRouteDialog(
                student = student,
                onDismiss = { editingRoute = false },
                onSave = { residence, direction ->
                    editingRoute = false
                    viewModel.changeRoute(residence, direction) { error -> routeError = error }
                }
            )
        }
    }
}

@Composable
private fun WelcomeBanner(user: User) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Ink),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(7.dp)
                    .fillMaxHeight()
                    .background(Orange)
            )
            Column(Modifier.padding(20.dp)) {
                Text("Welcome, ${user.fullName}", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(user.email, color = Color(0xFFCCCCCC))
            }
        }
    }
}

@Composable
private fun TripRow(trip: TripSession, now: Long, onTrack: () -> Unit) {
    val snapshot = TripMath.snapshot(trip.startedAt, trip.durationMinutes, now)
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(Orange)
            )
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bus ${trip.busNumber}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    LiveBadge()
                }
                Text(trip.direction.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(trip.residence, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Driver: ${trip.driverName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${snapshot.status.label} · ETA ${TripMath.formatEtaShort(snapshot.remainingMs)}",
                    fontWeight = FontWeight.Bold, color = Orange
                )
                Button(
                    onClick = onTrack,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) { Text("Track bus", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun ChangeRouteDialog(
    student: User,
    onDismiss: () -> Unit,
    onSave: (String, Direction) -> Unit
) {
    var residence by remember { mutableStateOf(student.residence) }
    var direction by remember { mutableStateOf(student.direction) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change route") },
        text = {
            Column {
                ResidencePickerField(value = residence, onSelected = { residence = it })
                Spacer(Modifier.height(12.dp))
                Text("Pickup point", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                ChoiceRow(
                    options = listOf(
                        Direction.RESIDENCE_TO_CAMPUS to "Residence",
                        Direction.CAMPUS_TO_RESIDENCE to "Campus"
                    ),
                    selected = direction,
                    onSelect = { direction = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = residence != null && direction != null,
                onClick = {
                    val r = residence
                    val d = direction
                    if (r != null && d != null) onSave(r, d)
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
