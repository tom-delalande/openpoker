'use client';

import { useEffect, useSyncExternalStore } from 'react';
import { useGameStore } from '../src/store/gameStore';
import HomePage from './page';
import TablePage from './table/page';

function useHydrated() {
  return useSyncExternalStore(
    () => () => {},
    () => true,
    () => false,
  );
}

export default function App() {
  const { currentView, setCurrentView, setAuthToken, setTableId } = useGameStore();
  const hydrated = useHydrated();

  useEffect(() => {
    const storedToken = localStorage.getItem('authToken');
    const storedTableId = localStorage.getItem('tableId');
    
    if (storedToken) {
      setAuthToken(storedToken);
    }
    if (storedTableId) {
      setTableId(storedTableId);
      setCurrentView('table');
    }

    const handlePopState = () => {
      const hasTableId = !!localStorage.getItem('tableId');
      setCurrentView(hasTableId ? 'table' : 'home');
    };

    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, [setAuthToken, setTableId, setCurrentView]);

  if (!hydrated) {
    return null;
  }

  return currentView === 'table' ? <TablePage /> : <HomePage key={currentView} />;
}
