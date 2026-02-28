package com.talosross.summaryyou.ui

import kotlinx.serialization.Serializable

@Serializable
sealed interface Nav {
    @Serializable data object Home : Nav
    @Serializable data object Onboarding : Nav
    @Serializable data object History : Nav
    @Serializable data class Settings(val highlight: String? = null) : Nav
}