'use client';

import { useState, useEffect } from 'react';
import { useGameStore } from '../src/store/gameStore';
import { client } from '../src/lib/api/client';
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
      const { data, error } = await client.POST('/auth/login', { 
        params: { query: { name } }
      });

      if (error) {
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

      console.log('Sending join request to:', `${apiUrl}/game/join?token=${token}`);
      const joinFetchResponse = await fetch(`${apiUrl}/game/join?token=${encodeURIComponent(token)}`, {
        method: 'POST',
      });
      console.log('Join fetch response status:', joinFetchResponse.status);
      
      if (!joinFetchResponse.ok) {
        throw new Error(`Failed to join game: ${joinFetchResponse.status}`);
      }
      
      const tableId = await joinFetchResponse.text();
      console.log('Table ID:', tableId);
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
      <div className="flex flex-1 items-center justify-center bg-[#1a472a]">
        <div className="bg-[#2d5a3d] border-4 border-[#1a3622] rounded-xl p-8 shadow-2xl min-w-[320px] text-center">
          <h2 className="text-2xl font-bold text-white mb-4">Welcome</h2>
          <div className="text-white text-lg mb-2">{playerInfo.name}</div>
          <div className="text-yellow-400 text-xl font-bold mb-6">Stack: ${playerInfo.stack.toFixed(2)}</div>
          <button
            onClick={handleJoinTable}
            disabled={isLoading}
            className="w-full px-6 py-3 text-lg font-bold text-white bg-[#4d9a5d] rounded-lg hover:bg-[#5dba6d] transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
          >
            {isLoading ? (
              <>
                <span className="animate-spin">⟳</span>
                Joining...
              </>
            ) : (
              'Join Table'
            )}
          </button>
          <button
            onClick={handleLogout}
            disabled={isLoading}
            className="w-full mt-4 px-6 py-3 text-lg font-bold text-white bg-[#1a3622] rounded-lg hover:bg-[#0f2415] transition-colors disabled:opacity-50"
          >
            Logout
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-1 items-center justify-center bg-[#1a472a]">
      <div className="bg-[#2d5a3d] border-4 border-[#1a3622] rounded-xl p-8 shadow-2xl min-w-[320px]">
        <h2 className="text-2xl font-bold text-white mb-6 text-center">Enter Your Name</h2>
        <input
          type="text"
          value={playerName}
          onChange={(e) => setPlayerName(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && !isLoading && handleLogin()}
          placeholder="Your name (default: Player)"
          maxLength={20}
          autoFocus
          disabled={isLoading}
          className="w-full px-4 py-3 text-lg bg-[#1a472a] border-2 border-[#1a3622] rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-[#4d9a5d] transition-colors mb-4 disabled:opacity-50"
        />
        {error && (
          <div className="text-red-400 text-sm mb-4 text-center">{error}</div>
        )}
        <button
          onClick={handleLogin}
          disabled={isLoading}
          className="w-full px-6 py-3 text-lg font-bold text-white bg-[#4d9a5d] rounded-lg hover:bg-[#5dba6d] transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
        >
          {isLoading ? (
            <>
              <span className="animate-spin">⟳</span>
              Logging in...
            </>
          ) : (
            'Login'
          )}
        </button>
      </div>
    </div>
  );
}