package com.adarsh.spotify.other


// this class is for handling unnecessary event fired on error cases when sreen is rotated
open class Event<out T>(private val data: T) {

    // when we write private set below property declaration than  it means we can set the property within this class
    // only its only readable to other classes but that cant be updated from outside this class
    var hasBeenHandled = false
        private set


    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            data
        }
    }

    fun peekContent() = data
}