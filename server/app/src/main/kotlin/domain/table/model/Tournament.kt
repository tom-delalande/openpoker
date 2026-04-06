package domain.table.model

import java.util.UUID
import kotlin.time.Instant

data class Tournament(
    val id: UUID,
    val name: String,
    val startDate: Instant,
    val type: TournamentType,
    val speed: Speed,
    val initialStack: Double,


    ) {
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