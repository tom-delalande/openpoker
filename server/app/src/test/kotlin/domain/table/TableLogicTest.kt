package domain.table

import domain.model.Table
import kotlin.test.assertEquals
import domain.model.Table.Round.Action.PlayerAction.RequestAction.ActionOption as ActionOption
import domain.model.Table.Round.Action.PlayerAction.*
import org.junit.jupiter.api.Test

class TableLogicTest {
    @Test
    fun `given tournament with set seed and 3 players, when dealing initial cards, then 6 cards are dealt`() {
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
                cards = listOf(),
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
    fun `given tournament with dealt cards, when processing table, then action requested with small blind or fold`() {
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
    fun `given tournament with dealt cards and small blind action requested, when processing table after timeout, then small blind folds and big blind requested`() {
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
    fun `given tournament with dealt cards and small blind action requested, when player posts small blind, then small blind posted and big blind requested`() {
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
}