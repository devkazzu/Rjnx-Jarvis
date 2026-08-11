package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.accessibility.ScreenContentHolder
import com.example.data.api.GeminiRepository
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthState
import com.example.data.auth.UserAccount
import com.example.data.db.*
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.sync.CloudSyncManager
import com.example.data.sync.SyncStatus
import com.example.services.JarvisFloatingOverlayService
import com.example.services.JarvisForegroundService
import com.example.utilities.NetworkMonitor
import com.example.utilities.SmartActionsManager
import com.example.utilities.SystemUtils
import com.example.voice.VoiceAssistantManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val db = JarvisDatabase.getDatabase(application)
    private val dao = db.appDao()
    private val geminiRepo = GeminiRepository()
    val preferencesRepo = UserPreferencesRepository(application)
    val voiceManager = VoiceAssistantManager(application)
    val authRepo = AuthRepository(application)
    val cloudSyncManager = CloudSyncManager(application)

    // User Authentication States
    val currentUser: StateFlow<UserAccount> = authRepo.currentUser
    val authState: StateFlow<AuthState> = authRepo.authState

    // Cloud Sync States
    val syncStatus: StateFlow<SyncStatus> = cloudSyncManager.syncStatus
    val lastSyncTimestamp: StateFlow<Long> = cloudSyncManager.lastSyncTimestamp
    val autoSyncEnabled: StateFlow<Boolean> = cloudSyncManager.autoSyncEnabled

    // Flow states from DB
    val chatMessages: StateFlow<List<ChatMessageEntity>> = dao.getAllChatMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = dao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashcards: StateFlow<List<FlashcardEntity>> = dao.getAllFlashcards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todos: StateFlow<List<TodoItemEntity>> = dao.getAllTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habits: StateFlow<List<HabitEntity>> = dao.getAllHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = dao.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = dao.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val examCountdowns: StateFlow<List<ExamCountdownEntity>> = dao.getAllExamCountdowns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyGoals: StateFlow<List<DailyGoalEntity>> = dao.getAllDailyGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings States
    val themeMode = preferencesRepo.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, "Dark Futuristic")
    val aiPersonality = preferencesRepo.aiPersonality.stateIn(viewModelScope, SharingStarted.Eagerly, "Classic Jarvis")
    val aiPersonalityTone = preferencesRepo.aiPersonalityTone.stateIn(viewModelScope, SharingStarted.Eagerly, "Classic Futuristic")
    val aiResponseVerbosity = preferencesRepo.aiResponseVerbosity.stateIn(viewModelScope, SharingStarted.Eagerly, "Balanced")
    val aiCustomDirective = preferencesRepo.aiCustomDirective.stateIn(viewModelScope, SharingStarted.Eagerly, "Always be polite, highly intelligent, and structured.")
    val customApiKey = preferencesRepo.customApiKey.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val isVoiceOutputEnabled = preferencesRepo.isVoiceOutputEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val isWakeWordEnabled = preferencesRepo.isWakeWordEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val speechPitch = preferencesRepo.speechPitch.stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)
    val speechRate = preferencesRepo.speechRate.stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)
    val accentColor = preferencesRepo.accentColor.stateIn(viewModelScope, SharingStarted.Eagerly, "Cyan")
    val isBackgroundServiceEnabled = preferencesRepo.isBackgroundServiceEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val isFloatingButtonEnabled = preferencesRepo.isFloatingButtonEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Service & Accessibility Diagnostics States
    val isForegroundServiceRunning: StateFlow<Boolean> = JarvisForegroundService.isRunning
    val isFloatingOverlayActive: StateFlow<Boolean> = JarvisFloatingOverlayService.isFloatingOverlayActive
    val isAccessibilityActive: StateFlow<Boolean> = ScreenContentHolder.isServiceActive
    val currentScreenText: StateFlow<String> = ScreenContentHolder.currentScreenText
    val currentScreenPackage: StateFlow<String> = ScreenContentHolder.currentPackageName

    // Connectivity & Offline Queued Actions States
    val isOnline: StateFlow<Boolean> = NetworkMonitor.isOnline
    val queuedActions: StateFlow<List<QueuedActionEntity>> = dao.getAllQueuedActions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // UI Interactive States
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _lastVoiceResponse = MutableStateFlow("RJNX Jarvis ready. Say 'Hey Jarvis' or press the voice core.")
    val lastVoiceResponse: StateFlow<String> = _lastVoiceResponse.asStateFlow()

    // Study Assistant States
    private val _doubtResult = MutableStateFlow("")
    val doubtResult: StateFlow<String> = _doubtResult.asStateFlow()

    private val _summaryResult = MutableStateFlow("")
    val summaryResult: StateFlow<String> = _summaryResult.asStateFlow()

    // Pomodoro Timer
    private val _pomodoroTimeLeft = MutableStateFlow(25 * 60) // 25 min
    val pomodoroTimeLeft: StateFlow<Int> = _pomodoroTimeLeft.asStateFlow()

    private val _isPomodoroRunning = MutableStateFlow(false)
    val isPomodoroRunning: StateFlow<Boolean> = _isPomodoroRunning.asStateFlow()

    private val _pomodoroMode = MutableStateFlow("Work") // "Work" or "Rest"
    val pomodoroMode: StateFlow<String> = _pomodoroMode.asStateFlow()

    private var pomodoroJob: Job? = null

    // Calculator State
    private val _calcDisplay = MutableStateFlow("0")
    val calcDisplay: StateFlow<String> = _calcDisplay.asStateFlow()

    // Flashlight state
    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    // App shortcuts list
    private val _installedApps = MutableStateFlow<List<SystemUtils.AppInfo>>(emptyList())
    val installedApps: StateFlow<List<SystemUtils.AppInfo>> = _installedApps.asStateFlow()

    init {
        loadInstalledApps()
        setupVoiceListener()
        setupConnectivityMonitoring()
        seedInitialSampleDataIfEmpty()
    }

    private fun setupConnectivityMonitoring() {
        viewModelScope.launch {
            NetworkMonitor.observeConnectivity(getApplication()).collect { online ->
                if (online) {
                    processQueuedActionsAutomatically()
                }
            }
        }
    }

    fun processQueuedActionsAutomatically() {
        viewModelScope.launch {
            val pending = dao.getPendingQueuedActions()
            if (pending.isNotEmpty()) {
                for (queued in pending) {
                    val actionReq = try {
                        val type = SmartActionsManager.ActionType.valueOf(queued.actionType)
                        SmartActionsManager.ActionRequest(type = type, target = queued.target)
                    } catch (e: Exception) {
                        SmartActionsManager.ActionRequest(type = SmartActionsManager.ActionType.OPEN_APP, target = queued.target)
                    }
                    val result = SmartActionsManager.executeAction(getApplication(), actionReq)
                    dao.updateQueuedActionStatus(queued.id, "EXECUTED")
                    val msg = "Connectivity Restored! Executed queued action: '${queued.command}' -> $result"
                    dao.insertChatMessage(ChatMessageEntity(sender = "JARVIS", content = msg))
                    _lastVoiceResponse.value = msg
                    if (isVoiceOutputEnabled.value) {
                        voiceManager.speak(msg, speechPitch.value, speechRate.value)
                    }
                }
            }
        }
    }


    private fun loadInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = SystemUtils.getInstalledApps(getApplication())
        }
    }

    private fun setupVoiceListener() {
        voiceManager.onSpeechResultListener = { spokenText ->
            processVoiceInput(spokenText)
        }
    }

    private fun processVoiceInput(text: String) {
        val cleanText = text.trim()
        val lower = cleanText.lowercase()

        // Check wake word or direct command
        val prompt = if (lower.startsWith("hey jarvis")) {
            cleanText.substringAfter("hey jarvis", "").trim().ifBlank { "Hello Jarvis" }
        } else {
            cleanText
        }

        sendChatMessage(prompt, isVoice = true)
    }

    fun sendChatMessage(prompt: String, isVoice: Boolean = false) {
        if (prompt.isBlank()) return

        viewModelScope.launch {
            _isGenerating.value = true

            // Insert User Message
            dao.insertChatMessage(ChatMessageEntity(sender = "USER", content = prompt, isVoice = isVoice))

            // Check if input is a smart system action (Open app, launch settings, set alarm, call, message)
            val actionReq = SmartActionsManager.parseCommand(prompt)

            // Local device actions must NEVER be blocked by internet availability.
            // Calls, apps, settings, alarms, accessibility controls, flashlight, etc.
            // are executed locally. Only cloud/AI requests require connectivity.            val isOnlineNow = NetworkMonitor.checkIsOnline(getApplication())

            if (!isOnlineNow) {
                if (actionReq != null) {
                    dao.insertQueuedAction(
                        QueuedActionEntity(
                            command = prompt,
                            actionType = actionReq.type.name,
                            target = actionReq.target,
                            status = "QUEUED"
                        )
                    )
                    val offlineMsg = "Device is offline. Smart Action '$prompt' captured in offline queue and will execute automatically when connectivity is restored."
                    dao.insertChatMessage(ChatMessageEntity(sender = "JARVIS", content = offlineMsg, isVoice = isVoice))
                    _lastVoiceResponse.value = offlineMsg
                    _isGenerating.value = false
                    if (isVoiceOutputEnabled.value || isVoice) {
                        voiceManager.speak(offlineMsg, speechPitch.value, speechRate.value)
                    }
                    return@launch
                } else {
                    val offlineMsg = "Device is offline. Cloud AI is unavailable without internet. Try asking a Smart Action (e.g., 'Open Settings', 'Set alarm 7:00', 'Call 12345') to queue it locally."
                    dao.insertChatMessage(ChatMessageEntity(sender = "JARVIS", content = offlineMsg, isVoice = isVoice))
                    _lastVoiceResponse.value = offlineMsg
                    _isGenerating.value = false
                    if (isVoiceOutputEnabled.value || isVoice) {
                        voiceManager.speak(offlineMsg, speechPitch.value, speechRate.value)
                    }
                    return@launch
                }
            }

            if (actionReq != null && actionReq.type != SmartActionsManager.ActionType.SCREEN_EXPLAIN) {
                val actionResult = SmartActionsManager.executeAction(getApplication(), actionReq)
                dao.insertChatMessage(ChatMessageEntity(sender = "JARVIS", content = actionResult, isVoice = isVoice))
                _lastVoiceResponse.value = actionResult
                _isGenerating.value = false
                if (isVoiceOutputEnabled.value || isVoice) {
                    voiceManager.speak(actionResult, speechPitch.value, speechRate.value)
                }
                return@launch
            }


            // Build history context
            val history = chatMessages.value.takeLast(6).map { Pair(if (it.sender == "USER") it.content else "", if (it.sender == "JARVIS") it.content else "") }

            val userName = currentUser.value.displayName
            val toneInstruction = when (aiPersonalityTone.value) {
                "Formal & Professional" -> "Use a formal, polite, executive tone."
                "Friendly & Empathetic" -> "Use a warm, friendly, encouraging, and empathetic tone."
                "Witty & Sarcastic" -> "Use a witty, clever, playful, and dryly humorous tone like Tony Stark's Jarvis."
                "Academic Tutor" -> "Act as a patient, world-class Academic Tutor explaining concepts step by step with clear analogies."
                "Tech Guru" -> "Act as a principal AI/Software Engineer giving high-efficiency technical solutions."
                "Motivational Coach" -> "Act as an energetic, inspiring performance coach."
                else -> "Maintain an advanced, futuristic, polite, and highly intelligent AI voice."
            }

            val verbosityInstruction = when (aiResponseVerbosity.value) {
                "Concise" -> "Keep responses short, punchy, bulleted, and strictly under 3 sentences unless complex code is requested."
                "Verbose & Detailed" -> "Provide comprehensive, in-depth explanations with step-by-step breakdowns, key points, and examples."
                else -> "Provide balanced, clear, and well-structured responses."
            }

            val customDir = aiCustomDirective.value.ifBlank { "" }

            // Include screen awareness context if user is asking about the screen
            val screenContext = if (prompt.lowercase().contains("screen") || prompt.lowercase().contains("visible") || actionReq?.type == SmartActionsManager.ActionType.SCREEN_EXPLAIN) {
                val textOnScreen = currentScreenText.value
                val pkgName = currentScreenPackage.value
                if (textOnScreen.isNotBlank()) {
                    "\n\n[VISIBLE SCREEN CONTENT FROM APP '$pkgName']:\n$textOnScreen"
                } else {
                    "\n\n[NOTE: Accessibility Service is not active or no text was detected on current screen]."
                }
            } else ""

            val personalityPrompt = """
                You are RJNX Jarvis, an advanced AI voice & productivity assistant addressing user '$userName'.
                Tone Directive: $toneInstruction
                Verbosity Directive: $verbosityInstruction
                ${if (customDir.isNotBlank()) "User Custom Directive: $customDir" else ""}
                $screenContext
            """.trimIndent()

            val reply = geminiRepo.generateResponse(
                prompt = prompt,
                history = history,
                personalityPrompt = personalityPrompt,
                customApiKey = customApiKey.value
            )

            // Insert Jarvis Response
            dao.insertChatMessage(ChatMessageEntity(sender = "JARVIS", content = reply, isVoice = isVoice))
            _lastVoiceResponse.value = reply
            _isGenerating.value = false

            // Voice output if enabled
            if (isVoiceOutputEnabled.value || isVoice) {
                voiceManager.speak(reply, speechPitch.value, speechRate.value)
            }

            // Auto Cloud Sync if enabled
            if (autoSyncEnabled.value) {
                cloudSyncManager.backupChatHistoryToCloud(currentUser.value.userId, chatMessages.value)
            }
        }
    }

    // Auth Actions
    fun signInWithEmail(email: String, pass: String) {
        viewModelScope.launch { authRepo.signInWithEmail(email, pass) }
    }

    fun signUpWithEmail(email: String, pass: String, name: String) {
        viewModelScope.launch { authRepo.signUpWithEmail(email, pass, name) }
    }

    fun signInWithGoogle(idToken: String = "simulated_google_token", email: String? = "commander.google@gmail.com", name: String? = "Google Commander") {
        viewModelScope.launch { authRepo.signInWithGoogleCredential(idToken, email, name) }
    }

    fun signInAsGuest() {
        authRepo.signInAsGuest()
    }

    fun signOut() {
        authRepo.signOut()
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch { authRepo.sendPasswordReset(email) }
    }

    // Cloud Sync & Backup Actions
    fun backupChatHistoryToCloud() {
        viewModelScope.launch {
            cloudSyncManager.backupChatHistoryToCloud(currentUser.value.userId, chatMessages.value)
        }
    }

    fun restoreChatHistoryFromCloud() {
        viewModelScope.launch {
            val result = cloudSyncManager.restoreChatHistoryFromCloud(currentUser.value.userId)
            val restored = result.getOrNull()
            if (!restored.isNullOrEmpty()) {
                dao.clearAllChatMessages()
                restored.forEach { dao.insertChatMessage(it) }
            }
        }
    }

    fun setAutoSync(enabled: Boolean) {
        cloudSyncManager.setAutoSync(enabled)
    }

    fun exportChatHistoryJson(): String {
        return cloudSyncManager.exportChatToJson(chatMessages.value)
    }

    fun importChatHistoryFromJson(json: String) {
        viewModelScope.launch {
            val list = cloudSyncManager.importChatFromJson(json)
            if (list.isNotEmpty()) {
                list.forEach { dao.insertChatMessage(it) }
                Toast.makeText(getApplication(), "Imported ${list.size} chat messages", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Background Assistant & Overlay Service Controls
    fun toggleBackgroundService(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.updateBackgroundServiceEnabled(enabled)
            if (enabled) {
                JarvisForegroundService.startService(getApplication())
            } else {
                JarvisForegroundService.stopService(getApplication())
            }
        }
    }

    fun toggleFloatingOverlay(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.updateFloatingButtonEnabled(enabled)
            if (enabled) {
                JarvisFloatingOverlayService.startOverlay(getApplication())
            } else {
                JarvisFloatingOverlayService.stopOverlay(getApplication())
            }
        }
    }

    // Personality Updates
    fun updatePersonalityTone(tone: String) = viewModelScope.launch { preferencesRepo.updateAiPersonalityTone(tone) }
    fun updateResponseVerbosity(verbosity: String) = viewModelScope.launch { preferencesRepo.updateAiResponseVerbosity(verbosity) }
    fun updateCustomDirective(directive: String) = viewModelScope.launch { preferencesRepo.updateAiCustomDirective(directive) }

    fun deleteChatMessage(id: Long) {
        viewModelScope.launch { dao.deleteChatMessage(id) }
    }

    fun clearAllChatMessages() {
        viewModelScope.launch { dao.clearAllChatMessages() }
    }

    fun copyToClipboard(text: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Jarvis Content", text))
        Toast.makeText(getApplication(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share via RJNX Jarvis")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(chooser)
    }

    // AI Doubt Solver
    fun solveDoubt(subject: String, question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _isGenerating.value = true
            val prompt = "Solve this $subject doubt step-by-step in clear layout:\n$question"
            val result = geminiRepo.generateResponse(prompt = prompt, customApiKey = customApiKey.value)
            _doubtResult.value = result
            _isGenerating.value = false
        }
    }

    // PDF / Text Summarizer
    fun summarizeDocument(docTitle: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            _isGenerating.value = true
            val prompt = "Provide a comprehensive Executive Summary, Key Highlights, and Action Items for document '$docTitle':\n$content"
            val result = geminiRepo.generateResponse(prompt = prompt, customApiKey = customApiKey.value)
            _summaryResult.value = result
            _isGenerating.value = false
        }
    }

    // Notes Management
    fun addNote(title: String, content: String, category: String = "General") {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            dao.insertNote(NoteEntity(title = title, content = content, category = category))
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch { dao.deleteNote(note) }
    }

    // Flashcards
    fun addFlashcard(deckName: String, question: String, answer: String) {
        if (question.isBlank() || answer.isBlank()) return
        viewModelScope.launch {
            dao.insertFlashcard(FlashcardEntity(deckName = deckName, question = question, answer = answer))
        }
    }

    fun toggleFlashcardLearned(id: Long, isLearned: Boolean) {
        viewModelScope.launch { dao.updateFlashcardLearned(id, isLearned) }
    }

    fun deleteFlashcard(flashcard: FlashcardEntity) {
        viewModelScope.launch { dao.deleteFlashcard(flashcard) }
    }

    // To-Do
    fun addTodo(title: String, category: String = "General", priority: String = "Medium") {
        if (title.isBlank()) return
        viewModelScope.launch {
            dao.insertTodo(TodoItemEntity(title = title, category = category, priority = priority))
        }
    }

    fun toggleTodo(id: Long, isCompleted: Boolean) {
        viewModelScope.launch { dao.updateTodoStatus(id, isCompleted) }
    }

    fun deleteTodo(todo: TodoItemEntity) {
        viewModelScope.launch { dao.deleteTodo(todo) }
    }

    // Habits
    fun addHabit(title: String, targetDays: Int = 7) {
        if (title.isBlank()) return
        viewModelScope.launch {
            dao.insertHabit(HabitEntity(title = title, targetDaysPerWeek = targetDays))
        }
    }

    fun incrementHabitStreak(habit: HabitEntity) {
        viewModelScope.launch {
            dao.updateHabitStreak(habit.id, habit.streakCount + 1, "2026-07-31")
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch { dao.deleteHabit(habit) }
    }

    // Expenses
    fun addExpense(title: String, amount: Double, category: String, isIncome: Boolean = false) {
        if (title.isBlank() || amount <= 0) return
        viewModelScope.launch {
            dao.insertExpense(ExpenseEntity(title = title, amount = amount, category = category, isIncome = isIncome))
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch { dao.deleteExpense(expense) }
    }

    // Reminders
    fun addReminder(title: String, timeMillis: Long) {
        if (title.isBlank()) return
        viewModelScope.launch {
            dao.insertReminder(ReminderEntity(title = title, timeMillis = timeMillis))
        }
    }

    fun toggleReminder(id: Long, isCompleted: Boolean) {
        viewModelScope.launch { dao.updateReminderStatus(id, isCompleted) }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch { dao.deleteReminder(reminder) }
    }

    // Exam Countdowns
    fun addExamCountdown(title: String, subject: String, examDateMillis: Long) {
        if (title.isBlank()) return
        viewModelScope.launch {
            dao.insertExamCountdown(ExamCountdownEntity(examTitle = title, subject = subject, examDateMillis = examDateMillis))
        }
    }

    fun deleteExamCountdown(exam: ExamCountdownEntity) {
        viewModelScope.launch { dao.deleteExamCountdown(exam) }
    }

    // Daily Goals
    fun addDailyGoal(title: String, targetCount: Int) {
        if (title.isBlank()) return
        viewModelScope.launch {
            dao.insertDailyGoal(DailyGoalEntity(goalTitle = title, targetCount = targetCount))
        }
    }

    fun incrementDailyGoalProgress(goal: DailyGoalEntity) {
        val newCount = goal.currentCount + 1
        val isDone = newCount >= goal.targetCount
        viewModelScope.launch {
            dao.updateDailyGoalProgress(goal.id, newCount, isDone)
        }
    }

    fun deleteDailyGoal(goal: DailyGoalEntity) {
        viewModelScope.launch { dao.deleteDailyGoal(goal) }
    }

    // Pomodoro Timer Controls
    fun startPomodoro() {
        if (_isPomodoroRunning.value) return
        _isPomodoroRunning.value = true
        pomodoroJob = viewModelScope.launch {
            while (_isPomodoroRunning.value && _pomodoroTimeLeft.value > 0) {
                delay(1000)
                _pomodoroTimeLeft.value -= 1
            }
            if (_pomodoroTimeLeft.value == 0) {
                _isPomodoroRunning.value = false
                if (_pomodoroMode.value == "Work") {
                    _pomodoroMode.value = "Rest"
                    _pomodoroTimeLeft.value = 5 * 60
                    voiceManager.speak("Pomodoro Work session complete! Take a 5 minute rest.")
                } else {
                    _pomodoroMode.value = "Work"
                    _pomodoroTimeLeft.value = 25 * 60
                    voiceManager.speak("Rest period finished! Time to lock in for the next work session.")
                }
            }
        }
    }

    fun pausePomodoro() {
        _isPomodoroRunning.value = false
        pomodoroJob?.cancel()
    }

    fun resetPomodoro() {
        pausePomodoro()
        _pomodoroTimeLeft.value = if (_pomodoroMode.value == "Work") 25 * 60 else 5 * 60
    }

    // Calculator Actions
    fun onCalcButtonPress(btn: String) {
        when (btn) {
            "C" -> _calcDisplay.value = "0"
            "DEL" -> {
                _calcDisplay.value = if (_calcDisplay.value.length > 1) _calcDisplay.value.dropLast(1) else "0"
            }
            "=" -> {
                _calcDisplay.value = SystemUtils.evaluateMathExpression(_calcDisplay.value)
            }
            else -> {
                _calcDisplay.value = if (_calcDisplay.value == "0") btn else _calcDisplay.value + btn
            }
        }
    }

    // Flashlight Toggle
    fun toggleFlashlight() {
        val newStatus = !_isFlashlightOn.value
        val success = SystemUtils.toggleFlashlight(getApplication(), newStatus)
        if (success) {
            _isFlashlightOn.value = newStatus
        } else {
            _isFlashlightOn.value = newStatus // visual HUD fallback
        }
    }

    // Settings Updates
    fun updateTheme(theme: String) = viewModelScope.launch { preferencesRepo.updateThemeMode(theme) }
    fun updatePersonality(personality: String) = viewModelScope.launch { preferencesRepo.updateAiPersonality(personality) }
    fun updateApiKey(key: String) = viewModelScope.launch { preferencesRepo.updateCustomApiKey(key) }
    fun updateVoiceOutput(enabled: Boolean) = viewModelScope.launch { preferencesRepo.updateVoiceOutputEnabled(enabled) }
    fun updateWakeWord(enabled: Boolean) = viewModelScope.launch { preferencesRepo.updateWakeWordEnabled(enabled) }
    fun updateSpeechPitch(pitch: Float) = viewModelScope.launch { preferencesRepo.updateSpeechPitch(pitch) }
    fun updateSpeechRate(rate: Float) = viewModelScope.launch { preferencesRepo.updateSpeechRate(rate) }
    fun updateAccentColor(color: String) = viewModelScope.launch { preferencesRepo.updateAccentColor(color) }

    // Offline Queued Actions Management
    fun queueSmartActionManually(command: String) {
        val actionReq = SmartActionsManager.parseCommand(command) ?: SmartActionsManager.ActionRequest(SmartActionsManager.ActionType.OPEN_APP, command)
        viewModelScope.launch {
            dao.insertQueuedAction(
                QueuedActionEntity(
                    command = command,
                    actionType = actionReq.type.name,
                    target = actionReq.target,
                    status = "QUEUED"
                )
            )
            Toast.makeText(getApplication(), "Action '$command' added to offline queue", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteQueuedAction(id: Long) {
        viewModelScope.launch {
            dao.deleteQueuedAction(id)
        }
    }

    fun clearExecutedQueuedActions() {
        viewModelScope.launch {
            dao.clearExecutedQueuedActions()
        }
    }


    private fun seedInitialSampleDataIfEmpty() {
        viewModelScope.launch {
            if (chatMessages.value.isEmpty()) {
                dao.insertChatMessage(ChatMessageEntity(sender = "JARVIS", content = "Greetings! I am RJNX Jarvis — your futuristic AI assistant. How may I assist you today?"))
            }
            if (todos.value.isEmpty()) {
                dao.insertTodo(TodoItemEntity(title = "Review Quantum Physics notes", category = "Study", priority = "High"))
                dao.insertTodo(TodoItemEntity(title = "Complete daily Pomodoro session", category = "Productivity", priority = "Medium"))
            }
            if (habits.value.isEmpty()) {
                dao.insertHabit(HabitEntity(title = "Daily AI Study", streakCount = 5))
                dao.insertHabit(HabitEntity(title = "Drink 3L Water", streakCount = 12))
            }
            if (flashcards.value.isEmpty()) {
                dao.insertFlashcard(FlashcardEntity(deckName = "Computer Science", question = "What is Time Complexity of QuickSort?", answer = "Average case O(N log N), Worst case O(N^2)"))
                dao.insertFlashcard(FlashcardEntity(deckName = "Physics", question = "State Newton's Second Law of Motion", answer = "Force equals Mass times Acceleration (F = m * a)"))
            }
            if (notes.value.isEmpty()) {
                dao.insertNote(NoteEntity(title = "Jarvis Architecture", content = "Kotlin + Jetpack Compose + Room + Gemini 3.5 Flash REST API + TTS Engine", isPinned = true))
            }
            if (examCountdowns.value.isEmpty()) {
                dao.insertExamCountdown(ExamCountdownEntity(examTitle = "Final Semester Finals", subject = "Computer Science", examDateMillis = System.currentTimeMillis() + 15 * 86400000L))
            }
            if (dailyGoals.value.isEmpty()) {
                dao.insertDailyGoal(DailyGoalEntity(goalTitle = "Solve 3 Doubts", targetCount = 3, currentCount = 1))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
