@file:OptIn(ExperimentalSerializationApi::class)

package server

import domain.model.Table
import domain.model.Table.Card
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


fun PlayerAction.toDomain(playerId: Int): PlayerActionRequest = when (this) {
    is PlayerActionBet -> PlayerActionRequest.Bet(
        playerId = playerId,
        amount = value.amount,
    )

    is PlayerActionCheck -> PlayerActionRequest.Check(
        playerId = playerId,
    )

    is PlayerActionFold -> PlayerActionRequest.Fold(
        playerId = playerId,
    )

    is PlayerActionPostSmallBlind -> PlayerActionRequest.PostSmallBlind(
        playerId = playerId,
        amount = value.amount,
    )

    is PlayerActionRaise -> PlayerActionRequest.Raise(
        playerId = playerId,
        amount = value.amount,
    )

    is PlayerActionPostBigBlind -> PlayerActionRequest.PostBigBlind(
        playerId = playerId,
        amount = value.amount,
    )

    is PlayerActionSitDown -> PlayerActionRequest.SitDown(
        playerId = playerId,
    )

    is PlayerActionStandUp -> PlayerActionRequest.StandUp(
        playerId = playerId,
    )

    is PlayerActionCall -> PlayerActionRequest.Call(
        playerId = playerId,
        amount = value.amount,
    )
}

sealed interface PlayerActionRequest {
    val playerId: Int

    data class MuckCards(override val playerId: Int) : PlayerActionRequest

    data class ShowCards(
        override val playerId: Int,
        val cards: List<Card>,
    ) : PlayerActionRequest

    data class PostAnte(
        override val playerId: Int,
        val amount: Double,
    ) : PlayerActionRequest

    data class PostSmallBlind(
        override val playerId: Int,
        val amount: Double,
    ) : PlayerActionRequest

    data class PostBigBlind(
        override val playerId: Int,
        val amount: Double,
    ) : PlayerActionRequest

    data class PostDeadBlind(
        override val playerId: Int,
        val amount: Double,
    ) : PlayerActionRequest

    data class PostExtraBlind(
        override val playerId: Int,
        val amount: Double,
    ) : PlayerActionRequest

    data class PostStraddle(
        override val playerId: Int,
        val amount: Double,
    ) : PlayerActionRequest

    data class Fold(
        override val playerId: Int,
    ) : PlayerActionRequest

    data class Check(
        override val playerId: Int,
    ) : PlayerActionRequest

    data class Bet(
        override val playerId: Int,
        val amount: Double,
    ) : PlayerActionRequest

    data class Raise(
        override val playerId: Int,
        val amount: Double,
    ) : PlayerActionRequest

    data class Call(
        override val playerId: Int,
        val amount: Double,
    ) : PlayerActionRequest

    data class SitDown(
        override val playerId: Int,
    ) : PlayerActionRequest

    data class StandUp(
        override val playerId: Int,
    ) : PlayerActionRequest
}