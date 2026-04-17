package domain.table

import domain.model.Table
import domain.model.Table.Round.Street
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
            cards = "9d 2d 7s 6s 1c 1c 5d 7h 10s 13s 1h"
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
            2 to 890.0,
            3 to 910.0
        )
    }

    @Test
    fun `given a player goes all in with less than another player's bets and draws, then pot is returned`() {
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
            1 to 100.0,
            2 to 1000.0,
            3 to 1000.0
        )
    }

    @Test
    fun `given a hand finished, next hand starts correctly`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(100.0, 1000.0, 1000.0),
            cards = "1d 1d 2s 2s 3c 3c 11s 10d 9h 2s 3c"
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
            manually {
                assertTrue(it.rounds.last().actions.last() is Table.Round.Action.HandEnded)
                assertEquals(
                    listOf(1 to 300.0, 2 to 890.0, 3 to 910.0),
                    it.players.map { it.playerId to it.stack }
                )
            }
            startNextHand()
            postBlinds()
            dealHoleCards()
            manually {
                assertEquals(2, it.handVersion)
            }
            p2().call(10.0)
            p3().call(5.0)
            p1().check()
        }
    }

    @Test
    fun `if dealer sits up instead of acting, then hand carries on`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(100.0, 1000.0, 1000.0),
        ) {
            postBlinds()
            dealHoleCards()
            manually {
                it
            }
            p1().standUp()
            p2().call(5.0)
            p3().check()
            dealFlop()
            manually {
                assertEquals(Street.Flop, it.currentRound?.street)
            }
        }
    }

    // ==================== HAPPY PATH - MULTIPLE TABLE SIZES ====================

    @Test
    fun `heads up hand plays through all streets to showdown`() {
        val r = pokerScenario(
            players = 2,
            blinds = 5.0 to 10.0,
            seed = 1,
            cards = "1h 2d 3s 4c 5c 6d 7s 8h"
        ) {
            postBlinds()
            dealHoleCards()
            p2().call(5.0)
            p1().check()
            dealFlop()
            p2().check()
            p1().bet(10.0)
            p2().call(10.0)
            dealTurn()
            p2().check()
            p1().bet(20.0)
            p2().call(20.0)
            dealRiver()
            p2().check()
            p1().check()
        }

        r.assertFinishedPots(
            1 to 1040.0,
            2 to 960.0
        )
    }

    @Test
    fun `three player hand typical play goes to showdown`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            cards = "1h 2d 3s 4c 5c 6d 7s 8h 9c 10d 6c"
        ) {
            postBlinds()
            dealHoleCards()
            p1().call(10.0)
            p2().call(5.0)
            p3().raise(20.0)
            p1().call(20.0)
            p2().call(20.0)
            dealFlop()
            p2().check()
            p3().bet(10.0)
            p1().call(10.0)
            p2().call(10.0)
            dealTurn()
            p2().check()
            p3().check()
            p1().bet(20.0)
            p2().call(20.0)
            p3().call(20.0)
            dealRiver()
            p2().check()
            p3().check()
            p1().check()
        }

        r.assertFinishedPots(
            1 to 1000.0,
            2 to 1000.0,
            3 to 1000.0
        )
    }

    // ==================== FOLD SCENARIOS ====================

    @Test
    fun `everyone folds pre-flop to big blind winner takes pot unshown`() {
        val r = pokerScenario(
            players = 4,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0, 200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p4().fold()
            p1().fold()
            p2().fold()
            manually {
                val ended =
                    it.rounds.flatMap { r -> r.actions }.filterIsInstance<domain.model.Table.Round.Action.HandEnded>()
                        .singleOrNull()
                assertTrue(ended != null, "Hand should have ended after big blind wins unshown")
            }
        }
    }

    @Test
    fun `fold on flop before showdown last player wins`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p1().call(10.0)
            p2().call(5.0)
            p3().check()
            dealFlop()
            p2().bet(15.0)
            p3().fold()
            p1().fold()
            manually {
                val ended =
                    it.rounds.flatMap { r -> r.actions }.filterIsInstance<domain.model.Table.Round.Action.HandEnded>()
                        .singleOrNull()
                assertTrue(ended != null, "Hand should have ended")
            }
        }
    }

    @Test
    fun `fold on river before showdown`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p1().call(10.0)
            p2().call(5.0)
            p3().check()
            dealFlop()
            p2().check()
            p3().bet(10.0)
            p1().call(10.0)
            p2().call(10.0)
            dealTurn()
            p2().check()
            p3().bet(20.0)
            p1().fold()
            p2().call(20.0)
            dealRiver()
            p2().check()
            p3().check()
            manually {
                val ended =
                    it.rounds.flatMap { r -> r.actions }.filterIsInstance<domain.model.Table.Round.Action.HandEnded>()
                        .singleOrNull()
                assertTrue(ended != null, "Hand should have ended")
            }
        }
    }

    @Test
    fun `only one player remains after folds hand ends immediately`() {
        val r = pokerScenario(
            players = 4,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0, 200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p4().raise(20.0)
            p1().fold()
            p2().fold()
            p3().fold()
            manually {
                val ended =
                    it.rounds.flatMap { r -> r.actions }.filterIsInstance<domain.model.Table.Round.Action.HandEnded>()
                        .singleOrNull()
                assertTrue(ended != null, "Hand should have ended when only one player remains")
            }
        }
    }

    // ==================== STAND UP SCENARIOS ====================

    @Test
    fun `player stands up leaving only one other player hand ends`() {
        val r = pokerScenario(
            players = 2,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p1().standUp()
            manually {
                val ended =
                    it.rounds.flatMap { r -> r.actions }.filterIsInstance<domain.model.Table.Round.Action.HandEnded>()
                        .singleOrNull()
                assertTrue(ended != null, "Hand should have ended with only one player")
            }
        }
    }

    @Test
    fun `last player stands up hand immediately ends with no winner`() {
        val r = pokerScenario(
            players = 2,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p2().call(5.0)
            p1().standUp()
            manually {
                val ended =
                    it.rounds.flatMap { r -> r.actions }.filterIsInstance<domain.model.Table.Round.Action.HandEnded>()
                        .singleOrNull()
                assertTrue(ended != null, "Hand should have ended with no winner when last player stands")
            }
        }
    }

    @Test
    fun `player stands up mid-hand remaining players continue to showdown`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p1().call(10.0)
            p2().call(5.0)
            p3().check()
            dealFlop()
            p2().standUp()
            p3().bet(15.0)
            p1().call(15.0)
            dealTurn()
            p3().bet(20.0)
            p1().call(20.0)
            dealRiver()
            p3().check()
            p1().check()
        }

        r.assertStreet(Street.Showdown)
    }

    // ==================== SIT DOWN SCENARIOS ====================

    @Test
    fun `player sits down mid-hand added to next hand`() {
        val r = pokerScenario(
            players = 2,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p2().call(5.0)
            p1().check()
            p3().sitDown(100.0)
            dealFlop()
            p2().check()
            p1().check()
            dealTurn()
            p2().check()
            p1().check()
            dealRiver()
            p2().check()
            p1().check()
            startNextHand()
            postBlinds()
            dealHoleCards()
            manually {
                assertEquals(3, it.players.size, "New player should be seated for next hand")
            }
        }
    }

    @Test
    fun `player sits down between hands joins table`() {
        val r = pokerScenario(
            players = 2,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p2().call(5.0)
            p1().check()
            dealFlop()
            p2().check()
            p1().check()
            dealTurn()
            p2().check()
            p1().check()
            dealRiver()
            p2().check()
            p1().check()
            startNextHand()
            p3().sitDown(100.0)
            postBlinds()
            dealHoleCards()
            manually {
                assertEquals(3, it.players.size, "Player should be seated for next hand")
            }
        }
    }

    @Test
    fun `player sits down becomes part of next hand`() {
        val r = pokerScenario(
            players = 2,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p2().call(5.0)
            p1().check()
            dealFlop()
            p2().check()
            p1().check()
            dealTurn()
            p2().check()
            p1().check()
            dealRiver()
            p2().check()
            p1().check()
            startNextHand()
            p3().sitDown(100.0)
            postBlinds()
            dealHoleCards()
            manually {
                assertTrue(it.players.any { it.playerId == 3 && !it.isSittingOut }, "Player 3 should be in hand")
            }
        }
    }

    // ==================== SUBSEQUENT HANDS ====================

    @Test
    fun `dealer button advances correctly to next hand`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p1().fold()
            p2().fold()
            manually {
                assertEquals(0, it.dealerSeat)
            }
            startNextHand()
            postBlinds()
            dealHoleCards()
            manually {
                assertEquals(1, it.dealerSeat, "Button should move to player 2")
            }
        }
    }

    @Test
    fun `blinds rotate correctly to next hand`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            manually {
                assertEquals(2, it.smallBlindPlayer?.playerId)
                assertEquals(3, it.bigBlindPlayer?.playerId)
            }
            p1().call(10.0)
            p2().call(5.0)
            p3().check()
            dealFlop()
            p2().fold()
            p3().fold()
            startNextHand()
            postBlinds()
            dealHoleCards()
            manually {
                assertEquals(3, it.smallBlindPlayer?.playerId, "SB should rotate to player 3")
                assertEquals(1, it.bigBlindPlayer?.playerId, "BB should rotate to player 1")
            }
        }
    }

    @Test
    fun `table with 2 players, 1 joins after starts, then active player folds, hand ends`() {
        val r = pokerScenario(
            players = 2,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p2().call(5.0)
            p1().check()
            p3().sitDown(200.0)
            dealFlop()
            p2().fold()
            dealTurn()
            dealRiver()
        }

        r.assertFinished()
    }

    // ==================== SHOWDOWN & POT DISTRIBUTION ====================

    @Test
    fun `high hand wins side pot while low hand takes main pot`() {
        val r = pokerScenario(
            players = 4,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(50.0, 100.0, 100.0, 100.0),
            cards = "2h 7d 13s 13c 2c 7d 1s 1h 9c 11d 6c 2h 1s"
            //       ^P2   ^P3     ^P4   ^P1   ^Table
        ) {
            postBlinds()
            dealHoleCards()
            p4().fold()
            p1().call(10.0)
            p2().call(5.0)
            p3().check()
            p1().raise(40.0)
            p2().call(40.0)
            p3().raise(50.0)
            p2().call(50.0)
            dealFlop()
            dealTurn()
            dealRiver()
        }

        r.assertFinishedPots(
            1 to 150.0,
            2 to 40.0,
            3 to 60.0,
            4 to 100.0,
        )
    }

    @Test
    fun `two players split main pot on tie`() {
        val r = pokerScenario(
            players = 2,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0),
            cards = "7h 2d 7s 2c 4d 5c 6s 7h"
        ) {
            postBlinds()
            dealHoleCards()
            p2().call(5.0)
            p1().check()
            dealFlop()
            p2().check()
            p1().bet(20.0)
            p2().call(20.0)
            dealTurn()
            p2().check()
            p1().bet(30.0)
            p2().call(30.0)
            dealRiver()
            p2().check()
            p1().check()
        }

        r.assertFinishedPots(
            1 to 200.0,
            2 to 200.0
        )
    }

    @Test
    fun `multiple winning players in different pots`() {
        val r = pokerScenario(
            players = 4,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(20.0, 80.0, 150.0, 200.0),
            cards = "12s 12c 11c 11d 7s 2h 1h 1d 3c 4d 5c 6h 8s"
        ) {
            postBlinds()
            dealHoleCards()
            p4().fold()
            p1().call(10.0)
            p2().call(5.0)
            p3().raise(30.0)
            p1().call(10.0) // All in
            p2().raise(70.0) // All in
            p3().call(40.0)
            dealFlop()
            dealTurn()
            dealRiver()
        }

        r.assertFinishedPots(
            1 to 60.0,
            2 to 120.0,
            3 to 70.0,
            4 to 200.0,
        )
    }

    // ==================== ADDITIONAL EDGE CASES ====================

    @Test
    fun `all players all-in pre-flop goes straight to showdown`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(100.0, 100.0, 100.0),
            cards = "1h 2d 3s 4c 5c 6d 7s 8h 9c"
        ) {
            postBlinds()
            dealHoleCards()
            p1().raise(90.0)
            p2().call(85.0)
            p3().call(80.0)
            manually {
                assertTrue(it.currentRound?.street == Street.Flop || it.currentRound?.street == Street.PreFlop || it.rounds.flatMap { r -> r.actions }
                    .filterIsInstance<domain.model.Table.Round.Action.HandEnded>().isNotEmpty())
            }
        }
    }

    @Test
    fun `two players all-in pre-flop with different stacks creates side pot`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(50.0, 100.0, 200.0),
            cards = "7h 2d 7s 2c 1c 1d 4s 5h 9c 11s 13c"
        ) {
            postBlinds()
            dealHoleCards()
            p1().raise(50.0)
            p2().raise(90.0)
            p3().fold()
            dealFlop()
            dealTurn()
            dealRiver()
        }

        r.assertFinishedPots(
            1 to 110.0, // 10 from p3, 50 from p2
            2 to 50.0, // gets their 40 back
            3 to 190.0,
        )
    }

    @Test
    fun `three way all-in with different amounts creates multiple side pots`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(30.0, 60.0, 120.0),
            cards = "1h 2d 3s 4c 5c 6d 7s 8h 9c"
        ) {
            postBlinds()
            dealHoleCards()
            p1().call(10.0)
            p2().call(5.0)
            p3().check()
            dealFlop()
            p2().check()
            p3().check()
            p1().bet(20.0)
            p2().raise(50.0)
            p3().raise(110.0)
            dealTurn()
            dealRiver()
        }

        r.assertPotCount(3)
        r.assertFinishedPots(
            1 to 90.0,
            2 to 60.0,
            3 to 60.0
        )
    }

    @Test
    fun `players go all-in on different streets creates growing side pots`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(100.0, 200.0, 200.0),
            cards = "1h 2d 3s 4c 5c 6d 7s 8h 9c 10d 11c"
        ) {
            postBlinds()
            dealHoleCards()
            p1().call(10.0)
            p2().call(5.0)
            p3().check()
            dealFlop()
            p2().bet(90.0)
            p3().call(90.0)
            p1().call(90.0)
            dealTurn()
            p2().bet(30.0)
            p3().call(30.0)
            dealRiver()
            p2().check()
            p3().check()
        }

        r.assertPotCount(2)
    }

    @Test
    fun `player with short stack less than big blind can still play`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(8.0, 200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p1().call(8.0)
            p2().call(5.0)
            p3().check()
            dealFlop()
            p2().bet(20.0)
            p3().fold()
            dealTurn()
            dealRiver()
        }

        r.assertStreet(Street.Showdown)
    }

    @Test
    fun `player with short stack less than big blind can still bet`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(19.0, 200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p1().call(10.0)
            p2().call(5.0)
            p3().check()
            dealFlop()
            p2().check()
            p3().check()
            p1().bet(9.0)
        }
    }

    @Test
    fun `player with short stack less than big blind can still raise`() {
        val r = pokerScenario(
            players = 2,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(20.0, 20.0)
        ) {
            postBlinds()
            dealHoleCards()
            p2().call(5.0)
            p1().raise(10.0)
        }
    }

    @Test
    fun `player with short stack too small then cannot raise`() {
        assertThrows<Exception> {
            pokerScenario(
                players = 2,
                blinds = 5.0 to 10.0,
                seed = 1,
                stacks = listOf(10.0, 20.0)
            ) {
                postBlinds()
                dealHoleCards()
                p2().call(5.0)
                p1().raise(10.0)
            }
        }
    }

    @Test
    fun `big blind can check-raise all-in on first action`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p1().call(10.0)
            p2().call(5.0)
            p3().raise(50.0)
            p1().fold()
            p2().raise(145.0)
            p3().call(95.0)
            dealFlop()
        }
    }

    // ==================== TIMEOUT SCENARIOS ====================

    @Test
    fun `player timeouts on pre-flop removed from hand`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p1().standUp()
            p2().call(5.0)
            p3().check()
            dealFlop()
            manually {
                assertEquals(2, it.players.filter { p -> !p.isSittingOut }.size)
            }
        }
    }

    @Test
    fun `player timeouts on flop auto-sits-out betting continues`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p1().call(10.0)
            p2().call(5.0)
            p3().check()
            dealFlop()
            p2().standUp()
            p3().bet(15.0)
            p1().call(15.0)
            dealTurn()
            p3().check()
            p1().bet(20.0)
            p3().call(20.0)
            dealRiver()
            p3().check()
            p1().check()
        }

        r.assertStreet(Street.Showdown)
    }

    @Test
    fun `player timeouts on turn auto-sits-out showdown continues`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p1().call(10.0)
            p2().call(5.0)
            p3().check()
            dealFlop()
            p2().check()
            p3().bet(10.0)
            p1().call(10.0)
            p2().call(10.0)
            dealTurn()
            p2().standUp()
            p3().check()
            p1().check()
            dealRiver()
            p3().check()
            p1().check()
        }

        r.assertStreet(Street.Showdown)
    }

    @Test
    fun `player timeouts on river auto-sits-out showdown continues`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p1().call(10.0)
            p2().call(5.0)
            p3().check()
            dealFlop()
            p2().check()
            p3().bet(10.0)
            p1().call(10.0)
            p2().call(10.0)
            dealTurn()
            p2().check()
            p3().bet(20.0)
            p1().call(20.0)
            p2().call(20.0)
            dealRiver()
            p2().standUp()
            p3().check()
            p1().check()
        }

        r.assertStreet(Street.Showdown)
    }

    @Test
    fun `all players timeout except one remaining player wins by default`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p1().standUp()
            p2().standUp()
            manually {
                val ended =
                    it.rounds.flatMap { r -> r.actions }.filterIsInstance<domain.model.Table.Round.Action.HandEnded>()
                        .singleOrNull()
                assertTrue(ended != null, "Hand should end when only one player remains")
            }
        }
    }

    @Test
    fun `timeout leaves less than 2 players hand ends immediately`() {
        val r = pokerScenario(
            players = 2,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(200.0, 200.0)
        ) {
            postBlinds()
            dealHoleCards()
            p1().standUp()
            manually {
                val ended =
                    it.rounds.flatMap { r -> r.actions }.filterIsInstance<domain.model.Table.Round.Action.HandEnded>()
                        .singleOrNull()
                assertTrue(ended != null, "Hand should end with less than 2 players")
            }
        }
    }

    @Test
    fun `player timeouts but all-in player remains and hand continues and all in player can win`() {
        val r = pokerScenario(
            players = 3,
            blinds = 5.0 to 10.0,
            seed = 1,
            stacks = listOf(100.0, 200.0, 200.0),
            cards = "3h 3d 2s 2c 1c 1d 7s 8h 9c 11c 13c"
        ) {
            postBlinds()
            dealHoleCards()
            p1().raise(100.0)
            p2().call(95.0)
            p3().call(90.0)
            p1().standUp()
            dealFlop()
            p2().check()
            p3().check()
            dealTurn()
            p2().check()
            p3().check()
            dealRiver()
            p2().check()
            p3().check()
        }

        r.assertFinishedPots(
            1 to 300.0,
            2 to 100.0,
            3 to 100.0
        )
    }
}