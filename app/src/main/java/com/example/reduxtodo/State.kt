package com.example.reduxtodo
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.util.*
import kotlin.coroutines.CoroutineContext

// MARK: redux library
class Store<State, Action>(
    var state: State,
    val reducer: (Action, State) -> Tuple<State, Command<Action>>,
    // a function will be passed that will need a way to dispatch actions
    // we are in charge of injecting this action handling function (dispatch)
    private val  subscription: ((Action) -> Unit) -> Unit = {},
    private val coroutineScope: CoroutineScope = MainScope()
) {
    init {
        // need to subscribe to updates if any
        subscription(::dispatch)
    }

    fun dispatch(action: Action) {
        coroutineScope.launch {
            val (newState, command) = reducer(action, state)
            state = newState

            // dispatch all actions
            command.actions().forEach {
                dispatch(it.action)
            }

            // start newly created subscribtions
            command.flows().forEach {
                it.subscription(::dispatch)
            }
        }
    }
}

// MARK: Our state and reducer
data class AppState(
    val apiClient: ApiClient,
    val localUserId: UserId,
    val messages: List<Message>,
    val chats: List<ChatId>
)

data class Tuple<First, Second>(val first: First, val second: Second)

sealed class Command<Action> {
    data class Action<Action>(val action: Action) : Command<Action>()
    data class Flow<Action>(val subscription: ((Action) -> Unit) -> Unit) : Command<Action>()
    data class Batch<Action>(val commands: List<Command<Action>>) : Command<Action>()
    class None<Action>() : Command<Action>()

    fun actions() : List<Command.Action<Action>> {
        return when (this) {
            is Command.Action -> listOf(this)
            is Batch -> commands.flatMap { it.actions() }
            else -> listOf()
        }
    }

    fun flows() : List<Command.Flow<Action>> {
        return when (this) {
            is Flow -> listOf(this)
            is Batch -> commands.flatMap { it.flows() }
            else -> listOf()
        }
    }
}

// ideally the reducer will return something like this Flow<Action>
// that would support running
fun appReducer(action: Action, state: AppState) : Tuple<AppState, Command<Action>> {
    when (action) {
        is Action.SendMessage -> {
            val outgoingMessage = Message.Local(
                state.localUserId,
                action.chatId,
                action.messageContent
            )

            fun messageDeliveryUpdates(dispatch: (Action) -> Unit) : Unit {
                GlobalScope.launch {
                    var message = outgoingMessage.copy()
                    while (outgoingMessage.deliveryAttemptsLeft != 0) {
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

            return Tuple(
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
            val newMessages =
                action.updates.fold(
                    state.messages,
                    { messages, update ->
                        when (update) {
                            is Update.NewMessage -> messages + update.message
                            is Update.DeleteMessage -> messages.filterNot { it.messageId == update.messageId }
                        }
                    }
                )
            return Tuple(
                state.copy(messages = newMessages),
                Command.None()
            )
        }
    }
}

sealed class Action {
    data class SendMessage(val chatId: ChatId, val messageContent: MessageContent) : Action()
    // data class GetUpdates(val after: LocalDateTime) : Action()

    // WATCH OUT: whats tricky about this method is that its used both outside
    // of the reducer as well as a return command
    data class ApplyUpdates(val updates: List<Update>) : Action()
}

sealed class Update {
    data class NewMessage(val message: Message) : Update()
    // TODO: display delivery
    // data class MessageDelivered(val messageId: MessageId) : Update()

    // TODO: private
    data class DeleteMessage(val messageId: MessageId) : Update()
}

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

    val messageId: MessageId
        get() = when (this) { is Local -> this.id; is Remote -> this.id }
}

data class MessageContent(val text: String)