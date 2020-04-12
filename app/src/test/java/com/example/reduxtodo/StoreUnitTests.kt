package com.example.reduxtodo

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class StoreUnitTests {

    @Test
    fun `action updates state`() = runBlockingTest {
        fun reducer(scope: CoroutineScope, action: Int, state: Int): Tuple<Int, Command<Int>> {
            return Tuple(state + action, Command.None())
        }

        // state - counter
        // action - increment
        var store = Store<Int, Int>(
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
                incCommand -> return Tuple(state, Command.Action(inc))
                inc -> return Tuple(state + 1, Command.None())
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
                    val `2 increments` = Command.Batch<Int>(listOf(
                        Command.Action(inc), Command.Action(inc)
                    ))
                    return Tuple(state, `2 increments`)
                }
                inc -> return Tuple(state + 1, Command.None())
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
                    return Tuple(state, incWithDelay)
                }
                inc -> return Tuple(state + 1, Command.None())
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