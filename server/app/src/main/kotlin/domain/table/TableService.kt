package domain.table

import domain.model.Table
import domain.model.Table.Round
import domain.model.Tournament
import java.time.Instant
import domain.model.Table.Round.Action.PlayerAction.RequestAction.ActionOption as ActionOption
import domain.model.Table.Round.Action.PlayerAction.*
import java.util.UUID
import kotlin.collections.indexOf
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class TableService(
    val activeRepository: ActiveTableStateRepository,
    val historicRepository: HandHistoryRepository,
) {
}
