package com.talosross.summaryyou

class APIKeyLibrary {
    companion object {
        init {
            System.loadLibrary("api-keys")
        }
        external fun getAPIKey(): String
        external fun getAPIKeyFree2(): String
        external fun getPaidAPIKey(): String
    }
}