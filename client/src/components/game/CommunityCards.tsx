import { formatAmount } from '../../lib/cards';
import { Card } from './Card';

interface CommunityCardsProps {
  cards: string[];
  street: string | null;
  className?: string;
}

export function CommunityCards({ cards, street, className = '' }: CommunityCardsProps) {
  const placeholderCount = 5 - cards.length;

  return (
    <div className={`flex flex-col items-center gap-2 ${className}`}>
      {street && (
        <div className="px-3 py-1 bg-[#0d3d22] rounded-full border border-[#1a5c32]">
          <span className="text-xs font-medium text-gray-400 uppercase tracking-wider">
            {street}
          </span>
        </div>
      )}
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
      <span className="text-gray-400 text-sm font-medium">POT</span>
      <span className="text-yellow-400 font-bold text-lg">
        {formatAmount(amount)}
      </span>
    </div>
  );
}
