const RANK_MAP: Record<string, string> = {
  '1': 'A',
  '2': '2',
  '3': '3',
  '4': '4',
  '5': '5',
  '6': '6',
  '7': '7',
  '8': '8',
  '9': '9',
  '10': '10',
  '11': 'J',
  '12': 'Q',
  '13': 'K',
};

const SUIT_MAP: Record<string, string> = {
  'h': '♥',
  'd': '♦',
  'c': '♣',
  's': '♠',
};

export interface ParsedCard {
  suit: string;
  rank: string;
  isHidden: boolean;
}

export function parseCard(cardStr: string): ParsedCard {
  if (cardStr === 'XX' || cardStr === 'xx') {
    return { suit: '', rank: '', isHidden: true };
  }

  if (cardStr.length < 2) {
    return { suit: '', rank: '?', isHidden: false };
  }

  const lastChar = cardStr[cardStr.length - 1].toLowerCase();
  const suit = SUIT_MAP[lastChar] || lastChar;
  const rankStr = cardStr.slice(0, -1);
  const rank = RANK_MAP[rankStr] || rankStr;

  return { suit, rank, isHidden: false };
}

export function isRedSuit(suit: string): boolean {
  return suit === '♥' || suit === '♦';
}

export function formatAmount(amount: number): string {
  if (amount >= 1000000) {
    return `${(amount / 1000000).toFixed(1)}M`;
  }
  if (amount >= 1000) {
    return `${(amount / 1000).toFixed(1)}K`;
  }
  return `${amount.toFixed(0)}`;
}
