'use client';

import { useState, useEffect } from 'react';
import { useGameStore } from '../src/store/gameStore';
import { client } from '../src/lib/api/client';
import { sounds } from '../src/lib/sounds';
import { formatAmount } from '../src/lib/cards';
import { Button } from '../src/components/ui/Button';
import { Input } from '../src/components/ui/Input';
import { SoundToggle } from '../src/components/ui/SoundToggle';
import type { components } from '../src/lib/api/types';

type PlayerInfo = components['schemas']['PlayerInfo'];

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

function getInitialPlayerName(): string {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('playerName') || '';
  }
  return '';
}

export default function HomePage() {
  const [playerName, setPlayerName] = useState(getInitialPlayerName);
  const [isLoading, setIsLoading] = useState(false);
  const [isFetchingStack, setIsFetchingStack] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [playerInfo, setPlayerInfo] = useState<PlayerInfo | null>(null);

  const { setAuthToken, setPlayerId, setPlayerName: setStorePlayerName, setTableId, setCurrentView, currentView, setStack } = useGameStore();

  useEffect(() => {
    if (currentView !== 'home') return;

    setIsFetchingStack(true);
    const token = localStorage.getItem('authToken');
    if (token) {
      setAuthToken(token);
      client.GET('/game/player', { params: { query: { token } } })
        .then(({ data }) => {
          if (data) {
            setPlayerInfo(data);
            setPlayerId(data.playerId);
            setStorePlayerName(data.name);
            setStack(data.stack);
            localStorage.setItem('playerName', data.name);
          }
        })
        .catch(console.error)
        .finally(() => setIsFetchingStack(false));
    } else {
      setIsFetchingStack(false);
    }
  }, [currentView, setAuthToken, setPlayerId, setStorePlayerName, setStack]);

  const handleLogin = async () => {
    sounds.playMenuClick();
    const name = playerName.trim() || 'Player';
    setIsLoading(true);
    setError(null);

    try {
      const { data, error: apiError } = await client.POST('/auth/login', { 
        params: { query: { name } }
      });

      if (apiError) {
        throw new Error('Login failed');
      }

      if (!data?.token || !data?.playerId) {
        throw new Error('Invalid auth response received');
      }

      setAuthToken(data.token);
      setPlayerId(data.playerId);
      localStorage.setItem('playerId', data.playerId.toString());
      localStorage.setItem('playerName', name);
      localStorage.setItem('authToken', data.token);

      const { data: playerData } = await client.GET('/game/player', { params: { query: { token: data.token } } });
      if (playerData) {
        setPlayerInfo(playerData);
        setStorePlayerName(playerData.name);
        setStack(playerData.stack);
      } else {
        setPlayerInfo({ playerId: data.playerId, name, stack: 0 });
        setStorePlayerName(name);
        setStack(0);
      }
    } catch (err) {
      console.error('Login error:', err);
      setError('Failed to login. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = () => {
    sounds.playMenuClick();
    localStorage.removeItem('authToken');
    localStorage.removeItem('playerId');
    localStorage.removeItem('playerName');
    localStorage.removeItem('tableId');
    setAuthToken(null);
    setPlayerId(null);
    setStorePlayerName('');
    setTableId(null);
    setPlayerInfo(null);
    setCurrentView('home');
  };

  const handleJoinTable = async () => {
    sounds.playMenuNavigate();
    const token = localStorage.getItem('authToken');
    if (!token) return;

    setIsLoading(true);
    setError(null);

    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:3001';

      const joinFetchResponse = await fetch(`${apiUrl}/game/join?token=${encodeURIComponent(token)}`, {
        method: 'POST',
      });

      if (!joinFetchResponse.ok) {
        throw new Error(`Failed to join game: ${joinFetchResponse.status}`);
      }

      const tableId = await joinFetchResponse.text();
      localStorage.setItem('tableId', tableId);
      setTableId(tableId);
      setCurrentView('table');
    } catch (err) {
      console.error('Join error:', err);
      setError('Failed to join table. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const hasToken = !!localStorage.getItem('authToken');

  if (hasToken && playerInfo) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-[#1a472a] to-[#0f3020] flex flex-col items-center justify-center p-4">
        <SoundToggle />
        <h1 style={{ fontFamily: 'var(--font-fredoka)' }} className="text-5xl sm:text-7xl text-white mb-4">TinyPoker</h1>

        <div className="text-center mb-6">
          <p className="text-white/60 text-sm">Playing as</p>
          <p className="text-white text-2xl font-bold">{playerInfo.name}</p>
        </div>

        <div className="w-full max-w-sm bg-[#2d5a3d] border-4 border-[#1a3622] rounded-2xl p-6 sm:p-8 shadow-2xl">
          <div className="bg-[#1a472a] rounded-xl p-4 mb-6 text-center border border-[#1a5c32]">
            {isFetchingStack ? (
              <div className="text-white/60 text-xl py-2 flex items-center justify-center gap-2">
                <span className="animate-spin">⟳</span>
                Loading...
              </div>
            ) : (
              <p className="text-yellow-400 text-3xl font-bold flex items-center justify-center gap-2">
                <ChipIcon className="w-7 h-7" />
                {formatAmount(playerInfo.stack)}
              </p>
            )}
          </div>

          {error && (
            <div className="bg-red-500/20 border border-red-500/50 rounded-lg p-3 mb-4 text-red-300 text-sm text-center">
              {error}
            </div>
          )}

          <Button
            onClick={handleJoinTable}
            disabled={isLoading}
            size="lg"
            fullWidth
          >
            {isLoading ? (
              <>
                <span className="animate-spin">⟳</span>
                Joining...
              </>
            ) : (
              'Join Table'
            )}
          </Button>

          <div className="mt-4 flex justify-center">
            <button
              onClick={handleLogout}
              className="text-white/40 hover:text-white/70 text-sm transition-colors"
            >
              Logout
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-[#1a472a] to-[#0f3020] flex flex-col items-center justify-center p-4">
      <h1 style={{ fontFamily: 'var(--font-fredoka)' }} className="text-5xl sm:text-7xl text-white mb-8">TinyPoker</h1>

      <div className="w-full max-w-sm bg-[#2d5a3d] border-4 border-[#1a3622] rounded-2xl p-6 sm:p-8 shadow-2xl">
        <div className="space-y-4">
          <div>
            <label className="block text-white/80 text-sm mb-2 font-medium">
              Your Name
            </label>
            <Input
              value={playerName}
              onChange={setPlayerName}
              placeholder="Enter your name"
              autoFocus
              disabled={isLoading}
              onKeyDown={(e) => e.key === 'Enter' && !isLoading && handleLogin()}
            />
          </div>

          {error && (
            <div className="bg-red-500/20 border border-red-500/50 rounded-lg p-3 text-red-300 text-sm text-center">
              {error}
            </div>
          )}

          <Button
            onClick={handleLogin}
            disabled={isLoading}
            size="lg"
            fullWidth
          >
            {isLoading ? (
              <>
                <span className="animate-spin">⟳</span>
                Logging in...
              </>
            ) : (
              'Play Now'
            )}
          </Button>
        </div>
      </div>
    </div>
  );
}
