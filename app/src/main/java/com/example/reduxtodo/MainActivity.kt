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

sealed class Screen {
    object Chats: Screen()
    data class Chat(val chatId: ChatId): Screen()
}

@Model
data class NavigationState(
    var screen: Screen = Screen.Chats
)

@Composable
fun App(navigationState: NavigationState = NavigationState()) {
    when (val screen = navigationState.screen) {
        is Screen.Chats -> Messenger(openChat = { chatId -> navigationState.screen = Screen.Chat(chatId) })
        is Screen.Chat -> Chat(listOf())
    }
}

@Composable
fun Messenger(openChat: (ChatId) -> Unit) {
    Column {
        Text("All chats")

        listOf(ChatId("kirill-natalia")).forEach {
            Button(
                text = "chatId: $it, last message: bla ha lol",
                onClick = { openChat(it) }
            )
        }
    }
}

@Composable
fun Chat(
    messages: List<Message>
) {
    var current = + state { "" }

    Column(Spacing(5.dp)) {
        Button("Back")

        messages.forEach {
            Text(it.content().text)
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
                    value = current.value,
                    onValueChange = { current.value = it },
                    imeAction = ImeAction.Send,
                    onImeActionPerformed = {
                        throw error("send message not implemented")
                    }
                )
            }
        }
    }
}
