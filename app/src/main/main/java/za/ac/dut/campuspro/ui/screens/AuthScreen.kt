@file:OptIn(ExperimentalMaterial3Api::class)

package za.ac.dut.campuspro.ui.screens

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import za.ac.dut.campuspro.data.Direction
import za.ac.dut.campuspro.data.Role
import za.ac.dut.campuspro.domain.Validators
import za.ac.dut.campuspro.ui.components.ChoiceRow
import za.ac.dut.campuspro.ui.components.MessageBanner
import za.ac.dut.campuspro.ui.components.ResidencePickerField
import za.ac.dut.campuspro.ui.components.SectionCard
import za.ac.dut.campuspro.ui.theme.Ink
import za.ac.dut.campuspro.ui.viewmodel.AppViewModelProvider
import za.ac.dut.campuspro.ui.viewmodel.AuthViewModel

/** Login and registration for students and drivers. */
@Composable
fun AuthScreen(
    initialMode: String,
    initialRole: Role,
    onBack: () -> Unit,
    onLoggedIn: (Role) -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    var isLogin by rememberSaveable { mutableStateOf(initialMode == "login") }
    var role by rememberSaveable { mutableStateOf(initialRole) }

    // shared fields
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    // registration fields
    var firstName by rememberSaveable { mutableStateOf("") }
    var surname by rememberSaveable { mutableStateOf("") }
    var residence by rememberSaveable { mutableStateOf<String?>(null) }
    var direction by rememberSaveable { mutableStateOf<Direction?>(null) }
    var busNumber by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }

    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var isError by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun show(text: String?, error: Boolean) {
        message = text
        isError = error
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Campus Pro", fontWeight = FontWeight.Black) },
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionCard {
                Text(
                    if (isLogin) "Welcome back" else "Create your account",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "Choose your account type and continue.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TabRow(
                    selectedTabIndex = if (isLogin) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Tab(selected = isLogin, onClick = { isLogin = true; show(null, false) },
                        text = { Text("Login", fontWeight = FontWeight.Bold) })
                    Tab(selected = !isLogin, onClick = { isLogin = false; show(null, false) },
                        text = { Text("Register", fontWeight = FontWeight.Bold) })
                }

                ChoiceRow(
                    options = listOf(Role.STUDENT to "Student", Role.DRIVER to "Bus Driver"),
                    selected = role,
                    onSelect = { role = it; show(null, false) }
                )
                Spacer(Modifier.height(8.dp))

                if (!isLogin) {
                    OutlinedTextField(
                        value = firstName, onValueChange = { firstName = it },
                        label = { Text("First name") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = surname, onValueChange = { surname = it },
                        label = { Text("Surname") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text(if (role == Role.STUDENT) "Student email" else "Driver email") },
                    placeholder = {
                        Text(if (role == Role.STUDENT) "21234567@dut4life.ac.za" else "driver@dut.ac.za")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password, onValueChange = { if (it.length <= 11) password = it },
                    label = { Text("Password") },
                    placeholder = { Text("\$\$Dut123456") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = { Text(Validators.PASSWORD_HINT) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isLogin && role == Role.STUDENT) {
                    Spacer(Modifier.height(8.dp))
                    ResidencePickerField(
                        value = residence,
                        onSelected = { residence = it },
                        label = "Residence"
                    )
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
                    OutlinedTextField(
                        value = direction?.destination ?: "",
                        onValueChange = {}, readOnly = true, singleLine = true,
                        label = { Text("Destination") },
                        placeholder = { Text("Selected automatically") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }

                if (!isLogin && role == Role.DRIVER) {
                    OutlinedTextField(
                        value = busNumber, onValueChange = { busNumber = it },
                        label = { Text("Bus number") }, placeholder = { Text("e.g. B1033") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone, onValueChange = { if (it.length <= 10) phone = it },
                        label = { Text("Driver phone number") }, placeholder = { Text("e.g. 0821234567") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        supportingText = { Text("Used for the trip; location is only shared while a trip is active.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        busy = true
                        show(null, false)
                        if (isLogin) {
                            viewModel.login(email, password, role) { error ->
                                busy = false
                                if (error == null) onLoggedIn(role) else show(error, true)
                            }
                        } else {
                            val onRegistered: (String?) -> Unit = { error ->
                                busy = false
                                if (error == null) {
                                    isLogin = true
                                    password = ""
                                    show("Account created successfully. You can now log in.", false)
                                } else {
                                    show(error, true)
                                }
                            }
                            if (role == Role.STUDENT) {
                                viewModel.registerStudent(firstName, surname, email, password, residence, direction, onRegistered)
                            } else {
                                viewModel.registerDriver(firstName, surname, email, password, busNumber, phone, onRegistered)
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        when {
                            busy -> "Please wait…"
                            isLogin -> "Access Campus Pro"
                            else -> "Create account"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                MessageBanner(message, isError)

                TextButton(
                    onClick = { isLogin = !isLogin; show(null, false) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isLogin) "Create an account" else "Already have an account? Login", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
