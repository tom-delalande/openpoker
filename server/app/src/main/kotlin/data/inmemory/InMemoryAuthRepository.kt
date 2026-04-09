package data.inmemory

import server.AuthRepository

class InMemoryAuthRepository : AuthRepository {
    private val tokens = mutableMapOf<String, AuthRepository.PlayerInfo>()
    override fun saveToken(token: String, playerId: Int, playerName: String) {
        tokens[token] = AuthRepository.PlayerInfo(playerId, playerName)
    }

    override fun getPlayer(token: String): AuthRepository.PlayerInfo? {
        return tokens[token]
    }
}