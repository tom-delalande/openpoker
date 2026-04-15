import { formatAmount } from '../../lib/cards';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';

interface ActionButton {
  kind: string;
  label: string;
  amount?: number;
  minAmount?: number;
  maxAmount?: number;
}

interface ActionBarProps {
  actions: ActionButton[];
  onAction: (action: ActionButton) => void;
  betAmount: string;
  onBetAmountChange: (amount: string) => void;
  timeRemaining: number;
  maxTime: number;
}

export function ActionBar({
  actions,
  onAction,
  betAmount,
  onBetAmountChange,
  timeRemaining,
  maxTime,
}: ActionBarProps) {
  const timerPercent = maxTime > 0 ? (timeRemaining / maxTime) * 100 : 0;

  const getActionVariant = (kind: string): 'primary' | 'secondary' | 'danger' => {
    switch (kind) {
      case 'Fold':
        return 'danger';
      case 'Check':
      case 'Call':
        return 'primary';
      default:
        return 'secondary';
    }
  };

  const needsBetInput = actions.some((a) => a.kind === 'Bet' || a.kind === 'Raise');
  const minBet = actions.find((a) => a.kind === 'Bet')?.minAmount || 0;
  const maxBet = actions.find((a) => a.kind === 'Bet')?.maxAmount || 0;
  const minRaise = actions.find((a) => a.kind === 'Raise')?.minAmount || 0;
  const maxRaise = actions.find((a) => a.kind === 'Raise')?.maxAmount || 0;

  return (
    <div className="fixed bottom-0 left-0 right-0 bg-[#0d3d22]/95 backdrop-blur-md border-t-2 border-[#1a5c32] p-4 safe-area-bottom">
      <div className="max-w-lg mx-auto space-y-3">
        <div className="flex justify-center">
          <div className={`w-32 h-2 bg-[#1a3622] rounded-full overflow-hidden border border-[#2d5a3d] ${timerPercent < 20 ? 'shadow-red-500/30' : timerPercent < 50 ? 'shadow-yellow-500/30' : 'shadow-green-500/30'}`}>
            <div
              className={`h-full transition-all duration-100 rounded-full ${
                timerPercent < 20 ? 'bg-red-500' : timerPercent < 50 ? 'bg-yellow-500' : 'bg-green-500'
              }`}
              style={{ width: `${Math.max(0, timerPercent)}%` }}
            />
          </div>
        </div>

        {needsBetInput && (
          <div className="flex items-center gap-2 justify-center">
            <Input
              type="number"
              value={betAmount}
              onChange={onBetAmountChange}
              placeholder={`${minBet || minRaise} - ${maxBet || maxRaise}`}
              min={minBet || minRaise}
              max={maxBet || maxRaise}
              className="w-32 text-center"
            />
            <div className="text-gray-400 text-sm">
              {minBet ? `SB: ${formatAmount(minBet)}` : `RB: ${formatAmount(minRaise)}`}
            </div>
          </div>
        )}

        <div className="flex flex-wrap gap-2 justify-center">
          {actions.map((action, i) => (
            <Button
              key={i}
              variant={getActionVariant(action.kind)}
              size="lg"
              onClick={() => onAction(action)}
              className="min-w-[90px]"
            >
              {action.label}
            </Button>
          ))}
        </div>
      </div>
    </div>
  );
}
