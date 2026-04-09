import { create } from 'zustand';

export interface Player {
  id: number;
  name: string;
  stack: number;
  seat: number;
  cards?: string[];
  hasActed?: boolean;
  hasFolded?: boolean;
}

export interface GameState {
  authToken: string | null;
  playerId: number | null;
  playerName: string;
  tableId: string | null;
  players: Player[];
  communityCards: string[];
  myCards: string[];
  currentPot: number;
  dealerButton: number;
  currentPlayerId: number | null;
  actionOptions: import('../lib/api/types').components['schemas']['ActionOptions'][] | null;
  actionExpiry: string | null;
  isConnected: boolean;
  isLoading: boolean;
  error: string | null;
  currentStreet: string | null;
}

export interface GameActions {
  setAuthToken: (token: string | null) => void;
  setPlayerId: (id: number | null) => void;
  setPlayerName: (name: string) => void;
  setTableId: (tableId: string | null) => void;
  setConnected: (connected: boolean) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  addPlayer: (player: Player) => void;
  removePlayer: (playerId: number) => void;
  updatePlayer: (playerId: number, updates: Partial<Player>) => void;
  setCommunityCards: (cards: string[]) => void;
  setMyCards: (cards: string[]) => void;
  setCurrentPot: (pot: number) => void;
  setDealerButton: (seat: number) => void;
  setCurrentPlayerId: (playerId: number | null) => void;
  setActionOptions: (options: import('../lib/api/types').components['schemas']['ActionOptions'][] | null) => void;
  setActionExpiry: (expiry: string | null) => void;
  setCurrentStreet: (street: string | null) => void;
  reset: () => void;
}

const initialState: GameState = {
  authToken: null,
  playerId: null,
  playerName: '',
  tableId: null,
  players: [],
  communityCards: [],
  myCards: [],
  currentPot: 0,
  dealerButton: 0,
  currentPlayerId: null,
  actionOptions: null,
  actionExpiry: null,
  isConnected: false,
  isLoading: false,
  error: null,
  currentStreet: null,
};

export const useGameStore = create<GameState & GameActions>((set) => ({
  ...initialState,

  setAuthToken: (token) => set({ authToken: token }),
  setPlayerId: (id) => set({ playerId: id }),
  setPlayerName: (name) => set({ playerName: name }),
  setTableId: (tableId) => set({ tableId }),
  setConnected: (connected) => set({ isConnected: connected }),
  setLoading: (loading) => set({ isLoading: loading }),
  setError: (error) => set({ error }),

  addPlayer: (player) => set((state) => {
    const existing = state.players.find((p) => p.id === player.id);
    if (existing) {
      return {
        players: state.players.map((p) => p.id === player.id ? { ...p, ...player } : p),
      };
    }
    return { players: [...state.players, player] };
  }),

  removePlayer: (playerId) => set((state) => ({
    players: state.players.filter((p) => p.id !== playerId),
  })),

  updatePlayer: (playerId, updates) => set((state) => ({
    players: state.players.map((p) => p.id === playerId ? { ...p, ...updates } : p),
  })),

  setCommunityCards: (cards) => set({ communityCards: cards }),
  setMyCards: (cards) => set({ myCards: cards }),
  setCurrentPot: (pot) => set({ currentPot: pot }),
  setDealerButton: (seat) => set({ dealerButton: seat }),
  setCurrentPlayerId: (playerId) => set({ currentPlayerId: playerId }),
  setActionOptions: (options) => set({ actionOptions: options }),
  setActionExpiry: (expiry) => set({ actionExpiry: expiry }),
  setCurrentStreet: (street) => set({ currentStreet: street }),

  reset: () => set(initialState),
}));
