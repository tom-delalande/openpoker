import gleam/option.{type Option, None, Some}

pub type Card {
  Card(suit: String, rank: Int)
}

pub type Seat {
  Seat(
    player_id: Int,
    cards: List(Card),
    stack: Float,
    out: Bool,
    last_action: Option(Action),
    current_raise: Float,
    hand_strength: String,
  )
}

pub type Action {
  SmallBlind
  BigBlind
  Fold
  Check
  Raise
  Bet
  Call
}

pub type Round {
  Blinds
  Flop
  Turn
  River
}

pub type CurrentAction {
  CurrentAction(seat_in_turn: Int, min_raise: Float, last_seat_to_raise: Int)
}

pub type HandState {
  HandState(
    seats: List(Seat),
    small_blind_amount: Float,
    big_blind_amount: Float,
    current_action: CurrentAction,
    round: Round,
    community_cards: List(Card),
    pot: Int,
    deck: List(Card),
    finished: Bool,
    game_finished: Bool,
    winners: List(Int),
  )
}
