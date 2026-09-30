package za.ac.dut.campuspro.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import za.ac.dut.campuspro.data.Residences
import za.ac.dut.campuspro.data.TransportAlert
import za.ac.dut.campuspro.ui.theme.Green
import za.ac.dut.campuspro.ui.theme.GreenTint
import za.ac.dut.campuspro.ui.theme.Orange
import za.ac.dut.campuspro.ui.theme.Red
import za.ac.dut.campuspro.ui.theme.RedTint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** White rounded card used throughout the app. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

/** Small label/value pair, e.g. "Bus Number" / "B1033". */
@Composable
fun InfoTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.Unspecified) {
    SectionCard(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor)
    }
}

/** Green success or red error message. */
@Composable
fun MessageBanner(text: String?, isError: Boolean, modifier: Modifier = Modifier) {
    if (text.isNullOrBlank()) return
    Text(
        text = text,
        color = if (isError) Red else Green,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .background(if (isError) RedTint else GreenTint, RoundedCornerShape(8.dp))
            .padding(12.dp)
    )
}

/** A row of equal-width toggle buttons (used for role and direction choices). */
@Composable
fun <T> ChoiceRow(
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            OutlinedButton(
                onClick = { onSelect(value) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            ) {
                Text(label, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Read-only field that opens a searchable list of residences. */
@Composable
fun ResidencePickerField(
    value: String?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Residence"
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value.orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            placeholder = { Text("Select a residence") },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        // Transparent overlay so a tap anywhere on the field opens the picker
        Box(
            Modifier
                .matchParentSize()
                .clickable { open = true }
        )
    }
    if (open) {
        ResidencePickerDialog(
            onDismiss = { open = false },
            onSelected = {
                onSelected(it)
                open = false
            }
        )
    }
}

@Composable
private fun ResidencePickerDialog(onDismiss: () -> Unit, onSelected: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        Residences.ALL.filter { it.contains(query.trim(), ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Select residence") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search ${Residences.ALL.size} residences") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(filtered) { name ->
                        Text(
                            text = name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelected(name) }
                                .padding(horizontal = 4.dp, vertical = 12.dp)
                        )
                        HorizontalDivider()
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                "No residence matches \"$query\".",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}

/** Green "LIVE" pill with a dot. */
@Composable
fun LiveBadge(text: String = "LIVE", color: Color = Green) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(9.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.size(6.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
    }
}

/** One notification row with an orange left edge. */
@Composable
fun AlertRow(alert: TransportAlert) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(IntrinsicSize.Min)
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Orange, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
        )
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(alert.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(alert.message, style = MaterialTheme.typography.bodyMedium)
            Text(
                formatTime(alert.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
