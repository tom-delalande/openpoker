'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { useGameStore } from '../src/store/gameStore';

function getInitialPlayerName(): string {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('playerName') || '';
  }
  return '';
}

export default function Home() {
  const router = useRouter();
  const [showModal, setShowModal] = useState(false);
  const [playerName, setPlayerName] = useState(getInitialPlayerName);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { setAuthToken, setPlayerName: setStorePlayerName } = useGameStore();

  const handleJoinTable = async () => {
    const name = playerName.trim() || `Player_${Math.floor(Math.random() * 10000)}`;
    setIsLoading(true);
    setError(null);

    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:3001';

      const loginResponse = await fetch(`${apiUrl}/auth/login/${encodeURIComponent(name)}`, {
        method: 'POST',
      });
      console.log('Login response:', loginResponse);
      
      if (!loginResponse.ok) {
        throw new Error('Login failed');
      }
      
      const token = await loginResponse.text();
      console.log('Token:', token);
      
      if (!token || typeof token !== 'string') {
        throw new Error('Invalid token received');
      }

      setAuthToken(token);
      setStorePlayerName(name);
      localStorage.setItem('playerName', name);
      localStorage.setItem('authToken', token);

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
      router.push(`/table?tableId=${tableId}`);
    } catch (err) {
      console.error('Join error:', err);
      setError('Failed to join table. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex flex-1 items-center justify-center bg-[#1a472a]">
      <button
        onClick={() => setShowModal(true)}
        className="px-8 py-4 text-xl font-bold text-white bg-[#2d5a3d] rounded-lg border-4 border-[#1a3622] hover:bg-[#3d7a4d] active:translate-y-0.5 transition-colors"
      >
        Join Table
      </button>

      {showModal && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-[#2d5a3d] border-4 border-[#1a3622] rounded-xl p-8 shadow-2xl min-w-[320px]">
            <h2 className="text-2xl font-bold text-white mb-6 text-center">Enter Your Name</h2>
            <input
              type="text"
              value={playerName}
              onChange={(e) => setPlayerName(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && !isLoading && handleJoinTable()}
              placeholder="Your name"
              maxLength={20}
              autoFocus
              disabled={isLoading}
              className="w-full px-4 py-3 text-lg bg-[#1a472a] border-2 border-[#1a3622] rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-[#4d9a5d] transition-colors mb-4 disabled:opacity-50"
            />
            {error && (
              <div className="text-red-400 text-sm mb-4 text-center">{error}</div>
            )}
            <div className="flex gap-4">
              <button
                onClick={() => {
                  setShowModal(false);
                  setError(null);
                }}
                disabled={isLoading}
                className="flex-1 px-6 py-3 text-lg font-bold text-white bg-[#1a3622] rounded-lg hover:bg-[#0f2415] transition-colors disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                onClick={handleJoinTable}
                disabled={isLoading}
                className="flex-1 px-6 py-3 text-lg font-bold text-white bg-[#4d9a5d] rounded-lg hover:bg-[#5dba6d] transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
              >
                {isLoading ? (
                  <>
                    <span className="animate-spin">⟳</span>
                    Joining...
                  </>
                ) : (
                  'Join'
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
