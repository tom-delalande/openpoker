import type { components } from '../lib/api/types';
import { useGameStore } from '../store/gameStore';

type PlayerAction = components['schemas']['PlayerAction'];
type HandEvent = components['schemas']['HandEvent'];

type MessageHandler = (events: HandEvent[]) => void;

class TableSocket {
  private ws: WebSocket | null = null;
  private messageHandlers: Set<MessageHandler> = new Set();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000;
  private tableId: string | null = null;
  private token: string | null = null;
  private isDisconnecting = false;

  connect(tableId: string, token: string): void {
    this.tableId = tableId;
    this.token = token;
    this.isDisconnecting = false;

    console.log(`[TableSocket] Connecting to table ${tableId}`);

    const wsUrl = `${process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:3001'}/table/ws/table/${tableId}/token/${token}`;
    
    try {
      this.ws = new WebSocket(wsUrl);
      this.setupEventHandlers();
    } catch (error) {
      console.error('WebSocket connection error:', error);
      useGameStore.getState().setError('Failed to connect to table');
    }
  }

  private setupEventHandlers(): void {
    if (!this.ws) return;

    this.ws.onopen = () => {
      console.log('WebSocket connected');
      this.reconnectAttempts = 0;
      useGameStore.getState().setConnected(true);
      useGameStore.getState().setError(null);
    };

    this.ws.onclose = (event) => {
      console.log('WebSocket disconnected', event.code);
      useGameStore.getState().setConnected(false);

      if (this.isDisconnecting) {
        this.isDisconnecting = false;
        return;
      }

      console.log('Server closed connection - leaving table');
      useGameStore.getState().reset();
      useGameStore.getState().setCurrentView('home');
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      useGameStore.getState().setError('Connection error');
    };

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        const events: HandEvent[] = Array.isArray(data) ? data : [data];
        console.log('%c[TableSocket] ← Received events:', 'color: #3b82f6; font-weight: bold', JSON.stringify(events));
        this.messageHandlers.forEach((handler) => handler(events));
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error);
      }
    };
  }

  private attemptReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log('Max reconnect attempts reached');
      return;
    }

    if (!this.tableId || !this.token) {
      return;
    }

    this.reconnectAttempts++;
    console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);

    setTimeout(() => {
      this.connect(this.tableId!, this.token!);
    }, this.reconnectDelay * this.reconnectAttempts);
  }

  send(action: PlayerAction): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.error('WebSocket is not connected');
      return;
    }

    try {
      console.log('%c[TableSocket] → Sending action: %s', 'color: #10b981; font-weight: bold', JSON.stringify(action));
      this.ws.send(JSON.stringify(action));
    } catch (error) {
      console.error('Failed to send action:', error);
    }
  }

  onMessage(handler: MessageHandler): () => void {
    this.messageHandlers.add(handler);
    return () => {
      this.messageHandlers.delete(handler);
    };
  }

  disconnect(): void {
    this.isDisconnecting = true;
    this.tableId = null;
    this.token = null;
    this.reconnectAttempts = this.maxReconnectAttempts;

    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    useGameStore.getState().setConnected(false);
  }
}

export const tableSocket = new TableSocket();
