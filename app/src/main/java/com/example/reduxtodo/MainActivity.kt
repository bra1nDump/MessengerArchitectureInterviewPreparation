package com.example.reduxtodo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.compose.*
import androidx.ui.core.*
import androidx.ui.input.*
import androidx.ui.layout.*
import androidx.ui.material.*
import androidx.ui.tooling.preview.Preview
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.ThreadLocalRandom

interface ApiClient {
    fun sendAsync(chatId: UUID, message: String) : Deferred<Boolean>
}

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // initial state
        var state: MessengerState = MessengerState.sample()
        var navigationState: NavigationState = NavigationState()

        // Server mock
        GlobalScope.launch {
            while (true) {
                delay(1000)

                launch(Dispatchers.Main) {
                    if (ThreadLocalRandom.current().nextBoolean())
                        state.chats = state.chats.plus(ChatState.sample())
                    else {
                        var chat = state.chats.random()
                        chat.messages = chat.messages.plus("Laaaaal")
                    }
                }
            }
        }

        val apiClient = object : ApiClient {
            override fun sendAsync(chatId: UUID, message: String) : Deferred<Boolean> {
                println("Trying to send message ...")
                // TODO: This might fail, several problems arrise from it
                // Need to handle error if it does fail? Or maybe just never marked this message
                // as sent?
                // Need to keep unsent messges in a separate collection? Or just keep in chats?
                // but if we do keep them in chats this might be a problem since
                return async {
                    delay(500)
                    true
                }
            }
        }

        setContent {
            MaterialTheme {
                App(state, navigationState, apiClient)
            }
        }
    }
}

@Model
data class ChatState(
    var id: UUID = UUID.randomUUID(),
    var messages: List<String>,
    // this is local and should be lost if not commited
    // actually thats not true. A good messenger will not loose your typing progress
    // even if you leave the chat
    var current: String = "",
    var failedToSendMessages: List<String> = listOf()
) {
    companion object {
        fun sample() : ChatState = ChatState(messages = mutableListOf("Lol", "Petux"))
    }
}

@Model
data class MessengerState(
    var chats: List<ChatState> = listOf()
) {
    companion object {
        fun sample() : MessengerState = MessengerState(
            (0..5).map { ChatState.sample() }
        )
    }
}

@Model
data class NavigationState(
    var visibleChatId: UUID? = null
)

@Composable
fun App(
    state: MessengerState = MessengerState.sample(),
    navigationState: NavigationState = NavigationState(),
    apiClient: ApiClient
) {
    // TODO: Desired interface - props.currentScreen
    when (val chatId = navigationState.visibleChatId) {
        null -> Messenger(state, navigationState)
        else ->
            // TODO: will fail silently here if chat not found
            state.chats.find { it.id == chatId }?.let {
                val chatId = it.id
                Chat(
                    it,
                    sendMessage = { message -> apiClient.sendAsync(chatId, message) },
                    navigateBack = { navigationState.visibleChatId = null }
                )
            }
    }
}

@Composable
fun Messenger(
    state: MessengerState,
    navigationState: NavigationState
) {
    Column {
        state.chats.forEach {
            Button(
                text = "${it.id} ${it.messages.last()}",
                onClick = { navigationState.visibleChatId = it.id }
            )
        }
    }
}

@Composable
fun Chat(
    state: ChatState = ChatState.sample(),
    sendMessage: (String) -> Deferred<Boolean>,
    navigateBack: () -> Unit = {}
) {
    Column(Spacing(5.dp)) {
        Button("Back", navigateBack)

        state.messages.forEach {
            Text(it)
        }

        Row(
            crossAxisAlignment = CrossAxisAlignment.Center,
            modifier = Spacing(10.dp)
        ) {
            Text("New message: ")
            Container(
                width = 100.dp,
                height = 50.dp
            ) {
                TextField(
                    value = state.current,
                    onValueChange = { state.current = it },
                    imeAction = ImeAction.Send,
                    onImeActionPerformed = {
                        if (it == ImeAction.Send) {
                            // ok this is getting reaaly ugly
                            val messageToSend = state.current
                            state.current = ""
                            GlobalScope.launch {
                                val success = sendMessage(messageToSend).await()

                                MainScope().launch {
                                    if (success) {
                                        println("success, adding to sent messages!")
                                        state.messages = state.messages.plus(messageToSend)
                                    } else {
                                        println("failed to send, adding to queue")
                                        state.failedToSendMessages = state.failedToSendMessages.plus(messageToSend)
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
