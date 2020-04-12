package com.example.reduxtodo

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class StoreUnitTests {

    @Test
    fun `action updates state`() = runBlockingTest {
        fun reducer(scope: CoroutineScope, action: Int, state: Int): Tuple<Int, Command<Int>> {
            return Tuple(state + action, Command.None())
        }

        // state - counter
        // action - increment
        val store = Store<Int, Int>(
            state = 0,
            reducer = ::reducer,
            coroutineScope = this
        )

        store.dispatch(1)
        assertEquals(1, store.state)
    }

    @Test
    fun `action returned in command changes state`() = runBlockingTest {
        val incCommand = 0
        val inc = 1
        fun reducer(scope: CoroutineScope, action: Int, state: Int): Tuple<Int, Command<Int>> {
            return when (action) {
                incCommand -> Tuple(state, Command.Action(inc))
                inc -> Tuple(state + 1, Command.None())
                else -> throw error("unexpected action")
            }
        }

        val store = Store(
            state = 0,
            reducer = ::reducer,
            coroutineScope = this
        )

        store.dispatch(incCommand)
        assertEquals(1, store.state)
    }

    @Test
    fun `batch command updates state`() = runBlockingTest {
        val incCommand = 0
        val inc = 1
        fun reducer(scope: CoroutineScope, action: Int, state: Int): Tuple<Int, Command<Int>> {
            return when (action) {
                incCommand -> {
                    val twoIncrements = Command.Batch<Int>(listOf(
                        Command.Action(inc), Command.Action(inc)
                    ))
                    Tuple(state, twoIncrements)
                }
                inc -> Tuple(state + 1, Command.None())
                else -> throw error("unexpected action")
            }
        }

        val store = Store(
            state = 0,
            reducer = ::reducer,
            coroutineScope = this
        )

        store.dispatch(incCommand)
        assertEquals(2, store.state)
    }

    @Test
    fun `flow command updates state`() = runBlockingTest {
        val incCommand = 0
        val inc = 1
        fun reducer(scope: CoroutineScope, action: Int, state: Int): Tuple<Int, Command<Int>> {
            return when (action) {
                incCommand -> {
                    val incWithDelay = Command.Flow<Int> { dispatch ->
                        this.launch {
                            delay(100)
                            dispatch(inc)
                            delay(100)
                            dispatch(inc)
                        }
                    }
                    Tuple(state, incWithDelay)
                }
                inc -> Tuple(state + 1, Command.None())
                else -> throw error("unexpected action")
            }
        }

        val store = Store(
            state = 0,
            reducer = ::reducer,
            coroutineScope = this
        )

        store.dispatch(incCommand)
        delay(300)
        assertEquals(2, store.state)
    }
}