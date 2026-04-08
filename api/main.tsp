import "@typespec/http";

using Http;

@service(#{ title: "OpenPoker API" })
namespace OpenPoker.V1;

model AuthRequest {
  name: string
}

model AuthResponse {
  token: string
}

model GetTablesRequest {}

model GetTablesResponse {

}

@discriminated
union HandEvent {
  HandStartedEvent: HandStartedEvent,
  RoundStartedEvent: RoundStartedEvent
}


model HandStartedEvent {
  type: "HandStartedEvent";
  dealerButton: int32
}

model RoundStartedEvent {
  type: "RoundStartedEvent"
}

alias HandEventType =
  | TableEventDealtCards
  | TableEventMucksCards
  | TableEventShowsCards
  | TableEventPostAnte
  | TableEventPostSmallBlind
  | TableEventPostBigBlind
  | TableEventPostStraddle
  | TableEventPostDeadBlind
  | TableEventPostExtraBlind
  | TableEventFold
  | TableEventCheck
  | TableEventBet
  | TableEventRaise
  | TableEventCall
  | TableEventAddedChips
  | TableEventSitsDown
  | TableEventStandsUp
  | TableEventAddToPot
  | TableEventRequestPlayerAction
  | TableEventDealTableCards
  | TableEventDealPrivateCards;

model TableEventDealtCards {
  player_id: string;
  card_string: string[];
}

model TableEventMucksCards {
  player_id: int32;
}

model TableEventShowsCards {
  player_id: int32;
  card_string: string[];
}

model TableEventPostAnte {
  player_id: int32;
  amount: float64;
  is_allin: boolean;
}

model TableEventPostSmallBlind {
  player_id: int32;
  amount: float64;
  is_allin: boolean;
}

model TableEventPostBigBlind {
  player_id: int32;
  amount: float64;
  is_allin: boolean;
}

model TableEventPostStraddle {
  player_id: int32;
  amount: float64;
  is_allin: boolean;
}

model TableEventPostDeadBlind {
  player_id: int32;
  amount: float64;
  is_allin: boolean;
}

model TableEventPostExtraBlind {
  player_id: int32;
  amount: float64;
  is_allin: boolean;
}

model TableEventFold {
  player_id: int32;
}

model TableEventCheck {
  player_id: int32;
}

model TableEventBet {
  player_id: int32;
  amount: float64;
  is_allin: boolean;
}

model TableEventRaise {
  player_id: int32;
  amount: float64;
  is_allin: boolean;
}

model TableEventCall {
  player_id: int32;
}

model TableEventAddedChips {
  player_id: int32;
  amount: float64;
}

model TableEventSitsDown {
  player_id: int32;
}

model TableEventStandsUp {
  player_id: int32;
}

model TableEventAddToPot {
  player_id: int32;
  amount: float64;
}

model TableEventRequestPlayerAction {}

model TableEventDealTableCards {}

model TableEventDealPrivateCards {}

model TableAction {
  kind: TableActionKind;
}

alias TableActionKind =
  | TableActionMuckCards
  | TableActionShowCards
  | TableActionPostAnte
  | TableActionPostSmallBlind
  | TableActionPostBigBlind
  | TableActionPostStraddle
  | TableActionPostDeadBlind
  | TableActionPostExtraBlind
  | TableActionFold
  | TableActionCheck
  | TableActionBet
  | TableActionRaise
  | TableActionCall
  | TableActionAddChips
  | TableActionSitDown
  | TableActionStandUp;

model TableActionPostAnte {}
model TableActionPostSmallBlind {}
model TableActionPostBigBlind {}
model TableActionPostStraddle {}
model TableActionPostDeadBlind {}
model TableActionPostExtraBlind {}
model TableActionFold {}
model TableActionCheck {}
model TableActionBet {}
model TableActionRaise {}
model TableActionCall {}
model TableActionMuckCards {}
model TableActionShowCards {}
model TableActionAddChips {}
model TableActionSitDown {}
model TableActionStandUp {}

model RequestedActionOption {
  kind: RequestedActionOptionKind;
}

alias RequestedActionOptionKind =
  | RequestedActionOptionPostSmallBlind
  | RequestedActionOptionPostBigBlind
  | RequestedActionOptionPostStraddle
  | RequestedActionOptionPostDeadBlind
  | RequestedActionOptionPostExtraBlind
  | RequestedActionOptionFold
  | RequestedActionOptionCheck
  | RequestedActionOptionBet
  | RequestedActionOptionRaise
  | RequestedActionOptionCall;

model RequestedActionOptionPostSmallBlind {
  amount: float64;
}

model RequestedActionOptionPostBigBlind {
  amount: float64;
}

model RequestedActionOptionPostStraddle {
  amount: float64;
}

model RequestedActionOptionPostDeadBlind {
  amount: float64;
}

model RequestedActionOptionPostExtraBlind {
  amount: float64;
}

model RequestedActionOptionFold {}

model RequestedActionOptionBet {
  min_amount: float64;
  max_amount: float64;
}

model RequestedActionOptionCheck {}

model RequestedActionOptionRaise {
  min_amount: float64;
  max_amount: float64;
}

model RequestedActionOptionCall {
  amount: float64;
}

model Pot {
  number: int32;
  amount: float64;
  rake: float64;
  player_wins: PlayerWin[];
}

model PlayerWin {
  player_id: int32;
  win_amount: float64;
  contributed_rake: float64;
}

@route("/game")
interface GameService {
  @route("/auth")
  @post auth(@body request: AuthRequest): AuthResponse;

  @route("/tables")
  @post getTables(@body request: GetTablesRequest): GetTablesResponse;
}

@route("/table")
interface TableService {
  // TypeSpec/OpenAPI cannot express the bidirectional gRPC stream directly.
  // This keeps the payload contract while approximating the connect call as a unary operation.
  @route("/connect")
  @get connect(@body action: TableAction): HandEvent;
}
