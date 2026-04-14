package data.inmemory

import java.util.UUID
import server.AuthRepository

class InMemoryAuthRepository : AuthRepository {
    private val tokens = mutableMapOf<UUID, AuthRepository.PlayerInfo>()
    override fun saveToken(token: UUID, playerInfo: AuthRepository.PlayerInfo) {
        tokens[token] = playerInfo
    }

    override fun getPlayer(token: UUID): AuthRepository.PlayerInfo? {
        return tokens[token]
    }
}