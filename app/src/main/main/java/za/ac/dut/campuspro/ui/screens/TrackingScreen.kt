@file:OptIn(ExperimentalMaterial3Api::class)

package za.ac.dut.campuspro.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import za.ac.dut.campuspro.data.TripSession
import za.ac.dut.campuspro.domain.TripMath
import za.ac.dut.campuspro.ui.components.AlertRow
import za.ac.dut.campuspro.ui.components.LiveBadge
import za.ac.dut.campuspro.ui.components.SectionCard
import za.ac.dut.campuspro.ui.theme.Amber
import za.ac.dut.campuspro.ui.theme.AmberTint
import za.ac.dut.campuspro.ui.theme.Green
import za.ac.dut.campuspro.ui.theme.GreenTint
import za.ac.dut.campuspro.ui.theme.Grey
import za.ac.dut.campuspro.ui.theme.Ink
import za.ac.dut.campuspro.ui.theme.Orange
import za.ac.dut.campuspro.ui.theme.OrangeTint
import za.ac.dut.campuspro.ui.viewmodel.AppViewModelProvider
import za.ac.dut.campuspro.ui.viewmodel.TrackingViewModel

/** The app's hub: route map with moving bus, ETA countdown and status. */
@Composable
fun TrackingScreen(
    onBack: () -> Unit,
    viewModel: TrackingViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Live tracking", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Ink, titleContentColor = Color.White, navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val trip = state.trip
        val snapshot = state.snapshot
        if (trip == null || snapshot == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (state.notFound) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("This trip has already ended", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "The bus is no longer live. Go back to see the buses running now.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onBack) { Text("Back to buses", fontWeight = FontWeight.Bold) }
                    }
                } else {
                    CircularProgressIndicator()
                }
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Bus ${trip.busNumber}", style = MaterialTheme.typography.headlineMedium)
                    Text("${trip.direction.label} · ${trip.residence}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (state.isLive) LiveBadge("LIVE SESSION") else LiveBadge("TRIP COMPLETED", Grey)
            }

            StatusCard(snapshot)
            RouteMap(trip, snapshot.progress)

            SectionCard {
                Text("Driver", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(trip.driverName, style = MaterialTheme.typography.titleMedium)
                if (trip.driverPhone.isNotBlank()) Text(trip.driverPhone, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SectionCard {
                Text("Route updates", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                if (state.alerts.isEmpty()) {
                    Text("No updates for this route yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.alerts.forEach { AlertRow(it) }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(snapshot: TripMath.Snapshot) {
    val (accent, tint) = when (snapshot.status) {
        TripMath.Status.EN_ROUTE -> Orange to OrangeTint
        TripMath.Status.ARRIVING_SOON -> Amber to AmberTint
        TripMath.Status.ARRIVED -> Green to GreenTint
    }
    val background by animateColorAsState(tint, label = "statusBackground")
    val progress by animateFloatAsState(snapshot.progress, tween(900), label = "progress")

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(snapshot.status.label, color = accent, style = MaterialTheme.typography.titleLarge)
                Text(snapshot.status.description, color = Ink)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (snapshot.status == TripMath.Status.ARRIVED) "0:00" else TripMath.formatCountdown(snapshot.remainingMs),
                    fontSize = 36.sp, fontWeight = FontWeight.Black, color = accent
                )
                Text("ETA", style = MaterialTheme.typography.labelMedium, color = Ink)
            }
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { progress },
            color = accent,
            trackColor = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.height(6.dp))
        Text("Trip progress: ${(snapshot.progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = Ink)
    }
}

/**
 * Simplified route diagram drawn with Canvas: pickup point, destination and a
 * bus marker that moves along a curved road according to trip progress.
 * Production would replace this with the Google Maps SDK.
 */
@Composable
private fun RouteMap(trip: TripSession, progress: Float) {
    val animated by animateFloatAsState(progress, tween(1_000), label = "busPosition")
    Box(
        Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEFEDEA))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val p0 = Offset(size.width * 0.12f, size.height * 0.78f)
            val p1 = Offset(size.width * 0.45f, size.height * 0.95f)
            val p2 = Offset(size.width * 0.50f, size.height * 0.10f)
            val p3 = Offset(size.width * 0.88f, size.height * 0.24f)

            drawMapGrid()

            val road = Path().apply {
                moveTo(p0.x, p0.y)
                cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
            }
            drawPath(road, Color(0xFF555555), style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round))
            drawPath(
                road, Color.White,
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f)))
            )

            // travelled part of the route in orange
            val travelled = Path().apply {
                moveTo(p0.x, p0.y)
                val steps = 60
                for (i in 1..steps) {
                    val t = animated * i / steps
                    val point = bezier(p0, p1, p2, p3, t)
                    lineTo(point.x, point.y)
                }
            }
            drawPath(travelled, Orange, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))

            drawStop(p0, Ink)
            drawStop(p3, Orange)

            val bus = bezier(p0, p1, p2, p3, animated)
            val busSize = Size(44.dp.toPx(), 26.dp.toPx())
            drawRoundRect(
                Ink, Offset(bus.x - busSize.width / 2, bus.y - busSize.height / 2), busSize,
                cornerRadius = CornerRadius(7.dp.toPx())
            )
            drawRoundRect(
                Orange, Offset(bus.x - busSize.width / 2, bus.y - busSize.height / 2), busSize,
                cornerRadius = CornerRadius(7.dp.toPx()), style = Stroke(width = 3.dp.toPx())
            )
        }
        MapLabel(trip.direction.pickup.uppercase() + " (pickup)", Modifier.align(Alignment.BottomStart))
        MapLabel(trip.direction.destination.uppercase(), Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun MapLabel(text: String, modifier: Modifier) {
    Text(
        text,
        fontWeight = FontWeight.Black,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .padding(10.dp)
            .background(Color.White, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    )
}

private fun DrawScope.drawMapGrid() {
    val step = 40.dp.toPx()
    var x = 0f
    while (x < size.width) {
        drawLine(Color(0xFFE2DFDA), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(Color(0xFFE2DFDA), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += step
    }
}

private fun DrawScope.drawStop(center: Offset, color: Color) {
    drawCircle(Color.White, radius = 13.dp.toPx(), center = center)
    drawCircle(color, radius = 9.dp.toPx(), center = center)
}

/** Point on a cubic Bézier curve at t (0..1). */
private fun bezier(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val u = 1 - t
    val a = u * u * u
    val b = 3 * u * u * t
    val c = 3 * u * t * t
    val d = t * t * t
    return Offset(
        a * p0.x + b * p1.x + c * p2.x + d * p3.x,
        a * p0.y + b * p1.y + c * p2.y + d * p3.y
    )
}
