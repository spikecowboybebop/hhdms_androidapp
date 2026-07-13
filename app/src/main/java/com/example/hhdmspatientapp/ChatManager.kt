package com.example.hhdmspatientapp

import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import java.util.HashMap

class ChatManager(
    private val baseUrl: String,
    private val token: String,
) {
    private var socket: Socket? = null
    private var currentConversationId: String? = null

    var onNewMessage: ((JSONObject) -> Unit)? = null
    var onUserTyping: ((JSONObject) -> Unit)? = null
    var onUserStopTyping: ((JSONObject) -> Unit)? = null
    var onMessagesRead: ((JSONObject) -> Unit)? = null
    var onConnect: (() -> Unit)? = null
    var onDisconnect: (() -> Unit)? = null

    fun connect() {
        val authMap = HashMap<String, String>()
        authMap["token"] = token
        val opts = IO.Options.builder()
            .setAuth(authMap)
            .build()

        socket = IO.socket("$baseUrl/chat", opts)

        socket?.on(Socket.EVENT_CONNECT) {
            onConnect?.invoke()
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            onDisconnect?.invoke()
        }

        socket?.on("new_message") { args ->
            (args.firstOrNull() as? JSONObject)?.let { onNewMessage?.invoke(it) }
        }

        socket?.on("user_typing") { args ->
            (args.firstOrNull() as? JSONObject)?.let { onUserTyping?.invoke(it) }
        }

        socket?.on("user_stop_typing") { args ->
            (args.firstOrNull() as? JSONObject)?.let { onUserStopTyping?.invoke(it) }
        }

        socket?.on("messages_read") { args ->
            (args.firstOrNull() as? JSONObject)?.let { onMessagesRead?.invoke(it) }
        }

        socket?.connect()
    }

    fun joinChat(conversationId: String) {
        currentConversationId = conversationId
        socket?.emit("join_chat", JSONObject().put("conversationId", conversationId))
    }

    fun sendMessage(conversationId: String, content: String) {
        socket?.emit(
            "send_message",
            JSONObject().put("conversationId", conversationId).put("content", content),
        )
    }

    fun startTyping(conversationId: String) {
        socket?.emit("typing", JSONObject().put("conversationId", conversationId))
    }

    fun stopTyping(conversationId: String) {
        socket?.emit("stop_typing", JSONObject().put("conversationId", conversationId))
    }

    fun markRead(conversationId: String) {
        socket?.emit("mark_read", JSONObject().put("conversationId", conversationId))
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }
}
