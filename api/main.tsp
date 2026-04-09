import "@typespec/http";

using Http;

@service(#{ title: "OpenPoker API" })
namespace OpenPoker.V1;

model AuthRequest {
  name: string;
}

model AuthResponse {
  token: string;
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
  seat: int32;
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

model HandFinished {
  type: "HandFinished";
  winners: int32[];
}

model PlayerActionRequested {
  type: "PlayerActionRequested";
  playerId: int32;
  actionOptions: ActionOptions[];
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
  cards: string[];
}

@discriminated
union PlayerAction {
  Fold: Fold,
  Check: Check,
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
  type: "Check";
}
model SitDown {
  type: "Check";
}

@route("/game")
interface GameService {
  @route("/auth")
  @post
  auth(@body request: AuthRequest): AuthResponse;

  @route("/tables")
  @post
  getTables(@body request: GetTablesRequest): GetTablesResponse;
}

@route("/table")
interface TableService {
  // TypeSpec/OpenAPI cannot express the bidirectional gRPC stream directly.
  // This keeps the payload contract while approximating the connect call as a unary operation.
  @route("/connect")
  @get
  connect(@body action: PlayerAction): HandEvent;
}

