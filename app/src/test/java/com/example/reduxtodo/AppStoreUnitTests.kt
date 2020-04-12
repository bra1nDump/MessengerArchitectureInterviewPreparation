package com.example.reduxtodo

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.*
import org.junit.Assert.*

class AppStoreUnitTests {

    class MockApiClient(private val scope: CoroutineScope) : ApiClient {
        override fun sendAsync(message: Message.Local): Deferred<Result<Message.Remote, ApiClient.Error>> {
            return scope.async {
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

    @Test
    fun `sent message gets added to state`() = runBlockingTest {
        val state = AppState(
            apiClient = MockApiClient(this),
            localUserId = UserId("kirill")
        )

        val store = Store(
            state = state,
            reducer = ::appReducer,
            coroutineScope = this
        )

        val sendMessage = Action.SendMessage(
            ChatId("kirill-natalia"),
            MessageContent("Hello pizduk")
        )
        store.dispatch(sendMessage)
        assertEquals(
            1,
            store.state.messages.count()
        )
    }

    @Test
    fun `sent message gets added to state as a Local message`() = runBlockingTest {
        val state = AppState(
            apiClient = MockApiClient(this),
            localUserId = UserId("kirill")
        )

        val store = Store(
            state = state,
            reducer = ::appReducer,
            coroutineScope = this
        )

        val sendMessage = Action.SendMessage(
            ChatId("kirill-natalia"),
            MessageContent("Hello pizduk")
        )
        store.dispatch(sendMessage)
        assertEquals(
            1,
            store.state.messages.filterIsInstance<Message.Local>().count()
        )
    }

    @Test
    fun `sent message after a delay is marked as Remote (delivered to server)`() = runBlockingTest {
        val state = AppState(
            apiClient = MockApiClient(this),
            localUserId = UserId("kirill")
        )

        val store = Store(
            state = state,
            reducer = ::appReducer,
            coroutineScope = this
        )

        val sendMessage = Action.SendMessage(
            ChatId("kirill-natalia"),
            MessageContent("Hello pizduk")
        )
        store.dispatch(sendMessage)
        delay(200)
        assertEquals(
            1,
            store.state.messages.filterIsInstance<Message.Remote>().count()
        )
    }
}