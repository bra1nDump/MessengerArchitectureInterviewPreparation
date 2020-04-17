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

class MockApiClient() : ApiClient {
    override fun sendAsync(message: Message.Local): Deferred<Result<Message.Remote, ApiClient.Error>> {
        return GlobalScope.async {
            delay(100)

            Ok<Message.Remote, ApiClient.Error>(
                Message.Remote(
                    id = message.id,
                    senderId = message.senderId,
                    chatId = message.chatId,
                    content = message.content,
                    timestamp = message.timestamp
                )
            )
        }
    }
}

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val store = AppStore(
            state = AppState(
                apiClient = MockApiClient(),
                localUserId = UserId("kirill"),
                chats = listOf(ChatId("kirill-natalia"))
            ),
            reducer = ::appReducer
        )

        setContent {
            MaterialTheme {
                App(store = store)
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

typealias AppStore = Store<AppState, Action>

@Composable
fun App(
    store: AppStore,
    navigationState: NavigationState = NavigationState()
) {
    when (val screen = navigationState.screen) {
        is Screen.Chats -> Messenger(store, openChat = { chatId -> navigationState.screen = Screen.Chat(chatId) })
        is Screen.Chat -> Chat(store, screen.chatId, dismiss = { navigationState.screen = Screen.Chats })
    }
}

@Composable
fun Messenger(store: AppStore, openChat: (ChatId) -> Unit) {
    Column {
        Text("All chats")

        store.state.chats.forEach { chatId ->
            val lastMessage = store.state.messages
                .filter { message -> message.chatId() == chatId }.lastOrNull()
            val lastMessageComponent = if (lastMessage == null) "" else "last message: ${lastMessage.content().text}"
            Button(
                text = "chatId: ${chatId.value} $lastMessageComponent",
                onClick = { openChat(chatId) }
            )
        }
    }
}

@Composable
fun Chat(
    store: AppStore,
    chatId: ChatId,
    dismiss: () -> Unit
) {
    val current = + state { "" }
    fun messages(): List<Message> = store.state.messages.filter { it.chatId() == chatId }

    Recompose { recompose ->
        store.dispatch(Action.AddListener(chatId, recompose))

        Column(Spacing(5.dp)) {
            Button("Back", onClick = dismiss)

            messages().forEach {
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
                            println("sending message..")
                            store.dispatch(Action.SendMessage(chatId, MessageContent(current.value)))
                        }
                    )
                }
            }
        }
    }
}
