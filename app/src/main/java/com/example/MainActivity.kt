package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.components.AuthDialog
import com.example.ui.components.JarvisHeader
import com.example.ui.components.JarvisNavBar
import com.example.ui.components.JarvisTab
import com.example.ui.screens.*
import com.example.ui.theme.JarvisTheme
import com.example.ui.theme.VoidBackground
import com.example.viewmodel.JarvisViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val accentColorName by viewModel.accentColor.collectAsState()
            val isListening by viewModel.voiceManager.isListening.collectAsState()
            val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsState()

            val currentUser by viewModel.currentUser.collectAsState()
            val authState by viewModel.authState.collectAsState()

            var selectedTab by remember { mutableStateOf(JarvisTab.VOICE) }
            var isAuthDialogOpen by remember { mutableStateOf(false) }

            // Request Audio, Camera, Notifications, Call & SMS permissions
            val permissionsState = rememberMultiplePermissionsState(
                permissions = buildList {
                    add(Manifest.permission.RECORD_AUDIO)
                    add(Manifest.permission.CAMERA)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    add(Manifest.permission.CALL_PHONE)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        add(Manifest.permission.ANSWER_PHONE_CALLS)
                    }
                    add(Manifest.permission.READ_PHONE_STATE)
                    add(Manifest.permission.SEND_SMS)
                }
            )

            LaunchedEffect(Unit) {
                if (!permissionsState.allPermissionsGranted) {
                    permissionsState.launchMultiplePermissionRequest()
                }

                if (intent?.getBooleanExtra("TRIGGER_VOICE_PROMPT", false) == true) {
                    selectedTab = JarvisTab.VOICE
                }
            }

            // Wait for the permission dialog to finish, then start the voice core.
            LaunchedEffect(permissionsState.allPermissionsGranted) {
                if (permissionsState.allPermissionsGranted) {
                    kotlinx.coroutines.delay(500)
                    viewModel.voiceManager.startListening()
                }
            }

            JarvisTheme(themeMode = themeMode, accentColorName = accentColorName) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = VoidBackground,
                    topBar = {
                        JarvisHeader(
                            title = "RJNX JARVIS",
                            subtitle = when (selectedTab) {
                                JarvisTab.VOICE -> "AI Voice Assistant & HUD Core"
                                JarvisTab.CHAT -> "Neural Gemini Chat"
                                JarvisTab.STATUS -> "Diagnostics & Service Control"
                                JarvisTab.SMART -> "Smart Tools & App Launcher"
                                JarvisTab.STUDY -> "Academic Suite & Doubt Solver"
                                JarvisTab.PRODUCTIVITY -> "To-Dos, Habits & Expenses"
                                JarvisTab.UTILITIES -> "QR Tools & Flashlight HUD"
                                JarvisTab.SETTINGS -> "Customization & API Keys"
                            },
                            isListening = isListening,
                            isSpeaking = isSpeaking,
                            userName = currentUser.displayName,
                            userProvider = currentUser.authProvider,
                            onProfileClick = { isAuthDialogOpen = true },
                            onVoiceClick = {
                                selectedTab = JarvisTab.VOICE
                                if (isSpeaking) {
                                    viewModel.voiceManager.stopSpeaking()
                                } else if (isListening) {
                                    viewModel.voiceManager.stopListening()
                                } else {
                                    viewModel.voiceManager.startListening()
                                }
                            }
                        )
                    },
                    bottomBar = {
                        JarvisNavBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            JarvisTab.VOICE -> VoiceAssistantScreen(viewModel = viewModel)
                            JarvisTab.CHAT -> ChatScreen(viewModel = viewModel)
                            JarvisTab.STATUS -> AssistantStatusScreen(viewModel = viewModel)
                            JarvisTab.SMART -> SmartAssistantScreen(viewModel = viewModel)
                            JarvisTab.STUDY -> StudyAssistantScreen(viewModel = viewModel)
                            JarvisTab.PRODUCTIVITY -> ProductivityScreen(viewModel = viewModel)
                            JarvisTab.UTILITIES -> UtilitiesScreen(viewModel = viewModel)
                            JarvisTab.SETTINGS -> SettingsScreen(viewModel = viewModel, onOpenAuthDialog = { isAuthDialogOpen = true })
                        }

                        // Account & Auth Modal Dialog
                        AuthDialog(
                            isOpen = isAuthDialogOpen,
                            onDismiss = { isAuthDialogOpen = false },
                            currentUser = currentUser,
                            authState = authState,
                            onSignInEmail = { email, pass -> viewModel.signInWithEmail(email, pass) },
                            onSignUpEmail = { email, pass, name -> viewModel.signUpWithEmail(email, pass, name) },
                            onGoogleSignIn = { viewModel.signInWithGoogle() },
                            onGuestSignIn = { viewModel.signInAsGuest() },
                            onSignOut = { viewModel.signOut() },
                            onResetPassword = { email -> viewModel.sendPasswordReset(email) }
                        )
                    }
                }
            }
        }
    }
}

