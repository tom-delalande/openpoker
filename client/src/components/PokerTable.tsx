'use client';

import { useState, useCallback, useEffect, useMemo } from 'react';
import { useGameStore } from '../store/gameStore';
import { tableSocket } from '../services/tableSocket';
import { processEvents } from '../store/gameEvents';
import { formatAmount } from '../lib/cards';
import { Button } from './ui/Button';
import { SoundToggle } from './ui/SoundToggle';
import { sounds } from '../lib/sounds';
import { Card, PlayerSeat, ActionBar, CommunityCards, PotDisplay } from './game';

interface ActionButton {
  kind: string;
  label: string;
  amount?: number;
  minAmount?: number;
  maxAmount?: number;
}

function getActionButtons(options: import('../lib/api/types').components['schemas']['ActionOptions'][] | null): ActionButton[] {
  if (!options) return [];

  return options.map((opt) => {
    switch (opt.kind) {
      case 'Fold':
        return { kind: 'Fold', label: 'Fold' };
      case 'Check':
        return { kind: 'Check', label: 'Check' };
      case 'Call':
        return { kind: 'Call', label: `Call ${formatAmount(opt.value.amount)}`, amount: opt.value.amount };
      case 'Bet':
        return { kind: 'Bet', label: 'Bet', minAmount: opt.value.minAmount, maxAmount: opt.value.maxAmount };
      case 'Raise':
        return { kind: 'Raise', label: 'Raise', minAmount: opt.value.minAmount, maxAmount: opt.value.maxAmount };
      case 'PostSmallBlind':
        return { kind: 'PostSmallBlind', label: `Post SB`, amount: opt.value.amount };
      case 'PostBigBlind':
        return { kind: 'PostBigBlind', label: `Post BB`, amount: opt.value.amount };
      default:
        return { kind: 'Unknown', label: '?' };
    }
  });
}

const STREET_NAMES: Record<string, string> = {
  preflop: 'Pre-Flop',
  flop: 'Flop',
  turn: 'Turn',
  river: 'River',
  showdown: 'Showdown',
};

export function PokerTable() {
  const {
    playerId,
    players,
    myCards,
    communityCards,
    currentPot,
    dealerButton,
    currentPlayerId,
    actionOptions,
    actionExpiry,
    isConnected,
    error,
    winners,
    currentStreet,
    handId,
    setTableId,
    setPlayerId,
    setCurrentView,
  } = useGameStore();

  const tableId = useMemo(() => useGameStore.getState().tableId, []);
  const displayId = handId || tableId;
  const isMyTurn = currentPlayerId === playerId && actionOptions !== null;

  const handleLeaveTable = useCallback(() => {
    sounds.playMenuNavigate();
    tableSocket.send({ kind: 'StandUp', value: { type: 'StandUp' } });
    tableSocket.disconnect();
    localStorage.removeItem('tableId');
    setTableId(null);
    useGameStore.getState().reset();
    setCurrentView('home');
  }, [setTableId, setCurrentView]);

  const [betAmount, setBetAmount] = useState('');

  useEffect(() => {
    const storedToken = localStorage.getItem('authToken');
    const storedTableId = localStorage.getItem('tableId');
    const storedPlayerId = localStorage.getItem('playerId');

    if (storedToken) {
      useGameStore.getState().setAuthToken(storedToken);
    }
    if (storedTableId) {
      setTableId(storedTableId);
    }
    if (storedPlayerId) {
      const playerId = parseInt(storedPlayerId, 10);
      setPlayerId(playerId);
    }

    if (storedToken && tableId) {
      const unsubscribe = tableSocket.onMessage((events) => {
        processEvents(events);
      });

      tableSocket.connect(tableId, storedToken);

      return () => {
        unsubscribe();
        tableSocket.disconnect();
      };
    }
  }, [tableId, setTableId, setPlayerId]);

  const sendAction = useCallback((action: import('../lib/api/types').components['schemas']['PlayerAction']) => {
    tableSocket.send(action);
  }, []);

  const handleAction = useCallback((button: ActionButton) => {
    sounds.playMenuClick();
    switch (button.kind) {
      case 'Fold':
        sendAction({ kind: 'Fold', value: { type: 'Fold' } });
        break;
      case 'Check':
        sendAction({ kind: 'Check', value: { type: 'Check' } });
        break;
      case 'Call':
        sendAction({ kind: 'Call', value: { type: 'Call', amount: button.amount || 0 } });
        break;
      case 'Bet': {
        const betAmt = parseFloat(betAmount) || button.minAmount || 0;
        sendAction({ kind: 'Bet', value: { type: 'Bet', amount: betAmt } });
        setBetAmount('');
        break;
      }
      case 'Raise': {
        const raiseAmt = parseFloat(betAmount) || button.minAmount || 0;
        sendAction({ kind: 'Raise', value: { type: 'Raise', amount: raiseAmt } });
        setBetAmount('');
        break;
      }
      case 'PostSmallBlind':
        sendAction({ kind: 'PostSmallBlind', value: { type: 'PostSmallBlind', amount: button.amount || 0 } });
        break;
      case 'PostBigBlind':
        sendAction({ kind: 'PostBigBlind', value: { type: 'PostBigBlind', amount: button.amount || 0 } });
        break;
    }
  }, [sendAction, betAmount]);

  const [timeRemaining, setTimeRemaining] = useState(0);
  const [maxTime, setMaxTime] = useState(0);

  useEffect(() => {
    if (!actionExpiry || !isMyTurn) {
      setTimeRemaining(0);
      return;
    }

    const expiryDate = new Date(actionExpiry).getTime();
    const totalMs = expiryDate - Date.now();

    if (totalMs <= 0) {
      setTimeRemaining(0);
      return;
    }

    setMaxTime(totalMs);
    setTimeRemaining(totalMs);

    const interval = setInterval(() => {
      const remaining = expiryDate - Date.now();
      if (remaining <= 0) {
        setTimeRemaining(0);
        clearInterval(interval);
      } else {
        setTimeRemaining(remaining);
      }
    }, 100);

    return () => clearInterval(interval);
  }, [actionExpiry, isMyTurn]);

  const actionButtons = getActionButtons(actionOptions);
  const streetName = currentStreet ? STREET_NAMES[currentStreet] || currentStreet : null;

  const sortedPlayers = useMemo(() => {
    if (players.length === 0) return [];
    const localPlayer = players.find((p) => p.id === playerId);
    const localSeat = localPlayer?.seat ?? 0;

    return [...players].sort((a, b) => {
      const aRelative = (a.seat - localSeat + 10) % 10;
      const bRelative = (b.seat - localSeat + 10) % 10;
      return aRelative - bRelative;
    });
  }, [players, playerId]);

  return (
    <div className="min-h-screen bg-[#0f3020] flex flex-col pb-36">
      <SoundToggle />
      <header className="flex items-center justify-between px-4 py-3 bg-[#0d3d22]/80 backdrop-blur-sm border-b border-[#1a5c32]">
        <Button variant="ghost" size="sm" onClick={handleLeaveTable}>
          ← Leave
        </Button>
        <div className="flex items-center gap-3">
          {displayId && (
            <button
              onClick={() => navigator.clipboard.writeText(displayId)}
              className="text-xs text-white/30 hover:text-white/50 transition-colors"
              title="Click to copy"
            >
              {displayId}
            </button>
          )}
          <div className={`w-3 h-3 rounded-full ${isConnected ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`} />
        </div>
        {error && <span className="text-red-400 text-sm hidden sm:inline">{error}</span>}
      </header>

      <div className="flex-1 flex flex-col items-center justify-center p-4 overflow-hidden">
        <div className="relative w-full max-w-lg">
          <div className="bg-[#35654d] rounded-3xl border-4 border-[#1a5c32] shadow-2xl shadow-black/50 p-4 sm:p-6">
            <div className="absolute inset-0 bg-gradient-to-b from-white/5 to-transparent rounded-3xl" />

            <CommunityCards cards={communityCards} />

            <div className="flex justify-center mt-4">
              <PotDisplay amount={currentPot} />
            </div>

            <div className="mt-6 flex justify-center">
              <div className="flex flex-wrap gap-4 justify-center max-w-md">
                {sortedPlayers.map((player) => (
                  <PlayerSeat
                    key={player.id}
                    name={player.name}
                    stack={player.stack}
                    seat={player.seat}
                    cards={player.id === playerId ? myCards : player.cards}
                    hasFolded={player.hasFolded}
                    currentBet={player.currentBet}
                    isDealer={player.seat === dealerButton}
                    isCurrentPlayer={player.id === currentPlayerId}
                    isLocalPlayer={player.id === playerId}
                    isWinner={winners.includes(player.id)}
                    showCards={player.id === playerId || winners.length > 0}
                  />
                ))}
              </div>
            </div>
          </div>
        </div>

        {!isConnected && (
          <div className="mt-4 text-yellow-400 text-center animate-pulse">
            Connecting to table...
          </div>
        )}

        {players.length <= 1 && isConnected && (
          <div className="mt-4 text-white/70 text-center">
            Waiting for players...
          </div>
        )}
      </div>

      {isMyTurn && actionButtons.length > 0 && (
        <ActionBar
          actions={actionButtons}
          onAction={handleAction}
          betAmount={betAmount}
          onBetAmountChange={setBetAmount}
          timeRemaining={timeRemaining}
          maxTime={maxTime}
        />
      )}
    </div>
  );
}
