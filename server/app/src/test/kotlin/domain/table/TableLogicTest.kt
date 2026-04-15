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
import domain.model.Table.Round.Action.PlayerAction.*
import domain.model.Table.Round.Action.*
import kotlin.test.assertEquals
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import server.PlayerActionRequest

class TableLogicTest {
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

        assertEquals(0, table.players[0].seat)
        assertEquals(1, table.players[0].playerId)

        assertEquals(1, table.players[1].seat)
        assertEquals(2, table.players[1].playerId)

        assertEquals(2, table.players[2].seat)
        assertEquals(3, table.players[2].playerId)


        // Small Blind

        var actionIndex = 8
        assertEquals(
            PostSmallBlind(
                playerId = 2,
                amount = 5.0,
                isAllIn = false,
            ),
            table.rounds[0].actions[actionIndex++],
        )

        // Big Blind
        assertEquals(
            PostBigBlind(
                playerId = 3,
                amount = 10.0,
                isAllIn = false,
            ),
            table.rounds[0].actions[actionIndex++],
        )

        // Private Cards
        table = table.processTable(now, seedGenerator)

        assertEquals(
            DealCards(
                playerId = 2,
                cards = listOf("10s".c(), "12c".c()),
            ),
            table.rounds[0].actions[actionIndex++],
        )

        assertEquals(
            DealCards(
                playerId = 3,
                cards = listOf("11d".c(), "1s".c()),
            ),
            table.rounds[0].actions[actionIndex++],
        )

        assertEquals(
            DealCards(
                playerId = 1,
                cards = listOf("1h".c(), "2d".c()),
            ),
            table.rounds[0].actions[actionIndex++],
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
            table.rounds[0].actions[actionIndex++],
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
            table.rounds[0].actions[actionIndex++],
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
            table.rounds[0].actions[actionIndex++],
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
            table.rounds[0].actions[actionIndex++],
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
            table.rounds[0].actions[actionIndex++],
        )

        table = table.processPlayerAction(playerId = 3, action = PlayerActionRequest.Check(playerId = 3), now = now)
        assertEquals(Check(playerId = 3), table.rounds[0].actions[actionIndex++])

        // -- FLOP --
        // Community Cards Dealt
        table = table.processTable(now, seedGenerator)
        assertEquals(actionIndex, table.rounds[0].actions.size)
        actionIndex = 1
        assertEquals(
            DealCommunityCards(
                cards = listOf("8d".c(), "4s".c(), "11c".c())
            ),
            table.rounds[1].actions[actionIndex++],
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
            table.rounds[1].actions[actionIndex++],
        )
        table = table.processPlayerAction(playerId = 2, action = PlayerActionRequest.Check(playerId = 2), now = now)
        assertEquals(Check(playerId = 2), table.rounds[1].actions[actionIndex++])

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
            table.rounds[1].actions[actionIndex++],
        )
        table = table.processPlayerAction(3, PlayerActionRequest.Bet(3, 10.0), now)
        assertEquals(Bet(3, 10.0, false), table.rounds[1].actions[actionIndex++])

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
            table.rounds[1].actions[actionIndex++],
        )
        table = table.processPlayerAction(1, PlayerActionRequest.Raise(1, 20.0), now)
        assertEquals(Raise(1, 20.0, false), table.rounds[1].actions[actionIndex++])

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
            table.rounds[1].actions[actionIndex++],
        )
        table = table.processPlayerAction(2, PlayerActionRequest.Call(2, 20.0), now)
        assertEquals(Call(2, 20.0, false), table.rounds[1].actions[actionIndex++])

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
            table.rounds[1].actions[actionIndex++],
        )
        table = table.processPlayerAction(3, PlayerActionRequest.Call(3, 10.0), now)
        assertEquals(Call(3, 10.0, false), table.rounds[1].actions[actionIndex++])


        // -- TURN --
        // Community Card Dealt
        actionIndex = 1
        table = table.processTable(now, seedGenerator)
        assertEquals(12, table.rounds[1].actions.size)
        assertEquals(
            DealCommunityCards(
                cards = listOf("3d".c())
            ),
            table.rounds[2].actions[actionIndex++],
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
            table.rounds[2].actions[actionIndex++],
        )
        table = table.processPlayerAction(2, PlayerActionRequest.Check(2), now)
        assertEquals(Check(2), table.rounds[2].actions[actionIndex++])

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
            table.rounds[2].actions[actionIndex++],
        )
        table = table.processPlayerAction(3, PlayerActionRequest.Bet(3, amount = 10.0), now)
        assertEquals(Bet(3, 10.0, false), table.rounds[2].actions[actionIndex++])

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
            table.rounds[2].actions[actionIndex++],
        )
        table = table.processPlayerAction(1, PlayerActionRequest.Fold(1), now)
        assertEquals(Fold(1), table.rounds[2].actions[actionIndex++])

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
            table.rounds[2].actions[actionIndex++],
        )
        table = table.processPlayerAction(2, PlayerActionRequest.Call(2, amount = 10.0), now)
        assertEquals(Call(2, amount = 10.0, false), table.rounds[2].actions[actionIndex++])

        // -- River --
        // Community Card Dealt
        actionIndex = 1
        table = table.processTable(now, seedGenerator)
        assertEquals(10, table.rounds[2].actions.size)
        assertEquals(
            DealCommunityCards(
                cards = listOf("6s".c())
            ),
            table.rounds[3].actions[actionIndex++],
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
            table.rounds[3].actions[actionIndex++],
        )
        table = table.processPlayerAction(2, PlayerActionRequest.Check(2), now)
        assertEquals(Check(2), table.rounds[3].actions[actionIndex++])

        table = table.processTable(now, seedGenerator)
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
            table.rounds[3].actions[actionIndex++],
        )
        table = table.processPlayerAction(3, PlayerActionRequest.Check(3), now)
        assertEquals(Check(3), table.rounds[3].actions[actionIndex++])

        // -- Showdown --
        // Showdown
        actionIndex = 0
        table = table.processTable(now, seedGenerator)
        assertEquals(6, table.rounds[3].actions.size)
        assertEquals(RoundStarted(id = 4, street = Table.Round.Street.Showdown), table.rounds[4].actions[actionIndex++])
        assertEquals(
            ShowCards(
                playerId = 3,
                cards = listOf("11d".c(), "1s".c()),
            ),
            table.rounds[4].actions[actionIndex++],
        )
        assertEquals(
            ShowCards(
                playerId = 2,
                cards = listOf("10s".c(), "12c".c()),
            ),
            table.rounds[4].actions[actionIndex++],
        )

        // Small Blind Wins
        assertEquals(true, table.isFinished)
        assertEquals(
            listOf(
                Pot(
                    number = 0,
                    amount = 110.0,
                    jackpot = 0.0,
                    playerWins = listOf(Pot.PlayerWin(playerId = 3, winAmount = 110.0))
                )
            ), table.pots
        )
    }

    @Test
    fun `complete hand, call call call pre-flop, check bet fold fold post flop, hand ended`() {
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

        // Pre-flop
        table = table.processPlayerAction(
            playerId = 1,
            action = PlayerActionRequest.Call(playerId = 1, amount = 10.0),
            now = now
        )
            .processTable(now, seedGenerator)

        table = table.processPlayerAction(
            playerId = 2,
            action = PlayerActionRequest.Call(playerId = 2, amount = 5.0),
            now = now
        )
            .processTable(now, seedGenerator)

        table = table.processPlayerAction(
            playerId = 3,
            action = PlayerActionRequest.Check(playerId = 3),
            now = now
        )
            .processTable(now, seedGenerator)

        assertEquals(
            listOf(
                SitDown(playerId = 1, playerName = "Player 1", seat = 0, stack = 1000.0),
                SitDown(playerId = 2, playerName = "Player 2", seat = 1, stack = 1000.0),
                SitDown(playerId = 3, playerName = "Player 3", seat = 2, stack = 1000.0),
                SitDown(playerId = 1, playerName = "Player 1", seat = 0, stack = 1000.0),
                SitDown(playerId = 2, playerName = "Player 2", seat = 1, stack = 1000.0),
                SitDown(playerId = 3, playerName = "Player 3", seat = 2, stack = 1000.0),
                HandStarted,
                RoundStarted(id = 0, street = Table.Round.Street.PreFlop),
                PostSmallBlind(playerId = 2, amount = 5.0, false),
                PostBigBlind(playerId = 3, amount = 10.0, false),
                DealCards(
                    playerId = 2,
                    cards = listOf("10s".c(), "12c".c()),
                ),
                DealCards(
                    playerId = 3,
                    cards = listOf("11d".c(), "1s".c()),
                ),

                DealCards(
                    playerId = 1,
                    cards = listOf("1h".c(), "2d".c()),
                ),
                RequestAction(
                    playerId = 1,
                    actionOptions = listOf(
                        ActionOption.Fold,
                        ActionOption.Call(amount = 10.0),
                        ActionOption.Raise(minAmount = 20.0, maxAmount = 1000.0)
                    ),
                    expiry = now.plusSeconds(10)
                ),
                Call(playerId = 1, amount = 10.0, false),
                RequestAction(
                    playerId = 2,
                    actionOptions = listOf(
                        ActionOption.Fold,
                        ActionOption.Call(amount = 5.0),
                        ActionOption.Raise(minAmount = 15.0, maxAmount = 995.0)
                    ),
                    expiry = now.plusSeconds(10)
                ),
                Call(playerId = 2, amount = 5.0, false),
                RequestAction(
                    playerId = 3,
                    actionOptions = listOf(
                        ActionOption.Fold,
                        ActionOption.Check,
                        ActionOption.Raise(minAmount = 10.0, maxAmount = 990.0)
                    ),
                    expiry = now.plusSeconds(10)
                ),
                Check(playerId = 3),
            ), table.rounds[0].actions
        )

        // Flop
        table = table.processPlayerAction(
            playerId = 2,
            action = PlayerActionRequest.Check(playerId = 2),
            now = now
        )
            .processTable(now, seedGenerator)

        table = table.processPlayerAction(
            playerId = 3,
            action = PlayerActionRequest.Bet(playerId = 3, amount = 30.0),
            now = now
        )
            .processTable(now, seedGenerator)

        table = table.processPlayerAction(
            playerId = 1,
            action = PlayerActionRequest.Fold(playerId = 1),
            now = now
        )
            .processTable(now, seedGenerator)

        table = table.processPlayerAction(
            playerId = 2,
            action = PlayerActionRequest.Fold(playerId = 2),
            now = now
        )
            .processTable(now, seedGenerator)


        assertEquals(
            listOf(
                RoundStarted(id = 1, street = Table.Round.Street.Flop),
                DealCommunityCards(
                    listOf(
                        Table.Card(suit = Suit.Diamonds, rank = 8),
                        Table.Card(suit = Suit.Spades, rank = 4),
                        Table.Card(suit = Suit.Clubs, rank = 11)
                    )
                ),
                RequestAction(
                    playerId = 2,
                    actionOptions = listOf(
                        ActionOption.Fold,
                        ActionOption.Check,
                        ActionOption.Bet(minAmount = 10.0, maxAmount = 990.0)
                    ),
                    expiry = now.plusSeconds(10)
                ),
                Check(playerId = 2),
                RequestAction(
                    playerId = 3,
                    actionOptions = listOf(
                        ActionOption.Fold,
                        ActionOption.Check,
                        ActionOption.Bet(minAmount = 10.0, maxAmount = 990.0)
                    ),
                    expiry = now.plusSeconds(10)
                ),
                Bet(playerId = 3, amount = 30.0, isAllIn = false),
                RequestAction(
                    playerId = 1,
                    actionOptions = listOf(
                        ActionOption.Fold,
                        ActionOption.Call(30.0),
                        ActionOption.Raise(minAmount = 40.0, maxAmount = 990.0)
                    ),
                    expiry = now.plusSeconds(10)
                ),
                Fold(playerId = 1),
                RequestAction(
                    playerId = 2,
                    actionOptions = listOf(
                        ActionOption.Fold,
                        ActionOption.Call(30.0),
                        ActionOption.Raise(minAmount = 40.0, maxAmount = 990.0)
                    ),
                    expiry = now.plusSeconds(10)
                ),
                Fold(playerId = 2),
                HandEnded(playerStacks = listOf(PlayerStack(3, 1020.0), PlayerStack(1, 990.0), PlayerStack(2, 990.0)))
            ), table.rounds[1].actions
        )
        assertEquals(true, table.isFinished)
        assertEquals(
            listOf(
                Pot(
                    number = 0,
                    amount = 60.0,
                    jackpot = 0.0,
                    playerWins = listOf(Pot.PlayerWin(playerId = 3, winAmount = 60.0))
                )
            ), table.pots
        )

        val nextHand = table.startNextHand(now = now)
        assertEquals(990.0, nextHand.players[0].initialStack)
        assertEquals(990.0, nextHand.players[1].initialStack)
        assertEquals(1020.0, nextHand.players[2].initialStack)
    }

    val seedGenerator = { 1L }
    val now = wellKnownTimestamp

    @Test
    fun `pre-flop, small blind cannot folds`() {
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

        // Pre-flop
        assertThrows<IllegalStateException> {
            table = table.processPlayerAction(
                playerId = 2,
                action = PlayerActionRequest.Fold(playerId = 2),
                now = now
            )
                .processTable(now, seedGenerator)
        }
    }

    @Test
    fun `pre-flop, dealer all in, fold, call`() {
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

        assertEquals(0, table.players[0].seat)
        assertEquals(1, table.players[0].playerId)

        assertEquals(1, table.players[1].seat)
        assertEquals(2, table.players[1].playerId)

        assertEquals(2, table.players[2].seat)
        assertEquals(3, table.players[2].playerId)


        // Small Blind

        var actionIndex = 8
        assertEquals(
            PostSmallBlind(
                playerId = 2,
                amount = 5.0,
                isAllIn = false,
            ),
            table.rounds[0].actions[actionIndex++],
        )

        // Big Blind
        assertEquals(
            PostBigBlind(
                playerId = 3,
                amount = 10.0,
                isAllIn = false,
            ),
            table.rounds[0].actions[actionIndex++],
        )

        // Private Cards
        table = table.processTable(now, seedGenerator)

        assertEquals(
            DealCards(
                playerId = 2,
                cards = listOf("10s".c(), "12c".c()),
            ),
            table.rounds[0].actions[actionIndex++],
        )

        assertEquals(
            DealCards(
                playerId = 3,
                cards = listOf("11d".c(), "1s".c()),
            ),
            table.rounds[0].actions[actionIndex++],
        )

        assertEquals(
            DealCards(
                playerId = 1,
                cards = listOf("1h".c(), "2d".c()),
            ),
            table.rounds[0].actions[actionIndex++],
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
            table.rounds[0].actions[actionIndex++],
        )

        table = table.processPlayerAction(
            playerId = 1,
            action = PlayerActionRequest.Raise(playerId = 1, amount = 1000.0),
            now = now
        )

        assertEquals(
            Raise(
                playerId = 1,
                amount = 1000.0,
                isAllIn = true,
            ),
            table.rounds[0].actions[actionIndex++],
        )

        // Small Blind calls BB amount
        table = table.processTable(now, seedGenerator)
        assertEquals(
            RequestAction(
                playerId = 2,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Call(amount = 995.0),
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[0].actions[actionIndex++],
        )

        table = table.processPlayerAction(
            playerId = 2,
            action = PlayerActionRequest.Fold(playerId = 2),
            now = now
        )

        assertEquals(
            Fold(playerId = 2),
            table.rounds[0].actions[actionIndex++],
        )

        // Big blind checks
        table = table.processTable(now, seedGenerator)
        assertEquals(
            RequestAction(
                playerId = 3,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Call(amount = 990.0),
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[0].actions[actionIndex++],
        )

        table = table.processPlayerAction(
            playerId = 3,
            action = PlayerActionRequest.Call(playerId = 3, amount = 990.0),
            now = now
        )
        assertEquals(Call(playerId = 3, amount = 990.0, isAllIn = true), table.rounds[0].actions[actionIndex++])

        table = table.processTable(now, seedGenerator)

        assertEquals(
            listOf(
                RoundStarted(1, Table.Round.Street.Flop),
                DealCommunityCards(cards = listOf("8d".c(), "4s".c(), "11c".c()))
            ), table.rounds[1].actions
        )

        assertEquals(
            listOf(
                RoundStarted(2, Table.Round.Street.Turn),
                DealCommunityCards(cards = listOf("3d".c()))
            ), table.rounds[2].actions
        )

        assertEquals(
            listOf(
                RoundStarted(3, Table.Round.Street.River),
                DealCommunityCards(cards = listOf("6s".c()))
            ), table.rounds[3].actions
        )

        assertEquals(
            listOf(
                RoundStarted(4, Table.Round.Street.Showdown),
                ShowCards(playerId = 1, cards = listOf("1h".c(), "2d".c())),
                ShowCards(playerId = 3, cards = listOf("11d".c(), "1s".c())),
                StandUp(playerId = 1, stack = 0.0),
                HandEnded(
                    playerStacks = listOf(
                        PlayerStack(1, 0.0),
                        PlayerStack(2, 995.0),
                        PlayerStack(3, 2005.0),
                    )
                )
            ), table.rounds[4].actions
        )

        val nextHand = table.processTable(now.plusSeconds(10))
        println(nextHand)
    }

    @Test
    fun `given small blind player sits out mid-hand, then the game continues and their blind remains in pot`() {
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

        assertEquals(wellKnownPreFlopActions, table.rounds.last().actions)
        table = table.processPlayerAction(2, PlayerActionRequest.StandUp(2), now)

        assertEquals(StandUp(2, 995.0), table.rounds.last().actions.last())
    }

    @Test
    fun `heads up game starts correctly`() {
        val seedGenerator = { 1L }
        var table = givenWellKnownTournamentTable {
            withSeed(1)
            withDealerSeat(0)
            withDefaultPlayers(2)
            withBlinds(
                smallBlind = 5.0,
                bigBlind = 10.0,
            )
        }

        var now = wellKnownTimestamp
        table = table.processTable(now, seedGenerator)
        table = table.processTable(now, seedGenerator)

        // Player Order

        assertEquals(0, table.players[0].seat)
        assertEquals(1, table.players[0].playerId)

        assertEquals(1, table.players[1].seat)
        assertEquals(2, table.players[1].playerId)


        // Small Blind

        var actionIndex = 6
        assertEquals(
            PostSmallBlind(
                playerId = 2,
                amount = 5.0,
                isAllIn = false,
            ),
            table.rounds[0].actions[actionIndex++],
        )

        // Big Blind
        assertEquals(
            PostBigBlind(
                playerId = 1,
                amount = 10.0,
                isAllIn = false,
            ),
            table.rounds[0].actions[actionIndex++],
        )

        // Private Cards
        table = table.processTable(now, seedGenerator)

        assertEquals(
            DealCards(
                playerId = 2,
                cards = listOf("10s".c(), "12c".c()),
            ),
            table.rounds[0].actions[actionIndex++],
        )

        assertEquals(
            DealCards(
                playerId = 1,
                cards = listOf("11d".c(), "1s".c()),
            ),
            table.rounds[0].actions[actionIndex++],
        )

        //
        assertEquals(
            RequestAction(
                playerId = 2,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Call(amount = 5.0),
                    ActionOption.Raise(minAmount = 20.0, maxAmount = 995.0)
                ),
                expiry = now.plusSeconds(10)
            ),
            table.rounds[0].actions[actionIndex++],
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
            table.rounds[0].actions[actionIndex++],
        )
    }

    val wellKnownPreFlopActions = listOf(
        SitDown(playerId = 1, playerName = "Player 1", seat = 0, stack = 1000.0),
        SitDown(playerId = 2, playerName = "Player 2", seat = 1, stack = 1000.0),
        SitDown(playerId = 3, playerName = "Player 3", seat = 2, stack = 1000.0),
        SitDown(playerId = 1, playerName = "Player 1", seat = 0, stack = 1000.0),
        SitDown(playerId = 2, playerName = "Player 2", seat = 1, stack = 1000.0),
        SitDown(playerId = 3, playerName = "Player 3", seat = 2, stack = 1000.0),
        HandStarted,
        RoundStarted(id = 0, street = Table.Round.Street.PreFlop),
        PostSmallBlind(playerId = 2, amount = 5.0, false),
        PostBigBlind(playerId = 3, amount = 10.0, false),
        DealCards(
            playerId = 2,
            cards = listOf("10s".c(), "12c".c()),
        ),
        DealCards(
            playerId = 3,
            cards = listOf("11d".c(), "1s".c()),
        ),

        DealCards(
            playerId = 1,
            cards = listOf("1h".c(), "2d".c()),
        ),
        RequestAction(
            playerId = 1,
            actionOptions = listOf(
                ActionOption.Fold,
                ActionOption.Call(amount = 10.0),
                ActionOption.Raise(minAmount = 20.0, maxAmount = 1000.0)
            ),
            expiry = now.plusSeconds(10)
        ),
    )


    // TODO: add a test with early all ins
    // TODO: add a test with 2 winners
    // TODO: add a test with split pot (all in with smaller stack and wins)

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
