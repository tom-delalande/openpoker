import { useState, useCallback } from 'react';
import { formatAmount } from '../../lib/cards';
import { Button } from '../ui/Button';

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

  const currentMin = minBet || minRaise;
  const currentMax = maxBet || maxRaise;
  const currentAmount = parseFloat(betAmount) || currentMin;

  const handlePresetClick = useCallback((percentage: number) => {
    const amount = Math.round((currentMax * percentage) / 100);
    const clamped = Math.max(currentMin, Math.min(currentMax, amount));
    onBetAmountChange(clamped.toString());
  }, [currentMax, currentMin, onBetAmountChange]);

  const handleAllIn = useCallback(() => {
    onBetAmountChange(currentMax.toString());
  }, [currentMax, onBetAmountChange]);

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
          <div className="space-y-3">
            <div className="flex items-center justify-center gap-2 text-yellow-400 text-xl font-bold">
              <ChipIcon className="w-6 h-6" />
              {currentAmount.toFixed(0)}
            </div>

            <div className="flex gap-2 justify-center">
              <button
                onClick={() => handlePresetClick(25)}
                className="px-3 py-1.5 bg-[#1a3622] hover:bg-[#2d5a3d] text-white text-sm rounded-lg transition-colors border border-[#1a5c32]"
              >
                25%
              </button>
              <button
                onClick={() => handlePresetClick(50)}
                className="px-3 py-1.5 bg-[#1a3622] hover:bg-[#2d5a3d] text-white text-sm rounded-lg transition-colors border border-[#1a5c32]"
              >
                50%
              </button>
              <button
                onClick={() => handlePresetClick(75)}
                className="px-3 py-1.5 bg-[#1a3622] hover:bg-[#2d5a3d] text-white text-sm rounded-lg transition-colors border border-[#1a5c32]"
              >
                75%
              </button>
              <button
                onClick={handleAllIn}
                className="px-3 py-1.5 bg-yellow-600/30 hover:bg-yellow-600/50 text-yellow-400 text-sm rounded-lg transition-colors border border-yellow-600/50"
              >
                All-In
              </button>
            </div>

            <div className="px-2">
              <input
                type="range"
                min={currentMin}
                max={currentMax}
                value={currentAmount}
                onChange={(e) => onBetAmountChange(e.target.value)}
                className="w-full"
              />
              <div className="flex justify-between text-gray-500 text-xs mt-1 px-1">
                <span>{formatAmount(currentMin)}</span>
                <span>{formatAmount(currentMax)}</span>
              </div>
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
