import domain/model.{
  type HandState, type Seat, Call, Check, Fold, HandState, Seat,
}
import gleam/bool.{guard}
import gleam/float
import gleam/list
import gleam/option.{type Option, None, Some}

pub fn perform_check_fold(state: HandState, seat_id: Int) -> HandState {
  use <- guard(state.current_action.seat_in_turn != seat_id, state)

  let seats =
    list.index_map(state.seats, fn(seat, index) {
      case index == seat_id {
        True ->
          case state.current_action.min_raise >. seat.current_raise {
            True -> Seat(..seat, last_action: Some(Fold), out: True)
            False -> Seat(..seat, last_action: Some(Check))
          }
        False -> seat
      }
    })

  HandState(..state, seats:)
}

pub fn perform_call(state: HandState, seat_id: Int) -> HandState {
  use <- guard(state.current_action.seat_in_turn != seat_id, state)

  let seats =
    list.index_map(state.seats, fn(seat, index) {
      case index == seat_id {
        True -> {
          let call_amount =
            float.min(
              seat.stack,
              state.current_action.min_raise -. seat.current_raise,
            )
          Seat(..seat, last_action: Some(Call), stack: seat.stack -. call_amount, current_raise: seat.current_raise +. call_amount)
        }
        False -> seat
      }
    })

  HandState(..state, seats:)
}

pub fn perform_bet_raise(
  state: HandState,
  seat_id: Int,
  raise_amount: Float,
) -> HandState {
  todo
}

fn is_action_out_of_turn(state: HandState, seat_id: Int) -> Bool {
  todo
}
