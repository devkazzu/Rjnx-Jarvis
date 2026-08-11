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
    val aiPersonality = preferencesRepo.aiPersonality.stateIn(viewModelScope, SharingStarted.Eagerly, "Classic Anu")
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

    private val _lastVoiceResponse = MutableStateFlow("Anu ready. Say 'Hey Anu' or press the voice core.")
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

        // Wake words: Anu or Mio, with legacy Jarvis compatibility.
        val prompt = when {
            lower.startsWith("hey mio") -> cleanText.substringAfter("hey mio", "").trim().ifBlank { "Hello Anu" }
            lower.startsWith("mio") -> cleanText.substringAfter("mio", "").trim().ifBlank { "Hello Anu" }
            lower.startsWith("hey anu") -> cleanText.substringAfter("hey anu", "").trim().ifBlank { "Hello Anu" }
            lower.startsWith("anu") -> cleanText.substringAfter("anu", "").trim().ifBlank { "Hello Anu" }
            lower.startsWith("hey jarvis") -> cleanText.substringAfter("hey jarvis", "").trim().ifBlank { "Hello Anu" }
            lower.startsWith("jarvis") -> cleanText.substringAfter("jarvis", "").trim().ifBlank { "Hello Anu" }
            else -> cleanText
        }

        sendChatMessage(prompt, isVoice = true)
    }

    val memories: StateFlow<List<MemoryEntity>> = dao.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun sendChatMessage(prompt: String, isVoice: Boolean = false) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isGenerating.value = true
            dao.insertChatMessage(ChatMessageEntity(sender = "USER", content = prompt, isVoice = isVoice))

            // Explicit memory commands are handled locally so they work even without AI/network.
            if (handleMemoryCommand(prompt, isVoice)) {
                _isGenerating.value = false
                return@launch
            }

            val screenText = currentScreenText.value
            val screenPkg = currentScreenPackage.value
            val memoryContext = memories.value.take(12).joinToString("\n") { "${it.key}: ${it.value}" }
            val screenContext = if (screenText.isNotBlank()) "App=$screenPkg\n$screenText" else "No accessible screen text available."
            val isOnlineNow = NetworkMonitor.checkIsOnline(getApplication())

            // Keep the deterministic parser as a fast/offline safety net.
            val directAction = SmartActionsManager.parseCommand(prompt)
            if (directAction != null && directAction.type != SmartActionsManager.ActionType.SCREEN_EXPLAIN) {
                if (!isOnlineNow) {
                    dao.insertQueuedAction(QueuedActionEntity(command = prompt, actionType = directAction.type.name, target = directAction.target))
                    val msg = "Offline. I queued that action and will run it when the connection returns."
                    finishResponse(msg, isVoice)
                    return@launch
                }
                val result = SmartActionsManager.executeAction(getApplication(), directAction)
                finishResponse(result, isVoice)
                _isGenerating.value = false
                return@launch
            }

            // AI action router: understand natural language and navigate one UI step at a time.
            // Re-reading the screen after every action is what makes deep navigation possible.
            if (isOnlineNow) {
                var actionPerformed = false
                var lastActionResult = "Done."
                var goal = prompt
                for (step in 0 until 6) {
                    val liveScreen = currentScreenText.value
                    val livePkg = currentScreenPackage.value
                    val liveContext = if (liveScreen.isNotBlank()) "App=$livePkg\n$liveScreen" else screenContext
                    val plans = geminiRepo.planActions(goal, liveContext, memoryContext, customApiKey.value)
                    val plan = plans.firstOrNull() ?: break
                    val type = runCatching { SmartActionsManager.ActionType.valueOf(plan.type) }.getOrNull() ?: break
                    lastActionResult = SmartActionsManager.executeAction(getApplication(), SmartActionsManager.ActionRequest(type, plan.target, plan.detail))
                    actionPerformed = true
                    delay(900)
                    if (type == SmartActionsManager.ActionType.OPEN_APP || type == SmartActionsManager.ActionType.LAUNCH_SETTINGS || type == SmartActionsManager.ActionType.CLICK) {
                        goal = prompt
                    }
                }
                if (actionPerformed) {
                    finishResponse(lastActionResult, isVoice)
                    _isGenerating.value = false
                    return@launch
                }
            } else {
                val offlineMsg = "I need an internet connection for general AI understanding. Basic phone actions still work offline."
                finishResponse(offlineMsg, isVoice)
                _isGenerating.value = false
                return@launch
            }

            val history = chatMessages.value.takeLast(10).map {
                Pair(if (it.sender == "USER") it.content else "", if (it.sender == "JARVIS") it.content else "")
            }
            val userName = currentUser.value.displayName
            val toneInstruction = when (aiPersonalityTone.value) {
                "Formal & Professional" -> "Use a formal, polite, executive tone."
                "Friendly & Empathetic" -> "Use a warm, friendly, encouraging, and empathetic tone."
                "Witty & Sarcastic" -> "Use a witty, clever, playful, dryly humorous tone."
                "Academic Tutor" -> "Act as a patient, world-class academic tutor."
                "Tech Guru" -> "Act as a principal AI/software engineer."
                "Motivational Coach" -> "Act as an energetic performance coach."
                else -> "Maintain an advanced, futuristic, polite and intelligent assistant voice."
            }
            val verbosityInstruction = when (aiResponseVerbosity.value) {
                "Concise" -> "Keep answers short and direct."
                "Verbose & Detailed" -> "Give detailed step-by-step explanations when useful."
                else -> "Give balanced, clear and useful answers."
            }
            val customDir = aiCustomDirective.value.ifBlank { "" }
            val personalityPrompt = """
                You are Anu, also known as Mio, the user's personal AI assistant.
                You can answer normal questions, explain concepts, calculate arithmetic, and converse naturally.
                Do not claim to have performed a phone action unless the app actually executed it.
                User display name: $userName
                Tone: $toneInstruction
                Response style: $verbosityInstruction
                ${if (customDir.isNotBlank()) "Custom directive: $customDir" else ""}
                Relevant long-term memory:
                ${if (memoryContext.isBlank()) "None" else memoryContext}
                Current accessible screen (may be empty):
                $screenContext
            """.trimIndent()

            val reply = geminiRepo.generateResponse(prompt, history, personalityPrompt, customApiKey.value)
            finishResponse(reply, isVoice)
            _isGenerating.value = false
            if (autoSyncEnabled.value) cloudSyncManager.backupChatHistoryToCloud(currentUser.value.userId, chatMessages.value)
        }
    }

    private suspend fun handleMemoryCommand(prompt: String, isVoice: Boolean): Boolean {
        val raw = prompt.trim()
        val lower = raw.lowercase()
        if (lower.startsWith("forget ") || lower.startsWith("delete memory ") || lower.startsWith("forget that ")) {
            val query = raw.replaceFirst(Regex("(?i)^(forget that|forget|delete memory)\\s+"), "").trim()
            val matches = dao.searchMemories(query).ifEmpty { memories.value.filter { it.key.contains(query, true) || it.value.contains(query, true) } }
            matches.forEach { dao.deleteMemory(it.id) }
            finishResponse(if (matches.isEmpty()) "I couldn't find that memory." else "Done. I forgot that memory.", isVoice)
            return true
        }
        if (lower == "what do you remember about me" || lower.contains("what do you remember") || lower == "show my memories") {
            val text = memories.value.take(20).joinToString("\n") { "• ${it.key}: ${it.value}" }
            finishResponse(if (text.isBlank()) "I don't have any saved memories yet." else "Here's what I remember:\n$text", isVoice)
            return true
        }
        val remember = Regex("(?i)^(remember|save this|remember that)\\s+(.+)$").find(raw)
        if (remember != null) {
            val fact = remember.groupValues[2].trim().trimEnd('.')
            saveMemoryFact(fact)
            finishResponse("Got it. I'll remember that.", isVoice)
            return true
        }
        // Useful automatic memories for stable personal facts.
        val patterns = listOf(
            Regex("(?i)^my name is\\s+(.+)$") to "name",
            Regex("(?i)^i am from\\s+(.+)$") to "location",
            Regex("(?i)^my best friend is\\s+(.+)$") to "best_friend",
            Regex("(?i)^my favorite subject is\\s+(.+)$") to "favorite_subject",
            Regex("(?i)^i like\\s+(.+)$") to "preference"
        )
        for ((regex, key) in patterns) {
            val m = regex.find(raw) ?: continue
            dao.insertMemory(
    MemoryEntity(
        key = key,
        value = m.groupValues[1].trim().trimEnd('.'),
        category = "Personal"
    )
)
            return false
        }
        return false
    }

    private suspend fun saveMemoryFact(fact: String) {
        val parts = fact.split(Regex("\\s*[:=]\\s*"), limit = 2)
        if (parts.size == 2) {
            dao.insertMemory(MemoryEntity(key = parts[0].trim().lowercase(), value = parts[1].trim(), category = "User"))
            return
        }
        val key = when {
            fact.contains("name", true) -> "name"
            fact.contains("best friend", true) -> "best_friend"
            fact.contains("favorite", true) -> "preference"
            fact.contains("from", true) -> "location"
            else -> "fact_${System.currentTimeMillis()}"
        }
        dao.insertMemory(MemoryEntity(key = key, value = fact, category = "User"))
    }

    private suspend fun finishResponse(message: String, isVoice: Boolean) {
        dao.insertChatMessage(ChatMessageEntity(sender = "JARVIS", content = message, isVoice = isVoice))
        _lastVoiceResponse.value = message
        if (isVoiceOutputEnabled.value || isVoice) voiceManager.speak(message, speechPitch.value, speechRate.value)
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
        clipboard.setPrimaryClip(ClipData.newPlainText("Anu Content", text))
        Toast.makeText(getApplication(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share via Anu")
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
                dao.insertChatMessage(ChatMessageEntity(sender = "JARVIS", content = "Greetings! I am Anu — your futuristic AI assistant. How may I assist you today?"))
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
                dao.insertNote(NoteEntity(title = "Anu Architecture", content = "Kotlin + Jetpack Compose + Room + Gemini 3.5 Flash REST API + TTS Engine", isPinned = true))
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
