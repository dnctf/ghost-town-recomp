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
        displayName = "Ghost Town Games",
        studioName = "Ghost Town Games / Team17",
        iconRes = R.drawable.ic_overcooked,
        accentColorHex = "#FF9F1C"
    ),
    SMG_STUDIO(
        devId = "smg_studio",
        displayName = "SMG Studio",
        studioName = "SMG Studio / DevM Games",
        iconRes = R.drawable.ic_moving_out,
        accentColorHex = "#00A8E8"
    );

    companion object {
        fun fromId(id: String): Developer =
            values().firstOrNull { it.devId.equals(id, ignoreCase = true) } ?: GHOST_TOWN_GAMES
    }
}
