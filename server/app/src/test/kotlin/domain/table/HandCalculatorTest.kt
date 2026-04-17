package domain.table

import domain.model.Table.Card
import domain.model.Table.Card.Suit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class HandCalculatorTest {

    @Test
    fun `high card is correctly identified`() {
        val cards = listOf(c("1h"), c("2d"), c("3s"), c("4c"), c("5d"))
        val rating = cards.rateHand()
        assertEquals(HandStrength.HighCard, rating.handStrength)
    }

    @Test
    fun `pair is correctly identified`() {
        val cards = listOf(c("1h"), c("1d"), c("2s"), c("3c"), c("4d"))
        val rating = cards.rateHand()
        assertEquals(HandStrength.Pair, rating.handStrength)
    }

    @Test
    fun `pair kings is correctly identified`() {
        val cards = listOf(c("13h"), c("13d"), c("2s"), c("3c"), c("4d"))
        val rating = cards.rateHand()
        assertEquals(HandStrength.Pair, rating.handStrength)
    }

    @Test
    fun `two pair is correctly identified`() {
        val cards = listOf(c("1h"), c("1d"), c("2s"), c("2c"), c("3d"))
        val rating = cards.rateHand()
        assertEquals(HandStrength.TwoPair, rating.handStrength)
    }

    @Test
    fun `three of a kind is correctly identified`() {
        val cards = listOf(c("1h"), c("1d"), c("1s"), c("2c"), c("3d"))
        val rating = cards.rateHand()
        assertEquals(HandStrength.ThreeOfAKind, rating.handStrength)
    }

    @Test
    fun `full house is correctly identified`() {
        val cards = listOf(c("1h"), c("1d"), c("1s"), c("2c"), c("2d"))
        val rating = cards.rateHand()
        assertEquals(HandStrength.FullHouse, rating.handStrength)
    }

    @Test
    fun `four of a kind is correctly identified`() {
        val cards = listOf(c("1h"), c("1d"), c("1s"), c("1c"), c("2d"))
        val rating = cards.rateHand()
        assertEquals(HandStrength.FourOfAKind, rating.handStrength)
    }

    @Test
    fun `straight is correctly identified`() {
        val cards = listOf(c("1h"), c("2d"), c("3s"), c("4c"), c("5d"))
        val rating = cards.rateHand()
        assertEquals(HandStrength.Straight, rating.handStrength)
    }

    @Test
    fun `straight to jack is correctly identified`() {
        val cards = listOf(c("2h"), c("3d"), c("4s"), c("5c"), c("6d"))
        val rating = cards.rateHand()
        assertEquals(HandStrength.Straight, rating.handStrength)
    }

    @Test
    fun `wheel straight with ace high is correctly identified`() {
        val cards = listOf(c("10h"), c("11d"), c("12s"), c("13c"), c("1d"))
        val rating = cards.rateHand()
        assertEquals(HandStrength.Straight, rating.handStrength)
    }

    @Test
    fun `flush is correctly identified`() {
        val cards = listOf(c("1h"), c("2h"), c("3h"), c("4h"), c("5h"))
        val rating = cards.rateHand()
        assertEquals(HandStrength.Flush, rating.handStrength)
    }

    @Test
    fun `flush with high cards is correctly identified`() {
        val cards = listOf(c("1h"), c("3h"), c("5h"), c("9h"), c("13h"))
        val rating = cards.rateHand()
        assertEquals(HandStrength.Flush, rating.handStrength)
    }

    @Test
    fun `straight flush is correctly identified`() {
        val cards = listOf(c("1h"), c("2h"), c("3h"), c("4h"), c("5h"))
        val rating = cards.rateHand()
        assertEquals(HandStrength.StraightFlush, rating.handStrength)
    }

    @Test
    fun `royal flush is correctly identified as straight flush`() {
        val cards = listOf(c("10h"), c("11h"), c("12h"), c("13h"), c("1h"))
        val rating = cards.rateHand()
        assertEquals(HandStrength.StraightFlush, rating.handStrength)
    }

    @Test
    fun `7 cards - four of a kind is selected`() {
        val privateCards = listOf(c("1h"), c("1d"))
        val communityCards = listOf(c("1s"), c("1c"), c("2d"), c("3d"), c("4d"))
        val allCards = privateCards + communityCards
        val rating = allCards.rateHand()
        assertEquals(HandStrength.FourOfAKind, rating.handStrength)
    }

    @Test
    fun `7 cards - straight flush when possible`() {
        val privateCards = listOf(c("1h"), c("2h"))
        val communityCards = listOf(c("3h"), c("4h"), c("5h"), c("1d"), c("2d"))
        val allCards = privateCards + communityCards
        val rating = allCards.rateHand()
        assertEquals(HandStrength.StraightFlush, rating.handStrength)
    }

    @Test
    fun `7 cards - full house beats three of a kind`() {
        val privateCards = listOf(c("1h"), c("1d"))
        val communityCards = listOf(c("1s"), c("2c"), c("2d"), c("3d"), c("4d"))
        val allCards = privateCards + communityCards
        val rating = allCards.rateHand()
        assertEquals(HandStrength.FullHouse, rating.handStrength)
    }

    @Test
    fun `7 cards - flush beats straight`() {
        val privateCards = listOf(c("1h"), c("2d"))
        val communityCards = listOf(c("3h"), c("4h"), c("5h"), c("6h"), c("7d"))
        val allCards = privateCards + communityCards
        val rating = allCards.rateHand()
        assertEquals(HandStrength.Flush, rating.handStrength)
    }

    @Test
    fun `7 cards - straight beats pair when no flush`() {
        val privateCards = listOf(c("1h"), c("1d"))
        val communityCards = listOf(c("2s"), c("3c"), c("4s"), c("5c"), c("6s"))
        val allCards = privateCards + communityCards
        val rating = allCards.rateHand()
        assertEquals(HandStrength.Straight, rating.handStrength)
    }

    @Test
    fun `score increases with hand strength - high card to pair`() {
        val highCard = listOf(c("1h"), c("2d"), c("3s"), c("4c"), c("5d")).rateHand()
        val pair = listOf(c("1h"), c("1d"), c("2s"), c("3c"), c("4d")).rateHand()
        assertTrue(highCard.score < pair.score, "HighCard < Pair")
    }

    @Test
    fun `score increases with hand strength - pair to two pair`() {
        val pair = listOf(c("1h"), c("1d"), c("2s"), c("3c"), c("4d")).rateHand()
        val twoPair = listOf(c("1h"), c("1d"), c("2s"), c("2c"), c("3d")).rateHand()
        assertTrue(pair.score < twoPair.score, "Pair < TwoPair")
    }

    @Test
    fun `score increases with hand strength - two pair to three of a kind`() {
        val twoPair = listOf(c("1h"), c("1d"), c("2s"), c("2c"), c("3d")).rateHand()
        val threeOfAKind = listOf(c("1h"), c("1d"), c("1s"), c("2c"), c("3d")).rateHand()
        assertTrue(twoPair.score < threeOfAKind.score, "TwoPair < ThreeOfAKind")
    }

    @Test
    fun `score increases with hand strength - three of a kind to straight`() {
        val threeOfAKind = listOf(c("1h"), c("1d"), c("1s"), c("2c"), c("3d")).rateHand()
        val straight = listOf(c("1h"), c("2d"), c("3s"), c("4c"), c("5d")).rateHand()
        assertTrue(threeOfAKind.score < straight.score, "ThreeOfAKind < Straight")
    }

    @Test
    fun `score increases with hand strength - straight to flush`() {
        val straight = listOf(c("1h"), c("2d"), c("3s"), c("4c"), c("5d")).rateHand()
        val flush = listOf(c("1h"), c("2h"), c("3h"), c("4h"), c("5h")).rateHand()
        assertTrue(straight.score < flush.score, "Straight < Flush")
    }

    @Test
    fun `score increases with hand strength - flush to full house`() {
        val flush = listOf(c("1h"), c("2h"), c("3h"), c("4h"), c("5h")).rateHand()
        val fullHouse = listOf(c("1h"), c("1d"), c("1s"), c("2c"), c("2d")).rateHand()
        assertTrue(flush.score < fullHouse.score, "Flush < FullHouse")
    }

    @Test
    fun `score increases with hand strength - full house to four of a kind`() {
        val fullHouse = listOf(c("1h"), c("1d"), c("1s"), c("2c"), c("2d")).rateHand()
        val fourOfAKind = listOf(c("1h"), c("1d"), c("1s"), c("1c"), c("2d")).rateHand()
        assertTrue(fullHouse.score < fourOfAKind.score, "FullHouse < FourOfAKind")
    }

    @Test
    fun `score increases with hand strength - four of a kind to straight flush`() {
        val fourOfAKind = listOf(c("1h"), c("1d"), c("1s"), c("1c"), c("2d")).rateHand()
        val straightFlush = listOf(c("1h"), c("2h"), c("3h"), c("4h"), c("5h")).rateHand()
        assertTrue(fourOfAKind.score < straightFlush.score, "FourOfAKind < StraightFlush")
    }

    @Test
    fun `higher pair beats lower pair`() {
        val lowPair = listOf(c("1h"), c("1d"), c("2s"), c("3c"), c("4d")).rateHand()
        val highPair = listOf(c("13h"), c("13d"), c("2s"), c("3c"), c("4d")).rateHand()
        assertTrue(lowPair.score < highPair.score, "LowPair < HighPair")
    }

    @Test
    fun `two pair - higher kicker wins when same pairs`() {
        val lowKicker = listOf(c("1h"), c("1d"), c("2s"), c("2c"), c("3d")).rateHand()
        val highKicker = listOf(c("1h"), c("1d"), c("2s"), c("2c"), c("5d")).rateHand()
        assertTrue(lowKicker.score < highKicker.score, "LowKicker < HighKicker")
    }

    @Test
    fun `two pair - higher top pair wins`() {
        val lowerTwoPair = listOf(c("1h"), c("1d"), c("2s"), c("2c"), c("3d")).rateHand()
        val higherTwoPair = listOf(c("1h"), c("1d"), c("13s"), c("13c"), c("3d")).rateHand()
        assertTrue(lowerTwoPair.score < higherTwoPair.score, "LowerTwoPair < HigherTwoPair")
    }

    @Test
    fun `three of a kind - higher trips wins`() {
        val lowTrips = listOf(c("1h"), c("1d"), c("1s"), c("2c"), c("3d")).rateHand()
        val highTrips = listOf(c("13h"), c("13d"), c("13s"), c("2c"), c("3d")).rateHand()
        assertTrue(lowTrips.score < highTrips.score, "LowTrips < HighTrips")
    }

    @Test
    fun `straight - higher straight wins`() {
        val lowStraight = listOf(c("1h"), c("2d"), c("3s"), c("4c"), c("5d")).rateHand()
        val highStraight = listOf(c("10h"), c("11d"), c("12s"), c("13c"), c("1d")).rateHand()
        assertTrue(lowStraight.score < highStraight.score, "LowStraight < HighStraight")
    }

    @Test
    fun `flush - higher flush wins`() {
        val lowFlush = listOf(c("1h"), c("2h"), c("3h"), c("4h"), c("5h")).rateHand()
        val highFlush = listOf(c("10h"), c("11h"), c("12h"), c("13h"), c("1h")).rateHand()
        assertTrue(lowFlush.score < highFlush.score, "LowFlush < HighFlush")
    }

    @Test
    fun `full house - higher trips wins`() {
        val lowFullHouse = listOf(c("1h"), c("1d"), c("1s"), c("2c"), c("2d")).rateHand()
        val highFullHouse = listOf(c("13h"), c("13d"), c("13s"), c("2c"), c("2d")).rateHand()
        assertTrue(lowFullHouse.score < highFullHouse.score, "LowFullHouse < HighFullHouse")
    }

    @Test
    fun `four of a kind - higher quads wins`() {
        val lowQuads = listOf(c("1h"), c("1d"), c("1s"), c("1c"), c("2d")).rateHand()
        val highQuads = listOf(c("13h"), c("13d"), c("13s"), c("13c"), c("2d")).rateHand()
        assertTrue(lowQuads.score < highQuads.score, "LowQuads < HighQuads")
    }

    @Test
    fun `straight flush - higher straight flush wins`() {
        val lowStraightFlush = listOf(c("1h"), c("2h"), c("3h"), c("4h"), c("5h")).rateHand()
        val highStraightFlush = listOf(c("10h"), c("11h"), c("12h"), c("13h"), c("1h")).rateHand()
        assertTrue(lowStraightFlush.score < highStraightFlush.score, "LowStraightFlush < HighStraightFlush")
    }

    @Test
    fun `high card - higher kicker wins`() {
        val lowHighCard = listOf(c("1h"), c("2d"), c("3s"), c("4c"), c("5d")).rateHand()
        val highHighCard = listOf(c("1h"), c("2d"), c("3s"), c("4c"), c("7d")).rateHand()
        assertTrue(lowHighCard.score < highHighCard.score, "LowHighCard < HighHighCard")
    }

    private fun c(cardStr: String): Card {
        val suit = when (cardStr.last()) {
            'h' -> Suit.Hearts
            'd' -> Suit.Diamonds
            's' -> Suit.Spades
            'c' -> Suit.Clubs
            else -> throw IllegalStateException("Invalid suit: ${cardStr.last()}")
        }
        val rank = cardStr.dropLast(1).toInt()
        return Card(suit, rank)
    }
}