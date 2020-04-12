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
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                App()
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

@Composable
fun App() {
    // TODO: Desired interface - props.currentScreen
    Messenger()
}

@Composable
fun Messenger() {
    Column {
        (0..5).forEach {
            Button(
                text = "chatId: $it, last message: bla blaa",
                onClick = { }
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
                        throw error("send message not implemented")
                    }
                )
            }
        }
    }
}
