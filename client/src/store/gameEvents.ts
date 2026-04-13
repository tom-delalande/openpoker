import type { components } from '../lib/api/types';
import { useGameStore } from '../store/gameStore';

type HandEvent = components['schemas']['HandEvent'];

export function handleGameEvent(event: HandEvent): void {
  const store = useGameStore.getState();
  const { playerId } = store;

  switch (event.kind) {
    case 'PlayerSatDown': {
      const player = event.value;
      store.addPlayer({
        id: player.playerId,
        name: player.playerName,
        stack: player.stack,
        seat: player.seat,
        currentBet: 0,
      });
      break;
    }

    case 'PlayerStoodUp': {
      store.removePlayer(event.value.playerId);
      break;
    }

    case 'HandStarted': {
      store.setDealerButton(event.value.dealerButton);
      store.setCommunityCards([]);
      store.setCurrentPot(0);
      store.setCurrentStreet(null);
      store.setActionOptions(null);
      store.setActionExpiry(null);
      store.setCurrentPlayerId(null);
      store.setMyCards([]);
      store.players.forEach((p) => {
        store.updatePlayer(p.id, { hasActed: false, hasFolded: false, cards: undefined, currentBet: 0 });
      });
      break;
    }

    case 'RoundStarted': {
      store.setCurrentStreet(event.value.street);
      store.setActionOptions(null);
      store.setCurrentPlayerId(null);
      store.players.forEach((p) => {
        store.updatePlayer(p.id, { hasActed: false, currentBet: 0 });
      });
      break;
    }

    case 'CommunityCardDealt': {
      const currentCards = store.communityCards;
      store.setCommunityCards([...currentCards, ...event.value.cards]);
      break;
    }

    case 'PrivateCardDealt': {
      const cards = event.value.cards;
      if (event.value.playerId === playerId) {
        store.setMyCards(cards);
      }
      store.updatePlayer(event.value.playerId, { cards });
      break;
    }

    case 'PlayerActionRequested': {
      const req = event.value;
      store.setCurrentPlayerId(req.playerId);
      store.setActionOptions(req.actionOptions);
      store.setActionExpiry(req.expiry);
      break;
    }

    case 'PlayerPostedSmallBlind': {
      const { playerId: sbPlayerId, amount } = event.value;
      const player = store.players.find((p) => p.id === sbPlayerId);
      if (player) {
        store.updatePlayer(sbPlayerId, { stack: player.stack - amount, hasActed: true, currentBet: player.currentBet + amount });
      }
      store.setCurrentPot(store.currentPot + amount);
      break;
    }

    case 'PlayerPostedBigBlind': {
      const { playerId: bbPlayerId, amount } = event.value;
      const player = store.players.find((p) => p.id === bbPlayerId);
      if (player) {
        store.updatePlayer(bbPlayerId, { stack: player.stack - amount, hasActed: true, currentBet: player.currentBet + amount });
      }
      store.setCurrentPot(store.currentPot + amount);
      break;
    }

    case 'PlayerPostedAnte': {
      const { playerId: antePlayerId, amount } = event.value;
      const player = store.players.find((p) => p.id === antePlayerId);
      if (player) {
        store.updatePlayer(antePlayerId, { stack: player.stack - amount, currentBet: player.currentBet + amount });
      }
      store.setCurrentPot(store.currentPot + amount);
      break;
    }

case 'PlayerFolded': {
      const { playerId } = event.value;
      store.updatePlayer(playerId, { hasFolded: true });
      break;
    }

    case 'PlayerChecked': {
      store.updatePlayer(event.value.playerId, { hasActed: true });
      break;
    }

    case 'PlayerBet': {
      const { playerId: betPlayerId, amount } = event.value;
      const player = store.players.find((p) => p.id === betPlayerId);
      if (player) {
        store.updatePlayer(betPlayerId, { stack: player.stack - amount, hasActed: true, currentBet: player.currentBet + amount });
      }
      store.setCurrentPot(store.currentPot + amount);
      break;
    }

    case 'PlayerRaised': {
      const { playerId: raisePlayerId, amount } = event.value;
      const player = store.players.find((p) => p.id === raisePlayerId);
      if (player) {
        store.updatePlayer(raisePlayerId, { stack: player.stack - amount, hasActed: true, currentBet: player.currentBet + amount });
      }
      store.setCurrentPot(store.currentPot + amount);
      break;
    }

    case 'PlayerCalled': {
      const { playerId: callPlayerId, amount } = event.value;
      const player = store.players.find((p) => p.id === callPlayerId);
      if (player) {
        store.updatePlayer(callPlayerId, { stack: player.stack - amount, hasActed: true, currentBet: player.currentBet + amount });
      }
      store.setCurrentPot(store.currentPot + amount);
      break;
    }

    case 'HandFinished': {
      store.setActionOptions(null);
      store.setActionExpiry(null);
      store.setCurrentPlayerId(null);
      store.setMyCards([]);
      store.setCommunityCards([]);
      break;
    }

    default:
      console.log('Unhandled event type:', event);
  }
}

export function processEvents(events: HandEvent[]): void {
  events.forEach(handleGameEvent);
}
