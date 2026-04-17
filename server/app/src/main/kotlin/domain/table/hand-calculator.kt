package domain.table

import domain.model.Table.Card

data class HandRating(
    val handStrength: HandStrength,
    val score: Double,
)

enum class HandStrength {
    StraightFlush,
    Straight,
    Flush,
    FourOfAKind,
    FullHouse,
    ThreeOfAKind,
    TwoPair,
    Pair,
    HighCard,
}

fun List<Card>.rateHand(): HandRating {
    val hands = calculateHands()
    val highCards = hands.highCard
    if (hands.straightFlush.isNotEmpty()) {
        val score = calculateHandScore(HandStrength.StraightFlush, hands.straightFlush, highCards)
        return HandRating(
            HandStrength.StraightFlush,
            score,
        )
    }

    if (hands.fourOfAKind.isNotEmpty()) {
        val score = calculateHandScore(HandStrength.FourOfAKind, hands.fourOfAKind, highCards)
        return HandRating(
            HandStrength.FourOfAKind,
            score,
        )
    }

    if (hands.fullHouse.isNotEmpty()) {
        val score = calculateHandScore(
            HandStrength.FullHouse,
            listOf(hands.fullHouse.first().threeOfAKind, hands.fullHouse.first().pair),
            highCards
        )
        return HandRating(
            HandStrength.FullHouse,
            score,
        )
    }

    if (hands.flush.isNotEmpty()) {
        val score = calculateHandScore(HandStrength.Flush, hands.flush, highCards)
        return HandRating(
            HandStrength.Flush,
            score,
        )
    }

    if (hands.straight.isNotEmpty()) {
        val score = calculateHandScore(HandStrength.Straight, hands.straight, highCards)
        return HandRating(
            HandStrength.Straight,
            score,
        )
    }

    if (hands.threeOfAKind.isNotEmpty()) {
        val score = calculateHandScore(HandStrength.ThreeOfAKind, hands.threeOfAKind, highCards)
        return HandRating(
            HandStrength.ThreeOfAKind,
            score,
        )
    }
    if (hands.pair.size > 1) {
        val score = calculateHandScore(HandStrength.TwoPair, hands.pair, highCards)
        return HandRating(
            HandStrength.TwoPair,
            score,
        )
    }
    if (hands.pair.isNotEmpty()) {
        val score = calculateHandScore(HandStrength.Pair, hands.pair, highCards)
        return HandRating(
            HandStrength.Pair,
            score,
        )
    }
    val score = calculateHandScore(HandStrength.HighCard, hands.highCard, highCards)
    return HandRating(
        HandStrength.HighCard,
        score,
    )
}

data class FullHouse(
    val threeOfAKind: Int,
    val pair: Int,
)

data class ScoredHands(
    val highCard: List<Int>,
    val pair: List<Int>,
    val threeOfAKind: List<Int>,
    val fullHouse: List<FullHouse>,
    val flush: List<Int>,
    val fourOfAKind: List<Int>,
    val straight: List<Int>,
    val straightFlush: List<Int>,
)

private fun List<Card>.calculateHands(): ScoredHands {
    val cardScores = listOf(0, 14, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13)

    val sortedCards = map { it.copy(rank = cardScores[it.rank]) }.sortedBy { it.rank }

    val rankFrequency = sortedCards.fold(mutableMapOf<Int, Int>()) { map, card ->
        val current = map[card.rank] ?: 0
        map[card.rank] = current + 1
        map
    }

    val suitFrequency = sortedCards.fold(mutableMapOf<Card.Suit, Int>()) { map, card ->
        val current = map[card.suit] ?: 0
        map[card.suit] = current + 1
        map
    }

    val highCard = sortedCards.map { it.rank }
    val pairCombinations = rankFrequency.calculatePairCombinations()
    val flushes = suitFrequency.calculateFlushes(sortedCards)
    val straights = rankFrequency.calculateStraights()
    val straightFlushes = calculateStraightFlushes(sortedCards)

    return ScoredHands(
        highCard.sortedDescending(),
        pairCombinations.pair.sortedDescending(),
        pairCombinations.threeOfAKind.sortedDescending(),
        pairCombinations.fullHouse.sortedWith(compareBy({ it.threeOfAKind }, { it.pair })).reversed(),
        flushes.sortedDescending(),
        pairCombinations.fourOfAKind.sortedDescending(),
        straights.sortedDescending(),
        straightFlushes.sortedDescending()
    )
}

private data class PairCombinations(
    val pair: List<Int>,
    val threeOfAKind: List<Int>,
    val fourOfAKind: List<Int>,
    val fullHouse: List<FullHouse>,

    )

private fun Map<Int, Int>.calculatePairCombinations(): PairCombinations {
    val pair = mutableListOf<Int>()
    val threeOfAKind = mutableListOf<Int>()
    val fourOfAKind = mutableListOf<Int>()
    val fullHouse = mutableListOf<FullHouse>()

    forEach { (key, value) ->
        if (pair.isNotEmpty() && value >= 3) {
            fullHouse.add(
                FullHouse(
                    key,
                    pair.first()
                )
            )
        }
        if (threeOfAKind.isNotEmpty() && value >= 2) {
            fullHouse.add(
                FullHouse(
                    threeOfAKind.first(),
                    key,
                )
            )
        }
        if (value >= 2) pair.add(key)
        if (value >= 3) threeOfAKind.add(key)
        if (value >= 4) fourOfAKind.add(key)
    }
    return PairCombinations(
        pair,
        threeOfAKind,
        fourOfAKind,
        fullHouse,
    )
}

private fun Map<Card.Suit, Int>.calculateFlushes(sortedCards: List<Card>): List<Int> {
    val flush = mutableListOf<Int>()
    forEach { (key, value) ->
        if (value >= 5) {
            flush.add(sortedCards.maxBy { it.suit == key }.rank)
        }
    }
    return flush
}

private fun Map<Int, Int>.calculateStraights(): List<Int> {
    val straight = mutableListOf<Int>()
    val ranks = mutableListOf<Int>()

    if (containsKey(14)) {
        ranks.add(1)
    }
    ranks.addAll(keys)

    ranks.forEachIndexed { index, rank ->
        if (ranks.size < index + 5) {
            return@forEachIndexed
        }
        if (
            ranks[index + 1] == rank + 1 &&
            ranks[index + 2] == rank + 2 &&
            ranks[index + 3] == rank + 3 &&
            ranks[index + 4] == rank + 4
        ) {
            straight.add(rank + 4)
        }
    }
    return straight
}

private fun calculateStraightFlushes(cards: List<Card>): List<Int> {
    val straight = mutableListOf<Int>()
    val aces = cards.filter { it.rank == 14 }

    val editedCards = (cards + aces.map { it.copy(rank = 1) }).sortedBy { it.rank }

    editedCards.groupBy { it.suit }
        .forEach { suit, cards ->
            cards.forEachIndexed { index, card ->
                if (cards.size < index + 5) {
                    return@forEachIndexed
                }
                if (
                    cards[index + 1].rank == card.rank + 1 &&
                    cards[index + 2].rank == card.rank + 2 &&
                    cards[index + 3].rank == card.rank + 3 &&
                    cards[index + 4].rank == card.rank + 4 &&
                    cards.subList(index, index + 5).groupBy { it.suit }.size == 1
                ) {
                    straight.add(card.rank + 4)
                }
            }
        }
    return straight
}


private fun calculateHandScore(handStrength: HandStrength, handCards: List<Int>, highCards: List<Int>): Double {
    if (handCards.isEmpty()) return 0.0
    val extraCards = highCards
        .filterNot { handCards.contains(it) }
        .toMutableList()
    while (extraCards.size < 4) {
        extraCards.add(0)
    }

    return when (handStrength) {
        HandStrength.HighCard -> 0.007 * handCards[0] +
                0.00007 * (handCards.getOrNull(0) ?: 0) +
                0.0000007 * (handCards.getOrNull(1) ?: 0) +
                0.000000007 * (handCards.getOrNull(2) ?: 0) +
                0.00000000007 * (handCards.getOrNull(3) ?: 0)

        HandStrength.Pair -> 0.2 + 0.007 * handCards[0] +
                0.00007 * extraCards[0] +
                0.0000007 * extraCards[1] +
                0.000000007 * extraCards[2]

        HandStrength.TwoPair -> 0.3 + 0.007 * handCards[0] +
                0.00007 * handCards[1] +
                0.0000007 * extraCards[0]

        HandStrength.ThreeOfAKind -> 0.4 + 0.007 * handCards[0] +
                0.00007 * extraCards[0] +
                0.0000007 * extraCards[1]

        HandStrength.Straight -> 0.5 + 0.007 * handCards[0]
        HandStrength.Flush -> 0.6 + 0.007 * handCards[0]
        HandStrength.FullHouse -> 0.7 + 0.007 * handCards[0] + 0.00007 * handCards[1]
        HandStrength.FourOfAKind -> 0.8 + 0.007 * handCards[0] + 0.00007 * extraCards[1]
        HandStrength.StraightFlush -> 0.9 + 0.007 * handCards[0]
    }
}