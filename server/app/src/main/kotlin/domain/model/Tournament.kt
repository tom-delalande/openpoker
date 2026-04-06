package domain.model

import java.util.UUID
import kotlin.time.Instant

data class Tournament(
    val id: UUID,
    val details: Details,
) {

    data class Details(
        val name: String,
        val startTime: Instant? = null,
        val type: TournamentType,
        val speed: Speed,
        val startingStack: Double,
        val players: List<Player>,
        val status: Status,
    ) {
        data class Player(val id: Int, val name: String, val startingStack: Double)
        enum class Status {
            Created,
            Started,
            Finished,
        }

        data class Speed(
            val type: SpeedType,
            val roundTime: Int,
        ) {
            enum class SpeedType {
                Normal,
                SemiTurbo,
                Turbo,
                SuperTurbo,
                HyperTurbo,
                UltraTurbo,
            }
        }

        enum class TournamentType {
            SingleTableTournament,
        }
    }
}
