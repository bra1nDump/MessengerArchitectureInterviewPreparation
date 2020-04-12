package com.example.reduxtodo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class Store<State, Action>(
    var state: State,
    val reducer: (CoroutineScope, Action, State) -> Tuple<State, Command<Action>>,
    private val coroutineScope: CoroutineScope = MainScope()
) {
    fun dispatch(action: Action) {
        coroutineScope.launch {
            val (newState, command) = reducer(coroutineScope, action, state)
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