@file:OptIn(ExperimentalSerializationApi::class)

package domain.table

import domain.model.Table
import domain.model.Table.Card.Suit
import domain.model.Table.Pot
import domain.model.Table.Round.Action.PlayerAction.Bet
import domain.model.Table.Round.Action.PlayerAction.Call
import domain.model.Table.Round.Action.PlayerAction.Check
import domain.model.Table.Round.Action.PlayerAction.DealCards
import domain.model.Table.Round.Action.PlayerAction.Fold
import domain.model.Table.Round.Action.PlayerAction.PostBigBlind
import domain.model.Table.Round.Action.PlayerAction.PostSmallBlind
import domain.model.Table.Round.Action.PlayerAction.Raise
import domain.model.Table.Round.Action.PlayerAction.RequestAction
import domain.model.Table.Round.Action.PlayerAction.RequestAction.ActionOption
import domain.model.Table.Round.Action.PlayerAction.ShowCards
import kotlin.test.assertEquals
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.jupiter.api.Test
import server.PlayerActionRequest

class TableLogicTest {
    @Test
    fun `given table with set seed and 3 players, when dealing initial cards, then 6 cards are dealt`() {
        val initialTable = givenWellKnownTournamentTable {
            withSeed(1)
            withDefaultPlayers(3)
        }

        val table = initialTable.dealInitialCards()

        assertEquals(1, table.rounds.size)
        assertEquals(
            Table.Round(
                id = 0,
                street = Table.Round.Street.PreFlop,
                actions = listOf(
                    DealCards(
                        2,
                        listOf(
                            Table.Card(Suit.Diamonds, 11),
                            Table.Card(Suit.Spades, 1),
                        )
                    ),
                    DealCards(
                        3,
                        listOf(
                            Table.Card(Suit.Hearts, 1),
                            Table.Card(Suit.Diamonds, 2),
                        )
                    ),
                    DealCards(
                        1,
                        listOf(
                            Table.Card(Suit.Spades, 10),
                            Table.Card(Suit.Clubs, 12),
                        )
                    )
                ),
            ),
            table.rounds[0]
        )
    }

    @Test
    fun `given table with dealt cards, when processing table, then action requested with small blind or fold`() {
        val initialTable = givenWellKnownTournamentTable {
            withSeed(1)
            withDefaultPlayers(3)
            withDealtCards()
        }

        val table = initialTable.processTable(wellKnownTimestamp)

        assertEquals(4, table.rounds[0].actions.size)
        assertEquals(
            RequestAction(
                playerId = 2,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.PostSmallBlind(amount = DEFAULT_SMALL_BLIND_AMOUNT)
                ),
                expiry = wellKnownTimestamp.plusSeconds(10),
            ),
            table.rounds[0].actions[3],
        )
    }

    @Test
    fun `given table with dealt cards and small blind action requested, when processing table after timeout, then small blind folds and big blind requested`() {
        val initialTable = givenWellKnownTournamentTable {
            withSeed(1)
            withDefaultPlayers(3)
            withDealtCards()
            withAction(
                RequestAction(
                    2,
                    actionOptions = listOf(ActionOption.Fold, ActionOption.PostSmallBlind(DEFAULT_SMALL_BLIND_AMOUNT)),
                    expiry = wellKnownTimestamp.plusSeconds(DEFAULT_TIMEOUT_IN_SECONDS)
                )
            )
        }

        val table = initialTable.processTable(wellKnownTimestamp.plusSeconds(DEFAULT_TIMEOUT_IN_SECONDS + 1))

        assertEquals(6, table.rounds[0].actions.size)
        assertEquals(
            Fold(
                playerId = 2,
            ),
            table.rounds[0].actions[4],
        )
        assertEquals(
            RequestAction(
                playerId = 3,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.PostBigBlind(amount = DEFAULT_BIG_BLIND_AMOUNT)
                ),
                expiry = wellKnownTimestamp.plusSeconds(21),
            ),
            table.rounds[0].actions[5],
        )
    }

    @Test
    fun `given table with dealt cards and small blind action requested, when player posts small blind, then small blind posted and big blind requested`() {
        val initialTable = givenWellKnownTournamentTable {
            withSeed(1)
            withDefaultPlayers(3)
            withDealtCards()
            withAction(
                RequestAction(
                    2,
                    actionOptions = listOf(ActionOption.Fold, ActionOption.PostSmallBlind(DEFAULT_SMALL_BLIND_AMOUNT)),
                    expiry = wellKnownTimestamp.plusSeconds(DEFAULT_TIMEOUT_IN_SECONDS)
                )
            )
        }

        val table = initialTable.processPlayerAction(
            2,
            PlayerActionRequest.PostSmallBlind(2, DEFAULT_SMALL_BLIND_AMOUNT),
            wellKnownTimestamp,
        )

        assertEquals(6, table.rounds[0].actions.size)
        assertEquals(
            PostSmallBlind(
                playerId = 2,
                amount = DEFAULT_SMALL_BLIND_AMOUNT,
                isAllIn = false,
            ),
            table.rounds[0].actions[4],
        )
        assertEquals(
            RequestAction(
                playerId = 3,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.PostBigBlind(amount = DEFAULT_BIG_BLIND_AMOUNT)
                ),
                expiry = wellKnownTimestamp.plusSeconds(10),
            ),
            table.rounds[0].actions[5],
        )
    }

//    @Test
    fun `given table with big blind requested, when big blind posts, then under the gun has actions requested`() {
        val initialTable = givenWellKnownTournamentTable {
            withSeed(1)
            withDefaultPlayers(3)
            withDealtCards()
            withAction(PostSmallBlind(2, DEFAULT_SMALL_BLIND_AMOUNT, false))
            withAction(
                RequestAction(
                    3,
                    actionOptions = listOf(ActionOption.Fold, ActionOption.PostBigBlind(DEFAULT_BIG_BLIND_AMOUNT)),
                    expiry = wellKnownTimestamp.plusSeconds(DEFAULT_TIMEOUT_IN_SECONDS)
                )
            )
        }

        val table = initialTable.processPlayerAction(
            playerId = 3,
            PlayerActionRequest.PostBigBlind(3, DEFAULT_BIG_BLIND_AMOUNT),
            wellKnownTimestamp,
        )

        assertEquals(7, table.rounds[0].actions.size)
        assertEquals(
            PostBigBlind(
                playerId = 3,
                amount = DEFAULT_BIG_BLIND_AMOUNT,
                isAllIn = false,
            ),
            table.rounds[0].actions[5],
        )
        assertEquals(
            RequestAction(
                playerId = 1,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Raise(
                        minAmount = DEFAULT_BIG_BLIND_AMOUNT + DEFAULT_BIG_BLIND_AMOUNT,
                        maxAmount = 1000.0
                    ),
                    ActionOption.Call(amount = DEFAULT_BIG_BLIND_AMOUNT),
                ),
                expiry = wellKnownTimestamp.plusSeconds(10),
            ),
            table.rounds[0].actions[6],
        )
    }

    @Test
    fun `given table with under the gun requested, when utg checks, then small blind player action requested`() {
        val initialTable = givenWellKnownTournamentTable {
            withSeed(1)
            withDefaultPlayers(3)
            withDealtCards()
            withAction(PostSmallBlind(2, DEFAULT_SMALL_BLIND_AMOUNT, false))
            withAction(PostBigBlind(3, DEFAULT_BIG_BLIND_AMOUNT, false))
            withAction(
                RequestAction(
                    1,
                    actionOptions = listOf(
                        ActionOption.Fold, ActionOption.Call(amount = DEFAULT_BIG_BLIND_AMOUNT),
                        ActionOption.Raise(minAmount = 5.0, maxAmount = null)
                    ),
                    expiry = wellKnownTimestamp.plusSeconds(DEFAULT_TIMEOUT_IN_SECONDS)
                )
            )
        }

        val table = initialTable.processPlayerAction(
            playerId = 1,
            PlayerActionRequest.Call(1, amount = DEFAULT_BIG_BLIND_AMOUNT),
            wellKnownTimestamp,
        )

        assertEquals(8, table.rounds[0].actions.size)
        assertEquals(
            RequestAction(
                playerId = 2,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Call(amount = DEFAULT_SMALL_BLIND_AMOUNT),
                    // TODO: [medium] check is this correct – does a bet count existing bets in the amount or is just the delta
                    // I think bet's are per street
                    ActionOption.Raise(
                        minAmount = 15.0,
                        maxAmount = 995.0
                    ),
                ),
                expiry = wellKnownTimestamp.plusSeconds(10),
            ),
            table.rounds[0].actions[7],
        )
    }


    @Test
    fun `given utg and small blind checks, when big blind checks, then round is finished and community cards are dealt and small blind action requested`() {
        val initialTable = givenWellKnownTournamentTable {
            withSeed(1)
            withDefaultPlayers(3)
            withDealtCards()
            withAction(PostSmallBlind(2, DEFAULT_SMALL_BLIND_AMOUNT, false))
            withAction(PostBigBlind(3, DEFAULT_BIG_BLIND_AMOUNT, false))
            withAction(Check(1))
            withAction(Check(2))
            withAction(
                RequestAction(
                    3,
                    actionOptions = listOf(ActionOption.Check),
                    expiry = wellKnownTimestamp.plusSeconds(DEFAULT_TIMEOUT_IN_SECONDS)
                )
            )
        }

        val table = initialTable.processPlayerAction(
            playerId = 3,
            PlayerActionRequest.Check(3),
            wellKnownTimestamp,
        )

        assertEquals(2, table.rounds.size)
        assertEquals(2, table.rounds[1].actions.size)
        assertEquals(
            Table.Round.Action.DealCommunityCards(
                listOf(
                    Table.Card(suit = Suit.Diamonds, rank = 8),
                    Table.Card(suit = Suit.Spades, rank = 4),
                    Table.Card(suit = Suit.Clubs, rank = 11)
                )
            ),
            table.rounds[1].actions[0],
        )
        assertEquals(
            RequestAction(
                playerId = 2,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Check,
                    ActionOption.Bet(minAmount = DEFAULT_BIG_BLIND_AMOUNT, maxAmount = 995.0)
                ),
                expiry = wellKnownTimestamp.plusSeconds(10),
            ),
            table.rounds[1].actions[1],
        )
    }

    @Test
    fun `given utg folds, when sb folds, then round is finished and big blind wins the pot`() {
        val initialTable = givenWellKnownTournamentTable {
            withSeed(1)
            withDefaultPlayers(3)
            withDealtCards()
            withAction(PostSmallBlind(2, DEFAULT_SMALL_BLIND_AMOUNT, false))
            withAction(PostBigBlind(3, DEFAULT_BIG_BLIND_AMOUNT, false))
            withAction(Fold(1))
            withAction(
                RequestAction(
                    2,
                    actionOptions = listOf(ActionOption.Fold),
                    expiry = wellKnownTimestamp.plusSeconds(DEFAULT_TIMEOUT_IN_SECONDS)
                )
            )
        }

        val table = initialTable.processPlayerAction(
            playerId = 2,
            PlayerActionRequest.Fold(2),
            wellKnownTimestamp,
        )

        assertEquals(true, table.isFinished)
        assertEquals(3, table.pots.first().playerWins.first().playerId)
    }

    @Test
    fun `complete hand, call call call pre-flop, check bet raise call call post flop, check bet fold call post turn, and check check post river, showdown, winners`() {
        val seedGenerator = { 1L }
        var table = givenWellKnownTournamentTable {
            withSeed(1)
            withDealerSeat(0)
            withDefaultPlayers(3)
            withBlinds(
                smallBlind = 5.0,
                bigBlind = 10.0,
            )
        }

        var now = wellKnownTimestamp
        table = table.processTable(now, seedGenerator)
        table = table.processTable(now, seedGenerator)

        // Player Order

        assertEquals(0, table.livePlayers[0].seat)
        assertEquals(1, table.livePlayers[0].playerId)

        assertEquals(1, table.livePlayers[1].seat)
        assertEquals(2, table.livePlayers[1].playerId)

        assertEquals(2, table.livePlayers[2].seat)
        assertEquals(3, table.livePlayers[2].playerId)

        // Small Blind

        assertEquals(
            RequestAction(
                playerId = 2,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.PostSmallBlind(amount = 5.0)
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[0].actions[0],
        )

        table = table.processPlayerAction(
            playerId = 2,
            action = PlayerActionRequest.PostSmallBlind(playerId = 2, amount = 5.0),
            now = now
        )

        assertEquals(
            PostSmallBlind(
                playerId = 2,
                amount = 5.0,
                isAllIn = false,
            ),
            table.rounds[0].actions[1],
        )

        // Big Blind

        table = table.processTable(now, seedGenerator)

        assertEquals(
            RequestAction(
                playerId = 3,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.PostBigBlind(amount = 10.0)
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[0].actions[2],
        )

        table = table.processPlayerAction(
            playerId = 3,
            action = PlayerActionRequest.PostBigBlind(playerId = 3, amount = 10.0),
            now = now
        )

        assertEquals(
            PostBigBlind(
                playerId = 3,
                amount = 10.0,
                isAllIn = false,
            ),
            table.rounds[0].actions[3],
        )

        // Private Cards
        table = table.processTable(now, seedGenerator)

        assertEquals(
            DealCards(
                playerId = 2,
                cards = listOf("11d".c(), "1s".c()),
            ),
            table.rounds[0].actions[4],
        )

        assertEquals(
            DealCards(
                playerId = 3,
                cards = listOf("1h".c(), "2d".c()),
            ),
            table.rounds[0].actions[5],
        )

        assertEquals(
            DealCards(
                playerId = 1,
                cards = listOf("10s".c(), "12c".c()),
            ),
            table.rounds[0].actions[6],
        )

        // UTG
        assertEquals(
            RequestAction(
                playerId = 1,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Call(amount = 10.0),
                    ActionOption.Raise(minAmount = 20.0, maxAmount = 1000.0)
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[0].actions[7],
        )

        table = table.processPlayerAction(
            playerId = 1,
            action = PlayerActionRequest.Call(playerId = 1, amount = 10.0),
            now = now
        )

        assertEquals(
            Call(
                playerId = 1,
                amount = 10.0,
                isAllIn = false,
            ),
            table.rounds[0].actions[8],
        )

        // Small Blind calls BB amount
        table = table.processTable(now, seedGenerator)
        assertEquals(
            RequestAction(
                playerId = 2,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Call(amount = 5.0),
                    ActionOption.Raise(minAmount = 15.0, maxAmount = 995.0)
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[0].actions[9],
        )

        table = table.processPlayerAction(
            playerId = 2,
            action = PlayerActionRequest.Call(playerId = 2, amount = 5.0),
            now = now
        )

        assertEquals(
            Call(
                playerId = 2,
                amount = 5.0,
                isAllIn = false,
            ),
            table.rounds[0].actions[10],
        )

        // Big blind checks
        table = table.processTable(now, seedGenerator)
        assertEquals(
            RequestAction(
                playerId = 3,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Check,
                    // TODO: [low] still check this amount is right
                    ActionOption.Raise(minAmount = 10.0, maxAmount = 990.0)
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[0].actions[11],
        )

        table = table.processPlayerAction(playerId = 3, action = PlayerActionRequest.Check(playerId = 3), now = now)
        assertEquals(Check(playerId = 3), table.rounds[0].actions[12])

        // -- FLOP --
        // Community Cards Dealt
        table = table.processTable(now, seedGenerator)
        assertEquals(13, table.rounds[0].actions.size)
        assertEquals(
            Table.Round.Action.DealCommunityCards(
                cards = listOf("8d".c(), "4s".c(), "11c".c())
            ),
            table.rounds[1].actions[0],
        )

        // Small Blind Bets
        assertEquals(
            RequestAction(
                playerId = 2,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Check,
                    ActionOption.Bet(minAmount = 10.0, maxAmount = 990.0)
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[1].actions[1],
        )
        table = table.processPlayerAction(playerId = 2, action = PlayerActionRequest.Check(playerId = 2), now = now)
        assertEquals(Check(playerId = 2), table.rounds[1].actions[2])

        // Big Blind Bets
        table = table.processTable(now, seedGenerator)
        assertEquals(
            RequestAction(
                playerId = 3,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Check,
                    ActionOption.Bet(minAmount = 10.0, maxAmount = 990.0)
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[1].actions[3],
        )
        table = table.processPlayerAction(3, PlayerActionRequest.Bet(3, 10.0), now)
        assertEquals(Bet(3, 10.0, false), table.rounds[1].actions[4])

        // Dealer Raises
        table = table.processTable(now, seedGenerator)
        assertEquals(
            RequestAction(
                playerId = 1,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Call(amount = 10.0),
                    ActionOption.Raise(minAmount = 20.0, maxAmount = 990.0)
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[1].actions[5],
        )
        table = table.processPlayerAction(1, PlayerActionRequest.Raise(1, 20.0), now)
        assertEquals(Raise(1, 20.0, false), table.rounds[1].actions[6])

        // Small Blind Calls
        table = table.processTable(now, seedGenerator)
        assertEquals(
            RequestAction(
                playerId = 2,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Call(amount = 20.0),
                    ActionOption.Raise(minAmount = 30.0, maxAmount = 990.0)
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[1].actions[7],
        )
        table = table.processPlayerAction(2, PlayerActionRequest.Call(2, 20.0), now)
        assertEquals(Call(2, 20.0, false), table.rounds[1].actions[8])

        // Big Blind Calls
        table = table.processTable(now, seedGenerator)
        assertEquals(
            RequestAction(
                playerId = 3,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Call(amount = 10.0),
                    ActionOption.Raise(minAmount = 20.0, maxAmount = 980.0)
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[1].actions[9],
        )
        table = table.processPlayerAction(3, PlayerActionRequest.Call(3, 10.0), now)
        assertEquals(Call(3, 10.0, false), table.rounds[1].actions[10])

        // -- TURN --
        // Community Card Dealt
        table = table.processTable(now, seedGenerator)
        assertEquals(11, table.rounds[1].actions.size)
        assertEquals(
            Table.Round.Action.DealCommunityCards(
                cards = listOf("3d".c())
            ),
            table.rounds[2].actions[0],
        )
        // Small Blind Checks
        assertEquals(
            RequestAction(
                playerId = 2,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Check,
                    ActionOption.Bet(minAmount = 10.0, maxAmount = 970.0)
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[2].actions[1],
        )
        table = table.processPlayerAction(2, PlayerActionRequest.Check(2), now)
        assertEquals(Check(2), table.rounds[2].actions[2])

        // Big Blind Bets
        table = table.processTable(now, seedGenerator)
        assertEquals(
            RequestAction(
                playerId = 3,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Check,
                    ActionOption.Bet(minAmount = 10.0, maxAmount = 970.0)
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[2].actions[3],
        )
        table = table.processPlayerAction(3, PlayerActionRequest.Bet(3, amount = 10.0), now)
        assertEquals(Bet(3, 10.0, false), table.rounds[2].actions[4])

        // Dealer Folds
        table = table.processTable(now, seedGenerator)
        assertEquals(
            RequestAction(
                playerId = 1,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Call(amount = 10.0),
                    ActionOption.Raise(minAmount = 20.0, maxAmount = 970.0)
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[2].actions[5],
        )
        table = table.processPlayerAction(1, PlayerActionRequest.Fold(1), now)
        assertEquals(Fold(1), table.rounds[2].actions[6])

        // Small Blind Calls
        table = table.processTable(now, seedGenerator)
        assertEquals(
            RequestAction(
                playerId = 2,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Call(amount = 10.0),
                    ActionOption.Raise(minAmount = 20.0, maxAmount = 970.0)
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[2].actions[7],
        )
        table = table.processPlayerAction(2, PlayerActionRequest.Call(2, amount = 10.0), now)
        assertEquals(Call(2, amount = 10.0, false), table.rounds[2].actions[8])

        // -- River --
        // Community Card Dealt
        table = table.processTable(now, seedGenerator)
        assertEquals(9, table.rounds[2].actions.size)
        assertEquals(
            Table.Round.Action.DealCommunityCards(
                cards = listOf("6s".c())
            ),
            table.rounds[3].actions[0],
        )

        assertEquals(
            RequestAction(
                playerId = 2,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Check,
                    ActionOption.Bet(minAmount = 10.0, maxAmount = 960.0)
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[3].actions[1],
        )
        table = table.processPlayerAction(2, PlayerActionRequest.Check(2), now)
        assertEquals(Check(2), table.rounds[3].actions[2])

        assertEquals(
            RequestAction(
                playerId = 3,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Check,
                    ActionOption.Bet(minAmount = 10.0, maxAmount = 960.0)
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[3].actions[3],
        )
        table = table.processPlayerAction(3, PlayerActionRequest.Check(3), now)
        assertEquals(Check(3), table.rounds[3].actions[4])

        // -- Showdown --
        // Showdown
        table = table.processTable(now, seedGenerator)
        assertEquals(5, table.rounds[3].actions.size)
        assertEquals(
            ShowCards(
                playerId = 3,
                cards = listOf("1h".c(), "2d".c())
            ),
            table.rounds[4].actions[0],
        )
        assertEquals(
            ShowCards(
                playerId = 2,
                cards = listOf("11d".c(), "1s".c())
            ),
            table.rounds[4].actions[1],
        )

        // Small Blind Wins
        assertEquals(true, table.isFinished)
        assertEquals(
            listOf(
                Pot(number = 0, amount = 110.0, jackpot = 0.0, playerWins = listOf(Pot.PlayerWin(playerId = 2, winAmount = 110.0)))
            ), table.pots
        )
    }

    // TODO: add a test with early all ins
    // TODO: add a test with early folds
    // TODO: add a test with 2 winners
    // TODO: add a test with split pot (all in with smaller stack and wins)
    // TODO: add a test for wrong action
    // TODO: add a test for acting out of turn
    // TODO: add a test for action with wrong amount


    fun String.c(): Table.Card {
        val suit = when (last()) {
            'h' -> Suit.Hearts
            'd' -> Suit.Diamonds
            's' -> Suit.Spades
            'c' -> Suit.Clubs
            else -> throw IllegalStateException()
        }
        val rank = dropLast(1).toInt()
        return Table.Card(suit, rank)
    }
}
