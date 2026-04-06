package domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val OPEN_HAND_HISTORY_SPEC_VERSION = "1.4.7"

@Serializable
data class OpenHandHistoryDocument(
    val ohh: OpenHandHistory,
)

@Serializable
data class OpenHandHistory(
    @SerialName("spec_version")
    val specVersion: String = OPEN_HAND_HISTORY_SPEC_VERSION,
    @SerialName("site_name")
    val siteName: String,
    @SerialName("network_name")
    val networkName: String,
    @SerialName("internal_version")
    val internalVersion: String = OPEN_HAND_HISTORY_SPEC_VERSION,
    val tournament: Boolean,
    @SerialName("tournament_info")
    val tournamentInfo: TournamentInfo? = null,
    @SerialName("game_number")
    val gameNumber: String,
    @SerialName("start_date_utc")
    val startDateUtc: String,
    @SerialName("table_name")
    val tableName: String,
    @SerialName("table_handle")
    val tableHandle: String? = null,
    @SerialName("table_skin")
    val tableSkin: String? = null,
    @SerialName("game_type")
    val gameType: GameType,
    @SerialName("bet_limit")
    val betLimit: BetLimit,
    @SerialName("table_size")
    val tableSize: Int,
    val currency: String,
    @SerialName("dealer_seat")
    val dealerSeat: Int,
    @SerialName("small_blind_amount")
    val smallBlindAmount: Double? = null,
    @SerialName("big_blind_amount")
    val bigBlindAmount: Double? = null,
    @SerialName("ante_amount")
    val anteAmount: Double,
    @SerialName("hero_player_id")
    val heroPlayerId: Int? = null,
    val flags: List<String>? = null,
    val players: List<Player>,
    val rounds: List<Round>,
    val pots: List<Pot>,
    @SerialName("tournament_bounties")
    val tournamentBounties: List<TournamentBounty> = emptyList(),
)

@Serializable
data class BetLimit(
    @SerialName("bet_type")
    val betType: BetType,
    @SerialName("bet_cap")
    val betCap: Double,
)

@Serializable
data class Player(
    val name: String,
    val id: Int,
    val seat: Int,
    @SerialName("starting_stack")
    val startingStack: Double,
    @SerialName("player_bounty")
    val playerBounty: Double? = null,
    val display: String? = null,
    @SerialName("is_sitting_out")
    val isSittingOut: Boolean? = null,
)

@Serializable
data class Round(
    val id: Int,
    val street: RoundStreet,
    val cards: List<String>? = null,
    val actions: List<Action>,
)

@Serializable
data class Action(
    @SerialName("action_number")
    val actionNumber: Int,
    @SerialName("player_id")
    val playerId: Int? = null,
    val action: ActionType,
    val amount: Double? = null,
    @SerialName("is_allin")
    val isAllIn: Boolean? = null,
    val cards: List<String>? = null,
)

@Serializable
data class Pot(
    val number: Int,
    val amount: Double,
    val rake: Double? = null,
    val jackpot: Double? = null,
    @SerialName("player_wins")
    val playerWins: List<PlayerWin>,
)

@Serializable
data class PlayerWin(
    @SerialName("player_id")
    val playerId: Int,
    @SerialName("win_amount")
    val winAmount: Double,
    @SerialName("contributed_rake")
    val contributedRake: Double? = null,
    val cashout: Double? = null,
    @SerialName("cashout_fee")
    val cashoutFee: Double? = null,
    val bonus: Double? = null,
)

@Serializable
data class TournamentInfo(
    val name: String,
    @SerialName("start_date_utc")
    val startDateUtc: String,
    val type: TournamentType,
    @SerialName("buyin_amount")
    val buyinAmount: Double? = null,
    val currency: String,
    @SerialName("fee_amount")
    val feeAmount: Double? = null,
    @SerialName("bounty_fee_amount")
    val bountyFeeAmount: Double? = null,
    @SerialName("tournament_number")
    val tournamentNumber: String,
    val flags: List<String>? = null,
    @SerialName("initial_stack")
    val initialStack: Double,
    val speed: Speed,
)

enum class TournamentType {
    STT,
    MTT,
}

enum class TournamentFlags {
    SNG,
    DON,
    Bounty,
    Shootout,
    Rebuy,
    Matrix,
    Push_Or_Fold,
    Satellite,
    Steps,
    Deep,

    @SerialName("Multi-Entry")
    MultiEntry,
    Fifty50,
    Flipout,
    TripleUp,
    Lottery,

    @SerialName("Re-Entry")
    ReEntry,
    Power_Up,

    @SerialName("Progressive-Bounty")
    ProgressiveBounty,
}

@Serializable
data class TournamentBounty(
    @SerialName("player_id")
    val playerId: Int,
    val amount: Double,
    @SerialName("hand_number")
    val handNumber: String? = null,
)

@Serializable
data class Speed(
    val type: String,
    @SerialName("round_time")
    val roundTime: Int,
)

enum class SpeedType {
    Normal,

    @SerialName("Semi-Turbo")
    SemiTurbo,
    Turbo,

    @SerialName("Super-Turbo")
    SuperTurbo,

    @SerialName("Hyper-Turbo")
    HyperTurbo,

    @SerialName("Ultra-Turbo")
    UltraTurbo,
}

@Serializable
enum class GameType {
    @SerialName("Holdem")
    HOLD_EM,

    @SerialName("Omaha")
    OMAHA,

    @SerialName("OmahaHiLo")
    OMAHA_HI_LO,

    @SerialName("Stud")
    STUD,

    @SerialName("StudHiLo")
    STUD_HI_LO,

    @SerialName("Draw")
    DRAW,
}

@Serializable
enum class BetType {
    @SerialName("No Limit")
    NO_LIMIT,

    @SerialName("Pot Limit")
    POT_LIMIT,

    @SerialName("Limit")
    LIMIT,
}

@Serializable
enum class RoundStreet {
    @SerialName("Preflop")
    PRE_FLOP,

    @SerialName("Flop")
    FLOP,

    @SerialName("Turn")
    TURN,

    @SerialName("River")
    RIVER,

    @SerialName("Showdown")
    SHOWDOWN,
}

@Serializable
enum class ActionType {
    @SerialName("Dealt Cards")
    DEALT_CARDS,

    @SerialName("Mucks Cards")
    MUCKS_CARDS,

    @SerialName("Shows Cards")
    SHOWS_CARDS,

    @SerialName("Post Ante")
    POST_ANTE,

    @SerialName("Post SB")
    POST_SB,

    @SerialName("Post BB")
    POST_BB,

    @SerialName("Straddle")
    STRADDLE,

    @SerialName("Post Dead")
    POST_DEAD,

    @SerialName("Post Extra Blind")
    POST_EXTRA_BLIND,

    @SerialName("Fold")
    FOLD,

    @SerialName("Check")
    CHECK,

    @SerialName("Bet")
    BET,

    @SerialName("Raise")
    RAISE,

    @SerialName("Call")
    CALL,

    @SerialName("Added Chips")
    ADDED_CHIPS,

    @SerialName("Sits Down")
    SITS_DOWN,

    @SerialName("Stands Up")
    STANDS_UP,

    @SerialName("Add to Pot")
    ADD_TO_POT,
}
