'use client';

import { useEffect, useState, useCallback, useMemo } from 'react';
import { useGameStore } from '../../src/store/gameStore';
import { tableSocket } from '../../src/services/tableSocket';
import { processEvents } from '../../src/store/gameEvents';
import { parseCard, isRedSuit, formatAmount } from '../../src/lib/cards';
import type { components } from '../../src/lib/api/types';

type ActionOptions = components['schemas']['ActionOptions'];
type PlayerAction = components['schemas']['PlayerAction'];

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

interface ActionButton {
  kind: string;
  label: string;
  amount?: number;
  minAmount?: number;
  maxAmount?: number;
}

function getActionButtons(options: ActionOptions[] | null): ActionButton[] {
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
        return { kind: 'PostSmallBlind', label: `Post SB ${formatAmount(opt.value.amount)}`, amount: opt.value.amount };
      case 'PostBigBlind':
        return { kind: 'PostBigBlind', label: `Post BB ${formatAmount(opt.value.amount)}`, amount: opt.value.amount };
      default:
        return { kind: 'Unknown', label: '?' };
    }
  });
}

function Card({ cardStr, hidden }: { cardStr: string; hidden?: boolean }) {
  const card = parseCard(cardStr);

  if (hidden || card.isHidden) {
    return (
      <div className="w-12 h-16 sm:w-14 sm:h-20 bg-[#1a5c32] border-2 border-[#0d3d22] rounded flex items-center justify-center">
        <span className="text-[#2d8a4e] text-lg">?</span>
      </div>
    );
  }

  const red = isRedSuit(card.suit);

  return (
    <div className={`w-12 h-16 sm:w-14 sm:h-20 bg-white border-2 border-gray-300 rounded flex flex-col items-center justify-center ${red ? 'text-red-600' : 'text-gray-900'}`}>
      <span className="text-sm font-bold">{card.rank}</span>
      <span className="text-lg">{card.suit}</span>
    </div>
  );
}

function PlayerSeat({ 
  player, 
  isDealer, 
  isCurrentPlayer,
  localPlayerSeat,
}: { 
  player: { id: number; name: string; stack: number; seat: number; cards?: string[]; hasFolded?: boolean };
  isDealer: boolean;
  isCurrentPlayer: boolean;
  localPlayerSeat: number;
}) {
  const relativeSeat = (player.seat - localPlayerSeat + SEAT_POSITIONS.length) % SEAT_POSITIONS.length;
  const seatIndex = (relativeSeat + 4) % SEAT_POSITIONS.length;
  const position = SEAT_POSITIONS[seatIndex];

  return (
    <div
      className={`absolute flex flex-col items-center transition-all ${isCurrentPlayer ? 'scale-110' : ''}`}
      style={{ left: `${position.x}%`, top: `${position.y}%`, transform: 'translate(-50%, -50%)' }}
    >
      {isDealer && (
        <div className="absolute -top-6 w-6 h-6 bg-white rounded-full border-2 border-[#1a5c32] flex items-center justify-center text-xs font-bold text-[#1a5c32] shadow-md">
          D
        </div>
      )}
      {isCurrentPlayer && (
        <div className="absolute -top-6 w-6 h-6 bg-yellow-400 rounded-full border-2 border-[#1a5c32] flex items-center justify-center text-xs font-bold text-[#1a5c32] shadow-md animate-pulse">
          !
        </div>
      )}
      <div className="flex gap-1 mb-1">
        <Card cardStr={player.cards?.[0] || 'XX'} hidden={player.hasFolded} />
        <Card cardStr={player.cards?.[1] || 'XX'} hidden={player.hasFolded} />
      </div>
      <div className={`bg-[#0d3d22] text-white px-3 py-1 rounded text-sm ${isCurrentPlayer ? 'ring-2 ring-yellow-400' : ''}`}>
        <div className="font-bold">{player.name}</div>
        <div className="text-yellow-400">{formatAmount(player.stack)}</div>
        {player.hasFolded && <div className="text-red-400 text-xs">Folded</div>}
      </div>
    </div>
  );
}

function TableContent() {
  const {
    playerId,
    players,
    communityCards,
    currentPot,
    dealerButton,
    currentPlayerId,
    actionOptions,
    isConnected,
    error,
    setTableId,
    setPlayerId,
    setCurrentView,
  } = useGameStore();

  const tableId = useMemo(() => useGameStore.getState().tableId, []);
  const isMyTurn = currentPlayerId === playerId && actionOptions !== null;

  const handleLeaveTable = useCallback(() => {
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
      setPlayerId(parseInt(storedPlayerId, 10));
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

  const sendAction = useCallback((action: PlayerAction) => {
    tableSocket.send(action);
  }, []);

  const handleAction = useCallback((button: ActionButton) => {
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

  const actionButtons = getActionButtons(actionOptions);
  const localPlayer = players.find((p) => p.id === playerId);
  const localPlayerSeat = localPlayer?.seat ?? 0;

  return (
    <div className="min-h-screen bg-[#0f3020] flex flex-col items-center justify-center p-4">
      <div className="mb-4 flex items-center gap-4">
        <button
          onClick={handleLeaveTable}
          className="text-white text-sm hover:text-red-400 transition-colors"
        >
          ← Leave Table
        </button>
        <div className={`w-3 h-3 rounded-full ${isConnected ? 'bg-green-500' : 'bg-red-500'}`} />
        <span className="text-white text-sm">
          {isConnected ? 'Connected' : 'Disconnected'}
        </span>
        {error && <span className="text-red-400 text-sm">{error}</span>}
      </div>

      <div className="relative w-full max-w-3xl aspect-[4/3]">
        <div className="absolute inset-0 bg-[#35654d] rounded-full border-8 border-[#1a5c32] shadow-2xl">
          <div className="absolute inset-4 bg-[#2d5a3d] rounded-full border-4 border-[#1a472a]">
            <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 flex flex-col items-center gap-4">
              <div className="flex gap-2">
                {communityCards.length === 0 ? (
                  <>
                    <Card cardStr="XX" hidden />
                    <Card cardStr="XX" hidden />
                    <Card cardStr="XX" hidden />
                    <Card cardStr="XX" hidden />
                    <Card cardStr="XX" hidden />
                  </>
                ) : (
                  communityCards.map((card, i) => (
                    <Card key={i} cardStr={card} />
                  ))
                )}
              </div>
              <div className="bg-[#0d3d22] text-white px-6 py-2 rounded-full text-xl font-bold border-4 border-[#1a3622]">
                POT: {formatAmount(currentPot)}
              </div>
            </div>

            {players.map((player) => (
              <PlayerSeat
                key={player.id}
                player={player}
                isDealer={player.seat === dealerButton}
                isCurrentPlayer={player.id === currentPlayerId}
                localPlayerSeat={localPlayerSeat}
              />
            ))}
          </div>
        </div>
      </div>

      {isMyTurn && actionButtons.length > 0 && (
        <div className="flex flex-col items-center gap-2 mt-4">
          <div className="flex gap-2 flex-wrap justify-center max-w-lg">
            {actionButtons.map((button, i) => (
              <div key={i} className="flex flex-col">
                <button
                  onClick={() => handleAction(button)}
                  className="bg-[#1e40af] hover:bg-[#3b82f6] text-white font-bold py-3 px-4 rounded-lg border-2 border-[#1e3a8a] shadow-lg transition-colors min-w-[80px]"
                >
                  <div className="text-sm">{button.label}</div>
                </button>
              </div>
            ))}
          </div>
          {(actionButtons.some(b => b.kind === 'Bet') || actionButtons.some(b => b.kind === 'Raise')) && (
            <div className="flex items-center gap-2 mt-2">
              <input
                type="number"
                value={betAmount}
                onChange={(e) => setBetAmount(e.target.value)}
                placeholder="Amount"
                className="px-3 py-2 bg-[#1a3622] border-2 border-[#1a5c32] rounded text-white w-32"
              />
            </div>
          )}
        </div>
      )}

      {!isConnected && (
        <div className="mt-4 text-yellow-400 text-center">
          Connecting to table...
        </div>
      )}

      {players.length === 0 && isConnected && (
        <div className="mt-4 text-white text-center">
          Waiting for players...
        </div>
      )}
    </div>
  );
}

export default function TablePage() {
  return <TableContent />;
}
