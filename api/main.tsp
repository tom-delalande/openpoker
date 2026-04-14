import "@typespec/http";

using Http;

@service(#{ title: "OpenPoker API" })
namespace OpenPoker.V1;

model AuthRequest {
  name: string;
}

model GetTablesRequest {}

model GetTablesResponse {}

@discriminated
union HandEvent {
  PlayerSatDown: PlayerSatDown,
  PlayerStoodUp: PlayerStoodUp,
  HandStarted: HandStarted,
  RoundStarted: RoundStarted,
  CommunityCardDealt: CommunityCardDealt,
  PrivateCardDealt: PrivateCardDealt,
  HandFinished: HandFinished,
  PlayerActionRequested: PlayerActionRequested,
  PlayerPostedSmallBlind: PlayerPostedSmallBlind,
  PlayerPostedBigBlind: PlayerPostedBigBlind,
  PlayerPostedAnte: PlayerPostedAnte,
  PlayerFolded: PlayerFolded,
  PlayerChecked: PlayerChecked,
  PlayerBet: PlayerBet,
  PlayerRaised: PlayerRaised,
  PlayerCalled: PlayerCalled,
  PlayerShowedCard: PlayerShowedCard,
}

model PlayerSatDown {
  type: "PlayerSatDownEvent";
  playerId: int32;
  playerName: string;
  seat: int32;
  stack: float64;
}

model PlayerStoodUp {
  type: "PlayerStoodUpEvent";
  playerId: int32;
}

model HandStarted {
  type: "HandStartedEvent";
  dealerButton: int32;
}

model RoundStarted {
  type: "RoundStartedEvent";
  street: string;
}

model CommunityCardDealt {
  type: "CommunityCardDealtEvent";
  cards: string[];
}

model PrivateCardDealt {
  type: "PrivateCardDealtEvent";
  playerId: int32;
  cards: string[];
}

model PlayerStack {
  playerId: int32;
  stack: float64;
  winner: boolean;
}

model HandFinished {
  type: "HandFinished";
  players: PlayerStack[];
}

model PlayerActionRequested {
  type: "PlayerActionRequested";
  playerId: int32;
  actionOptions: ActionOptions[];
  expiry: utcDateTime;
}

@discriminated
union ActionOptions {
  Fold: FoldOption,
  Check: CheckOption,
  Bet: BetOption,
  Raise: RaiseOption,
  Call: CallOption,
  PostSmallBlind: PostSmallBlindOption,
  PostBigBlind: PostBigBlindOption,
}

model FoldOption {
  type: "FoldOption";
}
model CheckOption {
  type: "CheckOption";
}
model BetOption {
  type: "BetOption";
  minAmount: float64;
  maxAmount?: float64;
}

model CallOption {
  type: "CallOption";
  amount: float64;
}

model RaiseOption {
  type: "RaiseOption";
  minAmount: float64;
  maxAmount?: float64;
}
model PostSmallBlindOption {
  type: "PostSmallBlindOption";
  amount: float64;
}
model PostBigBlindOption {
  type: "PostBigBlindOption";
  amount: float64;
}

model PlayerPostedSmallBlind {
  type: "PlayerPostedSmallBlind";
  playerId: int32;
  amount: float64;
  isAllIn: boolean;
}
model PlayerPostedBigBlind {
  type: "PlayerPostedBigBlind";
  playerId: int32;
  amount: float64;
  isAllIn: boolean;
}
model PlayerPostedAnte {
  type: "PlayerPostedAnte";
  playerId: int32;
  amount: float64;
  isAllIn: boolean;
}
model PlayerFolded {
  type: "PlayerFolded";
  playerId: int32;
}
model PlayerChecked {
  type: "PlayerChecked";
  playerId: int32;
}
model PlayerBet {
  type: "PlayerBet";
  playerId: int32;
  amount: float64;
  isAllIn: boolean;
}
model PlayerRaised {
  type: "PlayerRaised";
  playerId: int32;
  amount: float64;
  isAllIn: boolean;
}
model PlayerCalled {
  type: "PlayerCalled";
  playerId: int32;
  amount: float64;
  isAllIn: boolean;
}
model PlayerShowedCard {
  type: "PlayerShowedCard";
  playerId: int32;
  cards: string[];
}

@discriminated
union PlayerAction {
  Fold: Fold,
  Check: Check,
  Call: Call,
  Bet: Bet,
  Raise: Raise,
  PostSmallBlind: PostSmallBlind,
  PostBigBlind: PostBigBlind,
  StandUp: StandUp,
  SitDown: SitDown,
}

model Fold {
  type: "Fold";
}
model Check {
  type: "Check";
}
model Call {
  type: "Call";
  amount: float64;
}
model Bet {
  type: "Bet";
  amount: float64;
}
model Raise {
  type: "Raise";
  amount: float64;
}
model PostSmallBlind {
  type: "PostSmallBlind";
  amount: float64;
}
model PostBigBlind {
  type: "PostBigBlind";
  amount: float64;
}
model StandUp {
  type: "StandUp";
}
model SitDown {
  type: "SitDown";
}

model AuthResponse {
  token: string;
  playerId: int32;
}

model PlayerInfo {
  playerId: int32;
  name: string;
  stack: float64;
}

@route("/auth")
interface AuthService {
  @route("/login")
  @post
  login(@query name: string): AuthResponse;
}
@route("/game")
interface GameService {
  @route("/player")
  @get
  player(@query token: string): PlayerInfo;
  @route("/join")
  @post
  join(@query token: string): string;
}

@route("/table")
interface TableService {
  @route("/ws/table/{tableId}/token/{token}")
  connect(@path tableId: string, @path token: string): HandEvent | HandEvent[];
}
