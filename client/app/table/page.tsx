'use client';

const LOCAL_PLAYER_ID = 9;

interface Card {
  suit: string;
  rank: string;
}

interface Player {
  id: number;
  name: string;
  stack: number;
  seat: number;
  cards?: Card[];
}

const TABLE_PLAYERS: Player[] = [
  { id: 1, name: 'Player 1', stack: 1000, seat: 0 },
  { id: 2, name: 'Player 2', stack: 1500, seat: 1 },
  { id: 3, name: 'Player 3', stack: 2000, seat: 2 },
  { id: 4, name: 'Player 4', stack: 1200, seat: 3 },
  { id: 5, name: 'Player 5', stack: 1800, seat: 4 },
  { id: 6, name: 'Player 6', stack: 900, seat: 5 },
  { id: 7, name: 'Player 7', stack: 1100, seat: 6 },
  { id: 8, name: 'Player 8', stack: 1600, seat: 7 },
  { id: 9, name: 'You', stack: 5000, seat: 8, cards: [{ suit: '♠', rank: 'A' }, { suit: '♥', rank: 'K' }] },
];

const DEALER_SEAT = 4;

const SEAT_POSITIONS = [
  { x: 50, y: 8 },
  { x: 80, y: 18 },
  { x: 90, y: 40 },
  { x: 80, y: 65 },
  { x: 50, y: 92 },
  { x: 20, y: 65 },
  { x: 10, y: 40 },
  { x: 20, y: 18 },
];

const COMMUNITY_CARDS: Card[] = [
  { suit: '♠', rank: 'Q' },
  { suit: '♥', rank: 'J' },
  { suit: '♦', rank: '10' },
  { suit: '♣', rank: '9' },
  { suit: '♠', rank: '2' },
];

function Card({ card, hidden }: { card: Card | null; hidden?: boolean }) {
  if (!card || hidden) {
    return (
      <div className="w-12 h-16 sm:w-14 sm:h-20 bg-[#1a5c32] border-2 border-[#0d3d22] rounded flex items-center justify-center">
        <span className="text-[#2d8a4e] text-lg">?</span>
      </div>
    );
  }

  const isRed = card.suit === '♥' || card.suit === '♦';

  return (
    <div className={`w-12 h-16 sm:w-14 sm:h-20 bg-white border-2 border-gray-300 rounded flex flex-col items-center justify-center ${isRed ? 'text-red-600' : 'text-gray-900'}`}>
      <span className="text-sm font-bold">{card.rank}</span>
      <span className="text-lg">{card.suit}</span>
    </div>
  );
}

function PlayerSeat({ player, isDealer, localPlayerSeat }: { player: Player; isDealer: boolean; localPlayerSeat: number }) {
  const relativeSeat = (player.seat - localPlayerSeat + SEAT_POSITIONS.length) % SEAT_POSITIONS.length;
  const seatIndex = (relativeSeat + 4) % SEAT_POSITIONS.length;
  const position = SEAT_POSITIONS[seatIndex];

  return (
    <div
      className="absolute flex flex-col items-center"
      style={{ left: `${position.x}%`, top: `${position.y}%`, transform: 'translate(-50%, -50%)' }}
    >
      {isDealer && (
        <div className="absolute -top-6 w-6 h-6 bg-white rounded-full border-2 border-[#1a5c32] flex items-center justify-center text-xs font-bold text-[#1a5c32] shadow-md">
          D
        </div>
      )}
      <div className="flex gap-1 mb-1">
        <Card card={player.cards?.[0] ?? null} hidden={!player.cards} />
        <Card card={player.cards?.[1] ?? null} hidden={!player.cards} />
      </div>
      <div className="bg-[#0d3d22] text-white px-3 py-1 rounded text-sm">
        <div className="font-bold">{player.name}</div>
        <div className="text-yellow-400">${player.stack}</div>
      </div>
    </div>
  );
}

export default function Table() {
  return (
    <div className="min-h-screen bg-[#0f3020] flex items-center justify-center p-4">
      <div className="relative w-full max-w-3xl aspect-[4/3]">
        <div className="absolute inset-0 bg-[#35654d] rounded-full border-8 border-[#1a5c32] shadow-2xl">
          <div className="absolute inset-4 bg-[#2d5a3d] rounded-full border-4 border-[#1a472a]">
            <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 flex flex-col items-center gap-4">
              <div className="flex gap-2">
                {COMMUNITY_CARDS.map((card, i) => (
                  <Card key={i} card={card} />
                ))}
              </div>
              <div className="bg-[#0d3d22] text-white px-6 py-2 rounded-full text-xl font-bold border-4 border-[#1a3622]">
                POT: $2,450
              </div>
            </div>

            {TABLE_PLAYERS.map((player) => (
              <PlayerSeat 
                key={player.id} 
                player={player} 
                isDealer={player.seat === DEALER_SEAT}
                localPlayerSeat={TABLE_PLAYERS.find(p => p.id === LOCAL_PLAYER_ID)!.seat}
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
