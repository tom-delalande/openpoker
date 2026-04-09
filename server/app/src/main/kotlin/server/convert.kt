@file:OptIn(ExperimentalSerializationApi::class)

package server

import domain.model.Table
import domain.model.Table.Round.Action.PlayerAction.*
import kotlinx.serialization.ExperimentalSerializationApi
import server.models.PlayerAction
import server.models.PlayerActionBet
import server.models.PlayerActionCall
import server.models.PlayerActionCheck
import server.models.PlayerActionFold
import server.models.PlayerActionPostBigBlind
import server.models.PlayerActionPostSmallBlind
import server.models.PlayerActionRaise
import server.models.PlayerActionSitDown
import server.models.PlayerActionStandUp


fun PlayerAction.toDomain(playerId: Int): Table.Round.Action.PlayerAction = when (this) {
    is PlayerActionBet -> Bet(
        playerId = playerId,
        amount = value.amount,
        isAllIn = false, // TODO: [medium] set this somewhere
    )

    is PlayerActionCheck -> Check(
        playerId = playerId,
    )

    is PlayerActionFold -> Fold(
        playerId = playerId,
    )

    is PlayerActionPostSmallBlind -> PostSmallBlind(
        playerId = playerId,
        amount = value.amount,
        isAllIn = false, // TODO: [medium] set this somewhere
    )

    is PlayerActionRaise -> Raise(
        playerId = playerId,
        amount = value.amount,
        isAllIn = false, // TODO: [medium] set this somewhere
    )

    is PlayerActionPostBigBlind -> PostBigBlind(
        playerId = playerId,
        amount = value.amount,
        isAllIn = false, // TODO: [medium] set this somewhere
    )

    is PlayerActionSitDown -> SitDown(
        playerId = playerId,
        seat = 0, // TODO: [medium] set this somewhere
        playerName = "", // TODO
        stack = 0.0, // TODO
    )

    is PlayerActionStandUp -> StandUp(
        playerId = playerId,
    )

    is PlayerActionCall -> Call(
        playerId = playerId,
        amount = value.amount,
        isAllIn = false, // TODO: [medium] set this somewhere
    )
}