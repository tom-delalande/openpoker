package data.inmemory

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import server.AuthRepository

class InMemoryAuthRepository : AuthRepository {
    private val tokens: MutableMap<UUID, AuthRepository.PlayerInfo> = ConcurrentHashMap()

    override fun saveToken(token: UUID, playerInfo: AuthRepository.PlayerInfo) {
        tokens[token] = playerInfo
    }

    override fun getPlayer(token: UUID): AuthRepository.PlayerInfo? {
        return tokens[token]
    }
}