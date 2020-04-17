package com.example.reduxtodo
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.util.*

// ideally the reducer will return something like this Flow<Action>
// that would support running
fun appReducer(coroutineScope: CoroutineScope, action: Action, state: AppState) : Tuple<AppState, Command<Action>> {
    return when (action) {
        is Action.SendMessage -> {
            val outgoingMessage = Message.Local(
                state.localUserId,
                action.chatId,
                action.messageContent
            )

            fun messageDeliveryUpdates(dispatch: (Action) -> Unit) : Unit {
                coroutineScope.launch {
                    var message = outgoingMessage.copy()
                    while (message.deliveryAttemptsLeft != 0) {
                        when (val result = state.apiClient.sendAsync(message).await()) {
                            is Ok -> {
                                val remoteMessage = result.result

                                dispatch(
                                    Action.ApplyUpdates(
                                        listOf(
                                            // delete old message first
                                            Update.DeleteMessage(message.id),
                                            // insert remote message
                                            // WATCH OUT: what if the remoteMessage.id != message.id
                                            Update.NewMessage(remoteMessage)
                                        )
                                    )
                                )
                                return@launch
                            }
                            is Error -> {
                                // need to decrement not just here, but on the app as well
                                message = message.copy(deliveryAttemptsLeft = message.deliveryAttemptsLeft - 1)

                                dispatch(
                                    Action.ApplyUpdates(
                                        listOf(
                                            // delete old message first
                                            Update.DeleteMessage(message.id),
                                            // insert message with updated delivery attempts count
                                            Update.NewMessage(message)
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }

            val outgoingMessageInsert : Command<Action> = Command.Action(
                Action.ApplyUpdates(
                    listOf(Update.NewMessage(outgoingMessage))
                )
            )

            Tuple(
                state,
                Command.Batch(
                    listOf(
                        outgoingMessageInsert,
                        Command.Flow(::messageDeliveryUpdates)
                    )
                )
            )
        }
        is Action.ApplyUpdates -> {
            val messages =
                action.updates.fold(
                    state.messages,
                    { messages, update ->
                        when (update) {
                            is Update.NewMessage -> messages + update.message
                            is Update.DeleteMessage -> messages.filterNot { it.id() == update.messageId }
                        }
                    }
                )

            state.chatChangeListener?.let { (chatId, handler) ->
                for (update in action.updates) {
                    val updatedChatId = when (update) {
                        is Update.DeleteMessage ->
                            state.messages.find { it.id() == update.messageId }!!.chatId()
                        is Update.NewMessage ->
                            update.message.chatId()
                    }
                    if (updatedChatId == chatId) {
                        handler()
                        return@let
                    }
                }
            }

            // WATCH OUT: this +/- logic probably needs testing of its own :)
            val newMessages = messages - state.messages
            val newChatIds: List<ChatId> = newMessages.map { it.chatId() } - state.chats
            Tuple(
                state.copy(messages = messages, chats = state.chats + newChatIds),
                Command.None()
            )
        }
        is Action.AddListener -> {
            Tuple(
                state.copy(chatChangeListener = Pair(action.chatId, action.notify)),
                Command.None()
            )
        }
    }
}

sealed class Action {
    data class SendMessage(val chatId: ChatId, val messageContent: MessageContent) : Action()
    data class ApplyUpdates(val updates: List<Update>) : Action()

    data class AddListener(val chatId: ChatId, val notify: () -> Unit) : Action()
}

sealed class Update {
    data class NewMessage(val message: Message) : Update()
    data class DeleteMessage(val messageId: MessageId) : Update()
}

// MARK: Our state and reducer
data class AppState(
    val apiClient: ApiClient,
    val localUserId: UserId,
    val messages: List<Message> = listOf(),
    val chats: List<ChatId> = listOf(),
    val chatChangeListener: Pair<ChatId, () -> Unit>? = null
)

data class UserId(val value: String)
data class ChatId(val value: String)
data class MessageId(val value: UUID)

sealed class Message {
    data class Local(
        val senderId: UserId,
        val chatId: ChatId,
        val content: MessageContent,
        val deliveryAttemptsLeft: Int = 2,
        val timestamp: LocalDateTime = LocalDateTime.now(),
        val id: MessageId = MessageId(UUID.randomUUID())
    ) : Message()

    data class Remote(
        val id: MessageId,
        val senderId: UserId,
        val chatId: ChatId,
        val content: MessageContent,
        val timestamp: LocalDateTime = LocalDateTime.now()
    ) : Message()

    fun id() = when (this) { is Local -> id; is Remote -> id }
    fun chatId() = when (this) { is Local -> chatId; is Remote -> chatId }
    fun content() = when (this) { is Local -> content; is Remote -> content }
}

data class MessageContent(val text: String)
