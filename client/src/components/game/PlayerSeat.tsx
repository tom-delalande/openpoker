import { formatAmount } from '../../lib/cards';
import { Badge } from '../ui/Badge';
import { Card } from './Card';

interface PlayerSeatProps {
  name: string;
  stack: number;
  seat: number;
  cards?: string[];
  hasFolded?: boolean;
  currentBet: number;
  isDealer: boolean;
  isCurrentPlayer: boolean;
  isLocalPlayer: boolean;
  isWinner: boolean;
  showCards: boolean;
}

export function PlayerSeat({
  name,
  stack,
  cards,
  hasFolded,
  currentBet,
  isDealer,
  isCurrentPlayer,
  isLocalPlayer,
  isWinner,
  showCards,
}: PlayerSeatProps) {
  const displayCards = showCards ? cards : undefined;
  const cardsHidden = hasFolded || !showCards;

  return (
    <div className={`
      flex flex-col items-center transition-all duration-300
      ${isCurrentPlayer ? 'scale-110 z-10' : ''}
    `}>
      {(isDealer || isCurrentPlayer || isWinner) && (
        <div className="flex gap-1 mb-1">
          {isDealer && <Badge variant="dealer">D</Badge>}
          {isCurrentPlayer && !isWinner && <Badge variant="turn">!</Badge>}
          {isWinner && <Badge variant="winner">★</Badge>}
        </div>
      )}

      <div className="flex gap-0.5 mb-1">
        <Card cardStr={displayCards?.[0] || 'XX'} hidden={cardsHidden} size="sm" />
        <Card cardStr={displayCards?.[1] || 'XX'} hidden={cardsHidden} size="sm" />
      </div>

      <div className={`
        bg-[#0d3d22]/95 backdrop-blur-sm text-white px-2 py-1 rounded-lg text-center
        shadow-lg border-2 transition-all duration-200
        ${isCurrentPlayer ? 'border-yellow-400 shadow-yellow-400/30 ring-2 ring-yellow-400/50' : 'border-[#1a5c32]'}
        ${isWinner ? 'border-yellow-400 shadow-yellow-400/50 bg-yellow-400/20' : ''}
        ${hasFolded ? 'opacity-60' : ''}
      `}>
        <div className="font-bold text-sm truncate max-w-[80px]">
          {isLocalPlayer ? 'You' : name}
        </div>
        {currentBet > 0 && (
          <div className="text-blue-300 text-xs font-medium">{formatAmount(currentBet)}</div>
        )}
        <div className={`font-bold ${stack < 100 ? 'text-red-400' : 'text-yellow-400'}`}>
          {formatAmount(stack)}
        </div>
        {hasFolded && (
          <div className="text-red-400 text-xs font-medium mt-0.5">Folded</div>
        )}
      </div>
    </div>
  );
}
