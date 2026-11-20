package com.overcookedrecomp.app.compiler.core

import androidx.annotation.DrawableRes
import com.overcookedrecomp.app.R

/**
 * Representasi Pengembang Game Multi-Developer dalam Launcher Recomp
 * Setiap pengembang memiliki kumpulan compiler tersendiri untuk porting judul game.
 */
enum class Developer(
    val devId: String,
    val displayName: String,
    val studioName: String,
    @DrawableRes val iconRes: Int,
    val accentColorHex: String
) {
    GHOST_TOWN_GAMES(
        devId = "ghost_town",
        displayName = "SMG Studio / DevM Games",
        studioName = "SMG Studio / DevM Games (Team17 publisher)",
        iconRes = R.drawable.ic_overcooked,
        accentColorHex = "#FF9F1C"
    );

    companion object {
        fun fromId(id: String): Developer =
            values().firstOrNull { it.devId.equals(id, ignoreCase = true) } ?: GHOST_TOWN_GAMES
    }
}
