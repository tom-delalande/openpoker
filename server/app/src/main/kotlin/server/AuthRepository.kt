package server

interface AuthRepository {
    fun saveToken(token: String, playerId: Int, playerName: String)
    fun getPlayer(token: String): PlayerInfo?

    data class PlayerInfo(
        val playerId: Int,
        val playerName: String
    )
}