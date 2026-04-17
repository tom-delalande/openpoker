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

function ChipIcon({ className = '' }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 29"
      fill="none"
      className={className}
      style={{ width: '1em', height: '1em' }}
    >
      <circle cx="12" cy="20" r="7" fill="#eab308" stroke="#ca8a04" strokeWidth="1" />
      <circle cx="12" cy="20" r="4" fill="none" stroke="#ca8a04" strokeWidth="0.75" />
      <circle cx="12" cy="16" r="7" fill="#fcd34d" stroke="#ca8a04" strokeWidth="1" />
      <circle cx="12" cy="16" r="4" fill="none" stroke="#ca8a04" strokeWidth="0.75" />
      <circle cx="12" cy="12" r="7" fill="#fef08a" stroke="#ca8a04" strokeWidth="1" />
      <circle cx="12" cy="12" r="4" fill="none" stroke="#ca8a04" strokeWidth="0.75" />
    </svg>
  );
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
      flex flex-col items-center transition-all duration-300 relative
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
          {name}
        </div>
        {currentBet > 0 && (
          <div className="text-blue-300 text-xs font-medium flex items-center justify-center gap-0.5">
            <ChipIcon className="w-3 h-3" />
            {formatAmount(currentBet)}
          </div>
        )}
        <div className={`font-bold flex items-center justify-center gap-0.5 ${stack < 100 ? 'text-red-400' : 'text-yellow-400'}`}>
          <ChipIcon className="w-3.5 h-3.5" />
          {formatAmount(stack)}
        </div>
        {hasFolded && (
          <div className="text-red-400 text-xs font-medium mt-0.5">Folded</div>
        )}
      </div>
      {isLocalPlayer && (
        <div className="h-0.5 w-16 rounded-full bg-green-500/60 mt-1 shadow-sm" />
      )}
    </div>
  );
}