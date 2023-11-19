package com.talosross.summaryyou

class APIKeyLibrary {
    companion object {
        init {
            System.loadLibrary("api-keys")
        }
        external fun getAPIKey(): String
    }
}