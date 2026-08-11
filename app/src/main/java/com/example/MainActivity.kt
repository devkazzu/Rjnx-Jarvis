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

            val permissionsState = rememberMultiplePermissionsState(
                permissions = buildList {
                    add(Manifest.permission.RECORD_AUDIO)
                    add(Manifest.permission.CAMERA)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    add(Manifest.permission.CALL_PHONE)
                    add(Manifest.permission.READ_CONTACTS)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        add(Manifest.permission.ANSWER_PHONE_CALLS)
                    }
                    add(Manifest.permission.READ_PHONE_STATE)
                    add(Manifest.permission.SEND_SMS)
                }
            )

            LaunchedEffect(authState) {
                if (authState is com.example.data.auth.AuthState.Authenticated &&
                    currentUser.authProvider != com.example.data.auth.AuthProvider.GUEST &&
                    !permissionsState.allPermissionsGranted
                ) {
                    permissionsState.launchMultiplePermissionRequest()
                }
            }

            var selectedTab by remember { mutableStateOf(JarvisTab.VOICE) }

            JarvisTheme(themeMode = themeMode, accentColorName = accentColorName) {
                if (authState is com.example.data.auth.AuthState.Authenticated &&
                    currentUser.authProvider != com.example.data.auth.AuthProvider.GUEST
                ) {
                    MainAuthenticatedUi(
                        viewModel = viewModel,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        currentUser = currentUser,
                        isListening = isListening,
                        isSpeaking = isSpeaking
                    )
                } else {
                    AuthGateScreen(
                        authState = authState,
                        onEmailSignIn = { email, pass -> viewModel.signInWithEmail(email, pass) },
                        onEmailRegister = { email, pass, name -> viewModel.signUpWithEmail(email, pass, name) },
                        onGoogle = {
                            // Real Google OAuth requires the Firebase Android project/client ID.
                            viewModel.signInWithGoogle("")
                        },
                        onGithub = { viewModel.signInWithGithub(this@MainActivity) },
                        onPhoneStart = { phone, sent ->
                            viewModel.startPhoneVerification(this@MainActivity, phone, sent)
                        },
                        onPhoneVerify = { code -> viewModel.verifyPhoneCode(code) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainAuthenticatedUi(
    viewModel: JarvisViewModel,
    selectedTab: JarvisTab,
    onTabSelected: (JarvisTab) -> Unit,
    currentUser: com.example.data.auth.UserAccount,
    isListening: Boolean,
    isSpeaking: Boolean
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = VoidBackground,
        topBar = {
            JarvisHeader(
                title = "ANU THAPA",
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
                onProfileClick = { },
                onVoiceClick = {
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
                onTabSelected = onTabSelected
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            when (selectedTab) {
                JarvisTab.VOICE -> VoiceAssistantScreen(viewModel = viewModel)
                JarvisTab.CHAT -> ChatScreen(viewModel = viewModel)
                JarvisTab.STATUS -> AssistantStatusScreen(viewModel = viewModel)
                JarvisTab.SMART -> SmartAssistantScreen(viewModel = viewModel)
                JarvisTab.STUDY -> StudyAssistantScreen(viewModel = viewModel)
                JarvisTab.PRODUCTIVITY -> ProductivityScreen(viewModel = viewModel)
                JarvisTab.UTILITIES -> UtilitiesScreen(viewModel = viewModel)
                JarvisTab.SETTINGS -> SettingsScreen(viewModel = viewModel, onOpenAuthDialog = { viewModel.signOut() })
            }
        }
    }
}
