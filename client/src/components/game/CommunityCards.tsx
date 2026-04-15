import { formatAmount } from '../../lib/cards';
import { Card } from './Card';

interface CommunityCardsProps {
  cards: string[];
  className?: string;
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

export function CommunityCards({ cards, className = '' }: CommunityCardsProps) {
  const placeholderCount = 5 - cards.length;

  return (
    <div className={`flex flex-col items-center gap-2 ${className}`}>
      <div className="flex gap-1 sm:gap-2">
        {cards.map((card, i) => (
          <Card key={i} cardStr={card} size="md" />
        ))}
        {Array.from({ length: placeholderCount }).map((_, i) => (
          <Card key={`empty-${i}`} cardStr="XX" hidden size="md" />
        ))}
      </div>
    </div>
  );
}

interface PotDisplayProps {
  amount: number;
  className?: string;
}

export function PotDisplay({ amount, className = '' }: PotDisplayProps) {
  if (amount === 0) return null;

  return (
    <div className={`
      px-4 py-2 bg-[#0d3d22] rounded-full 
      border-2 border-yellow-400/50 shadow-lg
      flex items-center gap-2
      ${className}
    `}>
      <span className="text-yellow-400 font-bold text-lg flex items-center gap-1">
        <ChipIcon className="w-5 h-5" />
        {formatAmount(amount)}
      </span>
    </div>
  );
}
