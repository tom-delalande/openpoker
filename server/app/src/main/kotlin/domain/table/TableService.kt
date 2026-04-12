@file:OptIn(ExperimentalSerializationApi::class)

package domain.table

import app.logger
import domain.model.Table
import domain.tournament.CashGameRepository
import java.time.Instant
import java.util.UUID
import kotlin.random.Random
import kotlin.time.toKotlinInstant
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import server.PlayerActionRequest
import server.models.ActionOptionsBet
import server.models.ActionOptionsCall
import server.models.ActionOptionsCheck
import server.models.ActionOptionsFold
import server.models.ActionOptionsPostBigBlind
import server.models.ActionOptionsPostSmallBlind
import server.models.ActionOptionsRaise
import server.models.BetOption
import server.models.BetOptionType
import server.models.CallOption
import server.models.CallOptionType
import server.models.CheckOption
import server.models.CheckOptionType
import server.models.CommunityCardDealt
import server.models.CommunityCardDealtType
import server.models.FoldOption
import server.models.FoldOptionType
import server.models.HandEvent
import server.models.HandEventCommunityCardDealt
import server.models.HandEventHandStarted
import server.models.HandEventPlayerActionRequested
import server.models.HandEventPlayerBet
import server.models.HandEventPlayerCalled
import server.models.HandEventPlayerChecked
import server.models.HandEventPlayerFolded
import server.models.HandEventPlayerPostedBigBlind
import server.models.HandEventPlayerPostedSmallBlind
import server.models.HandEventPlayerRaised
import server.models.HandEventPlayerSatDown
import server.models.HandEventPlayerStoodUp
import server.models.HandEventPrivateCardDealt
import server.models.HandEventRoundStarted
import server.models.HandStarted
import server.models.HandStartedType
import server.models.PlayerActionRequested
import server.models.PlayerActionRequestedType
import server.models.PlayerBet
import server.models.PlayerBetType
import server.models.PlayerCalled
import server.models.PlayerCalledType
import server.models.PlayerChecked
import server.models.PlayerCheckedType
import server.models.PlayerFolded
import server.models.PlayerFoldedType
import server.models.PlayerPostedBigBlind
import server.models.PlayerPostedBigBlindType
import server.models.PlayerPostedSmallBlind
import server.models.PlayerPostedSmallBlindType
import server.models.PlayerRaised
import server.models.PlayerRaisedType
import server.models.PlayerSatDown
import server.models.PlayerSatDownType
import server.models.PlayerStoodUp
import server.models.PlayerStoodUpType
import server.models.PostBigBlindOption
import server.models.PostBigBlindOptionType
import server.models.PostSmallBlind
import server.models.PostSmallBlindOption
import server.models.PostSmallBlindOptionType
import server.models.PrivateCardDealt
import server.models.PrivateCardDealtType
import server.models.RaiseOption
import server.models.RaiseOptionType
import server.models.RoundStarted
import server.models.RoundStartedType

class TableService(
    val activeRepository: ActiveTableStateRepository,
    val historicRepository: HandHistoryRepository,
    val sessions: Map<UUID, MutableSharedFlow<HandEvent>>,
) {
    suspend fun process(now: Instant = Instant.now(), seedGenerator: () -> Long = { Random.nextLong() }) {
        val tables = activeRepository.getActiveTables()
        tables.forEach { (tableId, table, sockets) ->
            val updated = table.processTable(now)
            val events = updated.toEvents()
            val updatedSockets = mutableListOf<Socket>()
            for (playerSocket in sockets) {
                val playerEvents = events
                    .drop(playerSocket.version)
                    .prepareForPlayer(playerSocket.playerId)

                val session = sessions[playerSocket.sessionId] ?: return@forEach
                for (event in playerEvents) {
                    session.emit(event)
                }
                updatedSockets.add(playerSocket.copy(version = events.size))
            }

            saveTable(tableId, updated, updatedSockets.toList())
        }
    }

    val mutex = Mutex()
    suspend fun addWebSocketConnection(playerId: Int, tableId: UUID, sessionId: UUID) {
        // TODO: [low] there has got a better way to prevent websockets from over-writting each other here -> Test case TODO [1]
        mutex.withLock {
            val table = activeRepository.get(tableId) ?: throw IllegalStateException()
            saveTable(tableId, table.table, table.playerSockets + Socket(playerId, sessionId, 0))
        }
    }


    fun receivePlayerActions(
        tableId: UUID,
        playerId: Int,
        actions: List<PlayerActionRequest>,
        now: Instant = Instant.now(),
    ) {
        val activeTable = activeRepository.get(tableId)!!
        val updated = actions.fold(activeTable.table) { table, action ->
            table.processPlayerAction(playerId, action, now)
        }
        saveTable(tableId, updated, activeTable.playerSockets)
    }

    fun saveTable(id: UUID, table: Table, playerSockets: List<Socket>) {
        if (table.isFinished) {
            historicRepository.saveHand(id, table)
            val nextHand = table.startNextHand()
            activeRepository.set(id, nextHand, playerSockets)
        } else {
            activeRepository.set(id, table, playerSockets)
        }
    }

    fun createOrJoin(id: UUID, player: CashGameRepository.Player) {
        val activeTable = activeRepository.get(id)
        val table = activeTable?.table ?: createTable()
        val updated = table.addPlayer(player)
        activeRepository.set(id, updated, activeTable?.playerSockets ?: emptyList())
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun Table.toEvents(): List<HandEvent> = buildList {
        players.forEachIndexed { index, player ->
            add(
                HandEventPlayerSatDown(
                    value = PlayerSatDown(
                        type = PlayerSatDownType.PLAYER_SAT_DOWN_EVENT,
                        playerId = player.id,
                        playerName = player.name,
                        stack = player.stack,
                        seat = index,
                    )
                )
            )
        }
        rounds.forEach { round ->
            if (round.id == 0) {
                add(
                    HandEventHandStarted(
                        value = HandStarted(
                            type = HandStartedType.HAND_STARTED_EVENT,
                            dealerButton = dealerSeat,
                        )
                    )
                )
            }
            add(
                HandEventRoundStarted(
                    value = RoundStarted(
                        type = RoundStartedType.ROUND_STARTED_EVENT,
                        street = round.street.name
                    )
                )
            )

            round.actions.map { action ->
                when (action) {
                    is Table.Round.Action.DealCommunityCards -> add(
                        HandEventCommunityCardDealt(
                            value = CommunityCardDealt(
                                type = CommunityCardDealtType.COMMUNITY_CARD_DEALT_EVENT,
                                cards = action.cards.map { it.toStringValue() }
                            )
                        )
                    )

                    is Table.Round.Action.PlayerAction.AddChips -> TODO()
                    is Table.Round.Action.PlayerAction.Bet -> add(
                        HandEventPlayerBet(
                            value = PlayerBet(
                                type = PlayerBetType.PLAYER_BET,
                                amount = action.amount,
                                playerId = action.playerId,
                                isAllIn = action.isAllIn,
                            )
                        )
                    )

                    is Table.Round.Action.PlayerAction.Call -> add(
                        HandEventPlayerCalled(
                            value = PlayerCalled(
                                type = PlayerCalledType.PLAYER_CALLED,
                                amount = action.amount,
                                playerId = action.playerId,
                                isAllIn = action.isAllIn,
                            )
                        )
                    )

                    is Table.Round.Action.PlayerAction.Check -> add(
                        HandEventPlayerChecked(
                            value = PlayerChecked(
                                type = PlayerCheckedType.PLAYER_CHECKED,
                                playerId = action.playerId,
                            )
                        )
                    )

                    is Table.Round.Action.PlayerAction.DealCards -> add(
                        HandEventPrivateCardDealt(
                            value = PrivateCardDealt(
                                type = PrivateCardDealtType.PRIVATE_CARD_DEALT_EVENT,
                                playerId = action.playerId,
                                cards = action.cards.map { it.toStringValue() }
                            )
                        )
                    )

                    is Table.Round.Action.PlayerAction.Fold -> add(
                        HandEventPlayerFolded(
                            value = PlayerFolded(
                                type = PlayerFoldedType.PLAYER_FOLDED,
                                playerId = action.playerId,
                            )
                        )
                    )

                    is Table.Round.Action.PlayerAction.MuckCards -> TODO()
                    is Table.Round.Action.PlayerAction.PostAnte -> TODO()
                    is Table.Round.Action.PlayerAction.PostBigBlind -> add(
                        HandEventPlayerPostedBigBlind(
                            value = PlayerPostedBigBlind(
                                type = PlayerPostedBigBlindType.PLAYER_POSTED_BIG_BLIND,
                                amount = action.amount,
                                playerId = action.playerId,
                                isAllIn = action.isAllIn,
                            )
                        )
                    )

                    is Table.Round.Action.PlayerAction.PostSmallBlind -> add(
                        HandEventPlayerPostedSmallBlind(
                            value = PlayerPostedSmallBlind(
                                type = PlayerPostedSmallBlindType.PLAYER_POSTED_SMALL_BLIND,
                                amount = action.amount,
                                playerId = action.playerId,
                                isAllIn = action.isAllIn,
                            )
                        )
                    )

                    is Table.Round.Action.PlayerAction.RequestAction -> add(
                        HandEventPlayerActionRequested(
                            value = PlayerActionRequested(
                                type = PlayerActionRequestedType.PLAYER_ACTION_REQUESTED,
                                playerId = action.playerId,
                                expiry = action.expiry.toKotlinInstant(),
                                actionOptions = action.actionOptions.map {
                                    when (it) {
                                        is Table.Round.Action.PlayerAction.RequestAction.ActionOption.Bet -> ActionOptionsBet(
                                            value = BetOption(
                                                type = BetOptionType.BET_OPTION,
                                                minAmount = it.minAmount,
                                                maxAmount = it.maxAmount,
                                            )
                                        )

                                        is Table.Round.Action.PlayerAction.RequestAction.ActionOption.Call -> ActionOptionsCall(
                                            value = CallOption(
                                                type = CallOptionType.CALL_OPTION,
                                                amount = it.amount,
                                            )
                                        )

                                        Table.Round.Action.PlayerAction.RequestAction.ActionOption.Check -> ActionOptionsCheck(
                                            value = CheckOption(
                                                type = CheckOptionType.CHECK_OPTION,
                                            )
                                        )

                                        Table.Round.Action.PlayerAction.RequestAction.ActionOption.Fold -> ActionOptionsFold(
                                            value = FoldOption(
                                                type = FoldOptionType.FOLD_OPTION,
                                            )
                                        )

                                        is Table.Round.Action.PlayerAction.RequestAction.ActionOption.Raise -> ActionOptionsRaise(
                                            value = RaiseOption(
                                                type = RaiseOptionType.RAISE_OPTION,
                                                minAmount = it.minAmount,
                                                maxAmount = it.maxAmount,
                                            )
                                        )

                                        is Table.Round.Action.PlayerAction.RequestAction.ActionOption.PostBigBlind -> ActionOptionsPostBigBlind(
                                            value = PostBigBlindOption(
                                                type = PostBigBlindOptionType.POST_BIG_BLIND_OPTION,
                                                amount = it.amount,
                                            )
                                        )

                                        is Table.Round.Action.PlayerAction.RequestAction.ActionOption.PostSmallBlind -> ActionOptionsPostSmallBlind(
                                            value = PostSmallBlindOption(
                                                type = PostSmallBlindOptionType.POST_SMALL_BLIND_OPTION,
                                                amount = it.amount,
                                            )
                                        )

                                        is Table.Round.Action.PlayerAction.RequestAction.ActionOption.PostDeadBlind -> TODO()
                                        is Table.Round.Action.PlayerAction.RequestAction.ActionOption.PostExtraBlind -> TODO()
                                        is Table.Round.Action.PlayerAction.RequestAction.ActionOption.PostStraddle -> TODO()
                                        is Table.Round.Action.PlayerAction.RequestAction.ActionOption.PostAnte -> TODO()
                                        Table.Round.Action.PlayerAction.RequestAction.ActionOption.MuckCards -> TODO()
                                        Table.Round.Action.PlayerAction.RequestAction.ActionOption.ShowCards -> TODO()
                                    }
                                }
                            )
                        )
                    )

                    is Table.Round.Action.PlayerAction.SitDown -> add(
                        HandEventPlayerSatDown(
                            value = PlayerSatDown(
                                type = PlayerSatDownType.PLAYER_SAT_DOWN_EVENT,
                                playerId = action.playerId,
                                playerName = action.playerName,
                                stack = action.stack,
                                seat = action.seat
                            )
                        )
                    )

                    is Table.Round.Action.PlayerAction.StandUp -> add(
                        HandEventPlayerStoodUp(
                            value = PlayerStoodUp(
                                type = PlayerStoodUpType.PLAYER_STOOD_UP_EVENT,
                                playerId = action.playerId,
                            )
                        )
                    )

                    is Table.Round.Action.PlayerAction.Raise -> add(
                        HandEventPlayerRaised(
                            value = PlayerRaised(
                                type = PlayerRaisedType.PLAYER_RAISED,
                                amount = action.amount,
                                playerId = action.playerId,
                                isAllIn = action.isAllIn,
                            )
                        )
                    )

                    is Table.Round.Action.PlayerAction.PostDeadBlind -> TODO()
                    is Table.Round.Action.PlayerAction.PostExtraBlind -> TODO()
                    is Table.Round.Action.PlayerAction.PostStraddle -> TODO()
                    is Table.Round.Action.PlayerAction.ShowCards -> TODO()
                }
            }
        }
    }

    fun Table.Card.toStringValue(): String {
        val suit = when (suit) {
            Table.Card.Suit.Hearts -> "h"
            Table.Card.Suit.Diamonds -> "d"
            Table.Card.Suit.Spades -> "s"
            Table.Card.Suit.Clubs -> "c"
        }
        return "$rank$suit"
    }

    fun List<HandEvent>.prepareForPlayer(playerId: Int) = map { event ->
        when (event) {
            is HandEventPrivateCardDealt -> if (event.value.playerId == playerId) event else HandEventPrivateCardDealt(
                value = PrivateCardDealt(
                    type = PrivateCardDealtType.PRIVATE_CARD_DEALT_EVENT,
                    playerId = event.value.playerId,
                    cards = event.value.cards.map { "XX" }
                )
            )

            else -> event
        }
    }
}
