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

class MainActivity : AppCompatActivity() {
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

        setContent {
            MaterialTheme {
                Messenger(state, navigationState)
            }
        }
    }
}

@Model
data class ChatState(
    var id: UUID = UUID.randomUUID(),
    var messages: List<String>,
    // this is local and should be lost if not commited
    var current: String = ""
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

data class ServerMessage(
    var chat: UUID,
    var newMessages: List<String>
)

@Preview
@Composable
fun Messenger(
    state: MessengerState = MessengerState.sample(),
    navigationState: NavigationState = NavigationState()
) {
    when (val chatId = navigationState.visibleChatId) {
        null ->  Column {
            state.chats.forEach {
                Button(
                    text = "${it.id} ${it.messages.last()}",
                    onClick = { navigationState.visibleChatId = it.id }
                )
            }
        }
        else -> Chat(
            state.chats.find { it.id == chatId }!!,
            navigateBack = { navigationState.visibleChatId = null }
        )
    }
}

@Preview
@Composable
fun Chat(state: ChatState = ChatState.sample(), navigateBack: () -> Unit = {}) {
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
                            state.messages = state.messages.plus(state.current)
                            state.current = ""
                        }
                    }
                )
            }
        }
    }
}
