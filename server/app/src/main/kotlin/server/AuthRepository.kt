@file:UseSerializers(UUIDSerializer::class)

package server

import common.UUIDSerializer
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

interface AuthRepository {
    fun saveToken(token: UUID, playerInfo: PlayerInfo)
    fun getPlayer(token: UUID): PlayerInfo?

    @Serializable
    data class PlayerInfo(
        val playerId: Int,
        val playerName: String,
    )
}