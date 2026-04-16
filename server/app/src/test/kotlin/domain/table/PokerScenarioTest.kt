package domain.table

import domain.model.Table
import domain.model.Table.Round
import domain.model.Table.Round.Street
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class PokerScenarioTest {

    @Test
    fun `limps go to flop`() {
        val r = pokerScenario(3, 5.0 to 10.0, 1) {
            postBlinds()
            dealHoleCards()
            p1().call(10.0)
            p2().call(5.0)
            p3().check()
            dealFlop()
        }

        assertEquals(Street.Flop, r.table.currentRound?.street)
    }

    @Test
    fun `verify hole cards dealt correctly`() {
        val r = pokerScenario(3, 5.0 to 10.0, 1) {
            postBlinds()
            dealHoleCards()
        }

        r.assertPlayerCards(2, "10s", "12c")
        r.assertPlayerCards(3, "11d", "1s")
        r.assertPlayerCards(1, "1h", "2d")
    }

    @Test
    fun `verify community cards after flop`() {
        val r = pokerScenario(3, 5.0 to 10.0, 1) {
            postBlinds()
            dealHoleCards()
            p1().call(10.0)
            p2().call(5.0)
            p3().check()
            dealFlop()
        }

        r.assertCommunityCards("8d", "4s", "11c")
    }

    @Test
    fun `complete hand scenario - player bets and others fold`() {
        val r = pokerScenario(3, 5.0 to 10.0, 1) {
            postBlinds()
            dealHoleCards()
            p1().call(10.0)
            p2().call(5.0)
            p3().check()
            dealFlop()
            p2().bet(20.0)
            p3().fold()
        }

        assertTrue(r.table.isStarted, "Table should be started")
    }

    @Test
    fun `complete hand scenario - all four rounds`() {
        val r = pokerScenario(3, 5.0 to 10.0, 1) {
            postBlinds()
            dealHoleCards()
            p1().call(10.0)
            p2().call(5.0)
            p3().check()
            dealFlop()
            p2().check()
            p3().bet(10.0)
            p1().raise(20.0)
            p2().call(20.0)
            p3().call(10.0)
            dealTurn()
            p2().check()
            p3().bet(10.0)
            p1().fold()
            p2().call(10.0)
            dealRiver()
            p2().check()
            p3().check()
        }

        r.assertFinishedPots(
            1 to 970.0,
            2 to 960.0,
            3 to 1070.0
        )
    }

    @Test
    fun `given a player goes all in with less than another player's bets and wins, then pot is split`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(100.0, 1000.0, 1000.0),
            cards = "9d 2d 7s 6s 1c 12c 11c 10c 9c 2c 3c"
        ) {
            postBlinds()
            dealHoleCards()
            p1().call(10.0)
            p2().call(5.0)
            p3().check()
            dealFlop()
            p2().check()
            p3().bet(10.0)
            p1().raise(90.0)
            p2().call(90.0)
            p3().call(80.0)
            dealTurn()
            manually {
                assertEquals(Street.Turn, it.currentRound?.street)
            }
            p2().check()
            p3().bet(10.0)
            p2().call(10.0)
            dealRiver()
            manually {
                assertEquals(Street.River, it.currentRound?.street)
            }
            p2().check()
            p3().check()
        }

        r.assertFinishedPots(
            1 to 300.0,
            2 to 910.0,
            3 to 890.0
        )
    }
}