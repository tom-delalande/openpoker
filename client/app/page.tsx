'use client';

import { useState, useEffect } from 'react';
import { useGameStore } from '../src/store/gameStore';
import { client } from '../src/lib/api/client';
import { Button } from '../src/components/ui/Button';
import { Input } from '../src/components/ui/Input';
import type { components } from '../src/lib/api/types';

type PlayerInfo = components['schemas']['PlayerInfo'];

function getInitialPlayerName(): string {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('playerName') || '';
  }
  return '';
}

export default function HomePage() {
  const [playerName, setPlayerName] = useState(getInitialPlayerName);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [playerInfo, setPlayerInfo] = useState<PlayerInfo | null>(null);

  const { setAuthToken, setPlayerId, setPlayerName: setStorePlayerName, setTableId, setCurrentView } = useGameStore();

  useEffect(() => {
    const token = localStorage.getItem('authToken');
    if (token) {
      setAuthToken(token);
      client.GET('/game/player', { params: { query: { token } } })
        .then(({ data }) => {
          if (data) {
            setPlayerInfo(data);
            setPlayerId(data.playerId);
            setStorePlayerName(data.name);
            localStorage.setItem('playerName', data.name);
          }
        })
        .catch(console.error);
    }
  }, []);

  const handleLogin = async () => {
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
      } else {
        setPlayerInfo({ playerId: data.playerId, name, stack: 0 });
        setStorePlayerName(name);
      }
    } catch (err) {
      console.error('Login error:', err);
      setError('Failed to login. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = () => {
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
      <div className="min-h-screen bg-gradient-to-b from-[#1a472a] to-[#0f3020] flex items-center justify-center p-4">
        <div className="w-full max-w-sm bg-[#2d5a3d] border-4 border-[#1a3622] rounded-2xl p-6 sm:p-8 shadow-2xl">
          <h2 className="text-2xl sm:text-3xl font-bold text-white mb-2 text-center">Welcome</h2>
          <p className="text-white/70 text-center mb-6">{playerInfo.name}</p>
          
          <div className="bg-[#1a472a] rounded-xl p-4 mb-6 text-center border border-[#1a5c32]">
            <p className="text-gray-400 text-sm mb-1">Your Stack</p>
            <p className="text-yellow-400 text-3xl font-bold">${playerInfo.stack.toFixed(2)}</p>
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

          <Button
            onClick={handleLogout}
            variant="ghost"
            size="md"
            fullWidth
            className="mt-3"
          >
            Logout
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-[#1a472a] to-[#0f3020] flex items-center justify-center p-4">
      <div className="w-full max-w-sm bg-[#2d5a3d] border-4 border-[#1a3622] rounded-2xl p-6 sm:p-8 shadow-2xl">
        <div className="text-center mb-8">
          <h1 className="text-3xl sm:text-4xl font-bold text-white mb-2">OpenPoker</h1>
          <p className="text-white/60">Texas Hold'em</p>
        </div>

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

        <p className="text-white/40 text-xs text-center mt-6">
          No account needed. Just pick a name and play!
        </p>
      </div>
    </div>
  );
}
