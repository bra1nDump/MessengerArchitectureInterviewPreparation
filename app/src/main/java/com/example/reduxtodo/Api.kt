package com.example.reduxtodo

import kotlinx.coroutines.Deferred

sealed class Result<Result, Error>
class Ok<Result, Error>(val result: Result) : com.example.reduxtodo.Result<Result, Error>()
class Error<Result, Error>(val error: Error) : com.example.reduxtodo.Result<Result, Error>()

interface ApiClient {
    enum class Error { NoNetwork }

    fun sendAsync(message: Message.Local) : Deferred<Result<Message.Remote, Error>>

    // TODO: Once kotlin compose compiler fixes Flow support this should be a flow
    fun subscibeToUpdates(handleUpdates: (List<Update>) -> Unit) = {
        throw error("not implemented")
    }
}