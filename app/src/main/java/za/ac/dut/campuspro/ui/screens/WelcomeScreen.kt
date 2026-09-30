package za.ac.dut.campuspro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import za.ac.dut.campuspro.data.Role
import za.ac.dut.campuspro.ui.components.LiveBadge
import za.ac.dut.campuspro.ui.components.SectionCard
import za.ac.dut.campuspro.ui.theme.Ink
import za.ac.dut.campuspro.ui.theme.Orange
import za.ac.dut.campuspro.ui.theme.OrangeTint

@Composable
fun WelcomeScreen(onOpenAuth: (mode: String, role: Role) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Dark strip behind the status bar so its light icons stay visible
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Ink)
                    .windowInsetsPadding(WindowInsets.statusBars)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            LiveBadge("SMART UNIVERSITY TRANSPORT")
            Spacer(Modifier.height(10.dp))
            Text(
                buildAnnotatedString {
                    append("CAMPUS ")
                    withStyle(SpanStyle(color = Orange)) { append("PRO") }
                },
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 48.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Know where your DUT shuttle is, when it will arrive, and get updates straight from the driver.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))

            BusIllustration()

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onOpenAuth("register", Role.STUDENT) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Register as Student", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onOpenAuth("register", Role.DRIVER) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Driver Portal", fontWeight = FontWeight.Bold, color = Ink) }
                OutlinedButton(
                    onClick = { onOpenAuth("login", Role.STUDENT) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Login", fontWeight = FontWeight.Bold, color = Ink) }
            }

            Spacer(Modifier.height(28.dp))
            Text("One app for the entire trip", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            FeatureCard("01", "Live tracking", "See the active bus on your route and its estimated arrival time.")
            FeatureCard("02", "Arrival alerts", "Get a notification when the bus is arriving soon and when it has arrived.")
            FeatureCard("03", "Driver updates", "Drivers post delays, route problems and other trip changes in real time.")
        }
    }
}

@Composable
private fun FeatureCard(number: String, title: String, body: String) {
    SectionCard(Modifier.padding(bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(OrangeTint, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) { Text(number, color = Orange, fontWeight = FontWeight.Black) }
            Spacer(Modifier.size(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Simple bus drawn with Canvas (no image assets needed). */
@Composable
private fun BusIllustration() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Ink)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // orange glow in the corner
            drawCircle(Orange, radius = size.minDimension * 0.55f, center = Offset(size.width, 0f))
            val busLeft = size.width * 0.1f
            val busTop = size.height * 0.34f
            val busWidth = size.width * 0.8f
            val busHeight = size.height * 0.36f
            drawRoundRect(
                Color.White, Offset(busLeft, busTop), Size(busWidth, busHeight),
                cornerRadius = CornerRadius(18.dp.toPx())
            )
            drawRoundRect(
                Orange, Offset(busLeft, busTop), Size(busWidth, busHeight),
                cornerRadius = CornerRadius(18.dp.toPx()), style = Stroke(width = 6.dp.toPx())
            )
            // windows
            val windowTop = busTop + busHeight * 0.18f
            val windowHeight = busHeight * 0.38f
            for (i in 0 until 4) {
                val w = busWidth * 0.16f
                drawRoundRect(
                    Color(0xFF222222),
                    Offset(busLeft + busWidth * 0.12f + i * (w + busWidth * 0.04f), windowTop),
                    Size(w, windowHeight), cornerRadius = CornerRadius(6.dp.toPx())
                )
            }
            // wheels
            val wheelY = busTop + busHeight
            drawCircle(Color(0xFF222222), radius = 16.dp.toPx(), center = Offset(busLeft + busWidth * 0.2f, wheelY))
            drawCircle(Color(0xFF222222), radius = 16.dp.toPx(), center = Offset(busLeft + busWidth * 0.8f, wheelY))
        }
    }
}
