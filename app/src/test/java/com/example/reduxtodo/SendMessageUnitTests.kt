package com.example.reduxtodo

import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.*
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class SendMessageUnitTests {

    // moved to class level to avoid injecting into MockApiClient
    companion object {
        private val testCoroutineScope = TestCoroutineScope()
        private const val responseDelay: Long = 100
        private val sendMessage = Action.SendMessage(
            ChatId("kirill-natalia"),
            MessageContent("Hello")
        )
    }

    lateinit var store: Store<AppState, Action>

    class MockApiClient(private var attemptsBeforeFirstSuccess: Int) : ApiClient {
        override fun sendAsync(message: Message.Local): Deferred<Result<Message.Remote, ApiClient.Error>> {
            return testCoroutineScope.async {
                delay(responseDelay)

                if (attemptsBeforeFirstSuccess-- > 0)
                    return@async Error<Message.Remote, ApiClient.Error>(ApiClient.Error.NoNetwork)

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

    private fun prepare(apiClient: ApiClient) {
        val state = AppState(
            apiClient = apiClient,
            localUserId = UserId("kirill")
        )

        store = Store(
            state = state,
            reducer = ::appReducer,
            coroutineScope = testCoroutineScope
        )
    }

    @Test
    fun `sent message gets added to state`() = testCoroutineScope.runBlockingTest {
        prepare(MockApiClient(0))
        store.dispatch(sendMessage)
        assertEquals(
            1,
            store.state.messages.count()
        )
    }

    @Test
    fun `sent message gets added to state as a Local message`() = testCoroutineScope.runBlockingTest {
        prepare(MockApiClient(0))
        store.dispatch(sendMessage)
        assertEquals(
            1,
            store.state.messages.filterIsInstance<Message.Local>().count()
        )
    }

    @Test
    fun `sent message after a delay is marked as Remote (delivered to server)`() = testCoroutineScope.runBlockingTest {
        prepare(MockApiClient(0))
        store.dispatch(sendMessage)
        delay(responseDelay * 2)
        assertEquals(
            1,
            store.state.messages.filterIsInstance<Message.Remote>().count()
        )
    }

    @Test
    fun `if not delivered from the first attempt re-delivery attempts decreases by 1`() = testCoroutineScope.runBlockingTest {
        prepare(MockApiClient(1))
        store.dispatch(sendMessage)

        // this is pretty ugly. Ideally we would want to actually have the sample
        // message here, but we also want to hide the construction of the message
        // within the reducer .. hmm dilemma
        val pendingFirstAttemptMessage = store.state.messages.first() as Message.Local
        delay(responseDelay + responseDelay / 2)
        assertEquals(
            pendingFirstAttemptMessage.deliveryAttemptsLeft - 1,
            (store.state.messages.first() as Message.Local).deliveryAttemptsLeft
        )
    }

    @Test
    fun `if cant deliver message at all, should not drop attempts count below zero`() = testCoroutineScope.runBlockingTest {
        prepare(MockApiClient(50))
        store.dispatch(sendMessage)
        delay(responseDelay * 10)
        assertEquals(
            0,
            (store.state.messages.first() as Message.Local).deliveryAttemptsLeft
        )
    }

    @Test
    fun `sending message to a non-existing chat creates that chat`() = testCoroutineScope.runBlockingTest {
        prepare(MockApiClient(0))
        store.dispatch(sendMessage)

        assertEquals(
            1,
            store.state.chats.count()
        )
    }
}
