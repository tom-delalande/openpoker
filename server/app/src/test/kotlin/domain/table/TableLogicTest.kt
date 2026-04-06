package domain.table

import domain.model.Table
import domain.model.Table.Card.Suit
import kotlin.test.assertEquals
import domain.model.Table.Round.Action.PlayerAction.RequestAction.ActionOption as ActionOption
import domain.model.Table.Round.Action.PlayerAction.*
import org.junit.jupiter.api.Test

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
                            Table.Card(Table.Card.Suit.Diamonds, 11),
                            Table.Card(Table.Card.Suit.Spades, 1),
                        )
                    ),
                    DealCards(
                        3,
                        listOf(
                            Table.Card(Table.Card.Suit.Hearts, 1),
                            Table.Card(Table.Card.Suit.Diamonds, 2),
                        )
                    ),
                    DealCards(
                        1,
                        listOf(
                            Table.Card(Table.Card.Suit.Spades, 10),
                            Table.Card(Table.Card.Suit.Clubs, 12),
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
            PostSmallBlind(2, DEFAULT_SMALL_BLIND_AMOUNT, false),
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

    @Test
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
            PostBigBlind(3, DEFAULT_BIG_BLIND_AMOUNT, false),
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
                    ActionOption.Check,
                    ActionOption.Bet(minAmount = DEFAULT_BIG_BLIND_AMOUNT, maxAmount = 1000.0),
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
                    actionOptions = listOf(ActionOption.Check),
                    expiry = wellKnownTimestamp.plusSeconds(DEFAULT_TIMEOUT_IN_SECONDS)
                )
            )
        }

        val table = initialTable.processPlayerAction(
            playerId = 1,
            Check(1),
            wellKnownTimestamp,
        )

        assertEquals(8, table.rounds[0].actions.size)
        assertEquals(
            Check(playerId = 1),
            table.rounds[0].actions[6],
        )
        assertEquals(
            RequestAction(
                playerId = 2,
                actionOptions = listOf(
                    ActionOption.Fold,
                    ActionOption.Check,
                    ActionOption.Bet(minAmount = DEFAULT_BIG_BLIND_AMOUNT, maxAmount = 1000.0),
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
            Check(3),
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
                    ActionOption.Bet(minAmount = DEFAULT_BIG_BLIND_AMOUNT, maxAmount = 1000.0)
                ),
                expiry = wellKnownTimestamp.plusSeconds(10),
            ),
            table.rounds[1].actions[1],
        )
    }
}
