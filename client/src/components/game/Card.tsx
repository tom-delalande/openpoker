import { parseCard, isRedSuit } from '../../lib/cards';

interface CardProps {
  cardStr: string;
  hidden?: boolean;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const SUIT_SYMBOLS: Record<string, string> = {
  '♠': '♠',
  '♥': '♥',
  '♦': '♦',
  '♣': '♣',
};

export function Card({ cardStr, hidden = false, size = 'md', className = '' }: CardProps) {
  const card = parseCard(cardStr);

  if (hidden || card.isHidden) {
    return (
      <div className={`
        bg-gradient-to-br from-[#1a5c32] to-[#0d3d22] border-2 border-[#0d3d22] rounded-lg
        flex items-center justify-center shadow-lg
        transition-all duration-200
        ${size === 'sm' ? 'w-10 h-14' : size === 'lg' ? 'w-16 h-24' : 'w-12 h-16 sm:w-14 sm:h-20'}
        ${className}
      `}>
        <div className="w-3/4 h-[90%] bg-[#0d3d22] rounded border border-[#1a5c32] flex items-center justify-center">
          <span className="text-[#2d8a4e] text-opacity-60 font-bold opacity-40">
            {size === 'sm' ? '' : '?'}
          </span>
        </div>
      </div>
    );
  }

  const red = isRedSuit(card.suit);
  const suitSymbol = SUIT_SYMBOLS[card.suit] || card.suit;

  return (
    <div className={`
      bg-gradient-to-br from-white to-gray-100 border-2 border-gray-300 rounded-lg
      flex flex-col items-center justify-center shadow-xl
      relative overflow-hidden
      transition-all duration-200 hover:shadow-2xl hover:-translate-y-1
      ${size === 'sm' ? 'w-10 h-14 text-xs' : size === 'lg' ? 'w-16 h-24 text-lg' : 'w-12 h-16 sm:w-14 sm:h-20 text-base'}
      ${className}
    `}>
      <div className="absolute inset-0 bg-gradient-to-br from-transparent via-white/10 to-transparent" />
      <div className={`absolute top-0.5 left-0.5 font-bold leading-none ${red ? 'text-red-600' : 'text-gray-900'}`}>
        {card.rank}
      </div>
      <span className={`font-bold ${red ? 'text-red-600' : 'text-gray-900'}`}>
        {suitSymbol}
      </span>
      <div className={`absolute bottom-0.5 right-0.5 font-bold leading-none rotate-180 ${red ? 'text-red-600' : 'text-gray-900'}`}>
        {card.rank}
      </div>
    </div>
  );
}
