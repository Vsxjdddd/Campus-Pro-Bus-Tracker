@file:OptIn(ExperimentalMaterial3Api::class)

package za.ac.dut.campuspro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import za.ac.dut.campuspro.data.Direction
import za.ac.dut.campuspro.domain.TripMath
import za.ac.dut.campuspro.domain.Validators
import za.ac.dut.campuspro.ui.components.ChoiceRow
import za.ac.dut.campuspro.ui.components.InfoTile
import za.ac.dut.campuspro.ui.components.LiveBadge
import za.ac.dut.campuspro.ui.components.MessageBanner
import za.ac.dut.campuspro.ui.components.ResidencePickerField
import za.ac.dut.campuspro.ui.components.SectionCard
import za.ac.dut.campuspro.ui.theme.Green
import za.ac.dut.campuspro.ui.theme.Grey
import za.ac.dut.campuspro.ui.theme.Ink
import za.ac.dut.campuspro.ui.viewmodel.AppViewModelProvider
import za.ac.dut.campuspro.ui.viewmodel.DriverViewModel

@Composable
fun DriverHomeScreen(
    onLoggedOut: () -> Unit,
    viewModel: DriverViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val trip by viewModel.activeTrip.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()

    var direction by rememberSaveable { mutableStateOf(Direction.RESIDENCE_TO_CAMPUS) }
    var residence by rememberSaveable { mutableStateOf<String?>(null) }
    var duration by rememberSaveable { mutableStateOf("5") }
    var tripMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var tripError by rememberSaveable { mutableStateOf(false) }

    var updateText by rememberSaveable { mutableStateOf("") }
    var updateMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var updateError by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val syncError by viewModel.syncError.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Driver Portal", fontWeight = FontWeight.Black) },
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
        val driver = user ?: return@Scaffold
        val activeTrip = trip
        val snapshot = activeTrip?.let { TripMath.snapshot(it.startedAt, it.durationMinutes, now) }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Welcome, ${driver.fullName}", style = MaterialTheme.typography.headlineMedium)
            Text(driver.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MessageBanner(syncError?.let { "Offline: $it Retrying…" }, isError = true)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoTile("Bus number", driver.busNumber ?: "-", Modifier.weight(1f))
                InfoTile("Driver phone", driver.phone ?: "-", Modifier.weight(1f))
            }
            InfoTile(
                label = "Session",
                value = if (activeTrip != null && snapshot != null) {
                    "ONLINE · about ${TripMath.formatEtaShort(snapshot.remainingMs)} remaining"
                } else {
                    "Offline · no trip is active"
                },
                valueColor = if (activeTrip != null) Green else Grey
            )

            // ---------------------------------------------------- Trip control
            SectionCard {
                if (activeTrip == null || snapshot == null) {
                    Text("Start transport session", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Choose the trip you are operating. Location is shared only while the trip is active.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Trip direction", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    ChoiceRow(
                        options = listOf(
                            Direction.RESIDENCE_TO_CAMPUS to "Res → Campus",
                            Direction.CAMPUS_TO_RESIDENCE to "Campus → Res"
                        ),
                        selected = direction,
                        onSelect = { direction = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    ResidencePickerField(value = residence, onSelected = { residence = it }, label = "Residence / route")
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { if (it.length <= 3) duration = it.filter(Char::isDigit) },
                        label = { Text("Expected trip duration (minutes)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text("Tip: use 1 minute for a quick demo.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    Button(
                        onClick = {
                            val durationError = Validators.tripDurationError(duration)
                            if (durationError != null) {
                                tripError = true
                                tripMessage = durationError
                            } else {
                                busy = true
                                viewModel.startTrip(residence, direction, duration.toInt()) { error ->
                                    busy = false
                                    tripError = error != null
                                    tripMessage = error ?: "Trip started. Students on this route can now track Bus ${driver.busNumber}."
                                }
                            }
                        },
                        enabled = !busy,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) { Text("Start trip & share location", fontWeight = FontWeight.Bold) }
                } else {
                    Row {
                        Text("Trip in progress", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        LiveBadge()
                    }
                    Text("${activeTrip.direction.label} · ${activeTrip.residence}")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${snapshot.status.label} · ${TripMath.formatCountdown(snapshot.remainingMs)} remaining",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { snapshot.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            busy = true
                            viewModel.endTrip { error ->
                                busy = false
                                tripError = error != null
                                tripMessage = error ?: "Trip completed. Location sharing has stopped."
                            }
                        },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .height(52.dp)
                    ) { Text("End trip & stop location sharing", fontWeight = FontWeight.Bold) }
                }
                MessageBanner(tripMessage, tripError)
            }

            // --------------------------------------------------- Driver update
            SectionCard {
                Text("Driver update", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Send a live message to students on your current route.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = updateText,
                    onValueChange = { if (it.length <= 160) updateText = it },
                    placeholder = { Text("e.g. Traffic on the N2, about 10 minutes late") },
                    supportingText = { Text("${updateText.length}/160") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                Button(
                    onClick = {
                        busy = true
                        viewModel.sendUpdate(updateText) { error ->
                            busy = false
                            updateError = error != null
                            updateMessage = error ?: "Update sent to students on the active route."
                            if (error == null) updateText = ""
                        }
                    },
                    enabled = !busy,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Send update", fontWeight = FontWeight.Bold) }
                MessageBanner(updateMessage, updateError)
            }

            // ------------------------------------------------- Trip validation
            SectionCard {
                Text("Trip validation", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (activeTrip != null) {
                        "Active trip validated for Bus ${activeTrip.busNumber}. Location sharing is on and " +
                            "the trip ends automatically after ${activeTrip.durationMinutes} min."
                    } else {
                        "No active trip to validate."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
