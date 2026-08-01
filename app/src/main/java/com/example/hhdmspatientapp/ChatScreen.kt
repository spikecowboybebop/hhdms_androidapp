package com.example.hhdmspatientapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import com.example.hhdmspatientapp.TokenManager
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    otherUserName: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var otherUserTyping by remember { mutableStateOf(false) }

    val chatManager = remember {
        val token = TokenManager.getToken() ?: ""
        ChatManager("http://192.168.0.102:3001", token)
    }

    DisposableEffect(conversationId) {
        chatManager.onNewMessage = { json ->
            val msg = ChatMessage(
                id = json.optString("id", ""),
                conversation_id = json.optString("conversation_id", ""),
                sender_id = json.optString("sender_id", ""),
                content = json.optString("content", ""),
                read = json.optBoolean("read", false),
                created_at = json.optString("created_at", ""),
            )
            messages = messages + msg
            chatManager.markRead(conversationId)
        }

        chatManager.onUserTyping = { json ->
            if (json.optString("conversationId") == conversationId) {
                otherUserTyping = true
            }
        }

        chatManager.onUserStopTyping = { json ->
            if (json.optString("conversationId") == conversationId) {
                otherUserTyping = false
            }
        }

        chatManager.connect()
        chatManager.joinChat(conversationId)
        chatManager.markRead(conversationId)

        onDispose {
            chatManager.disconnect()
        }
    }

    LaunchedEffect(conversationId) {
        try {
            messages = RetrofitClient.apiService.getChatMessages(conversationId)
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = otherUserName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TitleBlack,
                        )
                        if (otherUserTyping) {
                            Text(
                                text = "Typing...",
                                fontSize = 12.sp,
                                color = TechTeal,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TitleBlack,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureWhite,
                ),
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = inputText,
                onInputChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        val content = inputText.trim()
                        chatManager.sendMessage(conversationId, content)
                        inputText = ""
                        chatManager.stopTyping(conversationId)
                    }
                },
                onTyping = { chatManager.startTyping(conversationId) },
                onStopTyping = { chatManager.stopTyping(conversationId) },
            )
        },
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = TechTeal)
            }
        } else {
            val myUserId = try {
                val jwt = TokenManager.getToken()?.split(".")?.get(1) ?: ""
                val decoded = String(android.util.Base64.decode(jwt, android.util.Base64.DEFAULT))
                JSONObject(decoded).optString("sub", "")
            } catch (_: Exception) { "" }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF5F5F5))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.sender_id == myUserId
                    BubbleMessage(
                        message = msg.content,
                        isMe = isMe,
                        time = formatChatTime(msg.created_at),
                    )
                }
            }
        }
    }
}

@Composable
fun BubbleMessage(message: String, isMe: Boolean, time: String) {
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isMe) TechTeal else PureWhite
    val textColor = if (isMe) PureWhite else TitleBlack

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMe) 16.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 16.dp,
                ),
                color = bubbleColor,
                tonalElevation = if (isMe) 0.dp else 1.dp,
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = textColor,
                    )
                }
            }
            Text(
                text = time,
                fontSize = 10.sp,
                color = SlateGray,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onTyping: () -> Unit,
    onStopTyping: () -> Unit,
) {
    var typingTimer by remember { mutableStateOf< kotlinx.coroutines.Job? >(null) }

    Surface(
        tonalElevation = 3.dp,
        color = PureWhite,
        modifier = Modifier.navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    onInputChange(it)
                    onTyping()
                    typingTimer?.cancel()
                    typingTimer = null
                },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Type a message...",
                        fontSize = 14.sp,
                        color = CoolGray,
                    )
                },
                textStyle = LocalTextStyle.current.copy(color = TitleBlack),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = TechTeal,
                    cursorColor = TechTeal,
                ),
                maxLines = 4,
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(44.dp)
                    .background(TechTeal, CircleShape),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = PureWhite,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun formatChatTime(isoTime: String?): String {
    if (isoTime.isNullOrBlank()) return ""
    return try {
        val clean = isoTime.replace("Z", "+00:00")
        val dt = java.time.OffsetDateTime.parse(clean)
        val bst = dt.withOffsetSameInstant(java.time.ZoneOffset.ofHours(6))
        val nowBst = java.time.OffsetDateTime.now(java.time.ZoneOffset.ofHours(6))
        if (bst.toLocalDate() == nowBst.toLocalDate()) {
            String.format("%02d:%02d", bst.hour, bst.minute)
        } else {
            "${bst.dayOfMonth}/${bst.monthValue} ${String.format("%02d:%02d", bst.hour, bst.minute)}"
        }
    } catch (_: Exception) {
        ""
    }
}
