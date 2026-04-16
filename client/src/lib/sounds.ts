const STORAGE_KEY = 'poker-sounds-enabled';

class SoundService {
  private audioContext: AudioContext | null = null;
  private _enabled: boolean = true;

  constructor() {
    if (typeof window !== 'undefined') {
      this._enabled = localStorage.getItem(STORAGE_KEY) !== 'false';
    }
  }

  private getContext(): AudioContext {
    if (!this.audioContext) {
      this.audioContext = new AudioContext();
    }
    return this.audioContext;
  }

  get enabled(): boolean {
    return this._enabled;
  }

  set enabled(value: boolean) {
    this._enabled = value;
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, String(value));
    }
  }

  toggle(): boolean {
    this.enabled = !this.enabled;
    return this.enabled;
  }

  isEnabled(): boolean {
    return this._enabled;
  }

  private playTone(
    frequency: number,
    duration: number,
    type: OscillatorType = 'sine',
    endFrequency?: number,
    volume: number = 0.3
  ): void {
    if (!this._enabled) return;

    try {
      const ctx = this.getContext();
      if (ctx.state === 'suspended') {
        ctx.resume();
      }

      const oscillator = ctx.createOscillator();
      const gainNode = ctx.createGain();

      oscillator.type = type;
      oscillator.frequency.setValueAtTime(frequency, ctx.currentTime);

      if (endFrequency) {
        oscillator.frequency.exponentialRampToValueAtTime(endFrequency, ctx.currentTime + duration);
      }

      gainNode.gain.setValueAtTime(volume, ctx.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration);

      oscillator.connect(gainNode);
      gainNode.connect(ctx.destination);

      oscillator.start(ctx.currentTime);
      oscillator.stop(ctx.currentTime + duration);
    } catch (e) {
      console.warn('Sound playback failed:', e);
    }
  }

  private playChord(frequencies: number[], duration: number, type: OscillatorType = 'sine'): void {
    if (!this._enabled) return;

    try {
      const ctx = this.getContext();
      if (ctx.state === 'suspended') {
        ctx.resume();
      }

      frequencies.forEach((freq, i) => {
        const oscillator = ctx.createOscillator();
        const gainNode = ctx.createGain();

        oscillator.type = type;
        oscillator.frequency.setValueAtTime(freq, ctx.currentTime);

        gainNode.gain.setValueAtTime(0.2, ctx.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration);

        oscillator.connect(gainNode);
        gainNode.connect(ctx.destination);

        oscillator.start(ctx.currentTime);
        oscillator.stop(ctx.currentTime + duration);
      });
    } catch (e) {
      console.warn('Sound playback failed:', e);
    }
  }

  playCardDeal(): void {
    this.playTone(800, 0.1, 'sine', 400, 0.2);
  }

  playPrivateCardDeal(): void {
    this.playTone(600, 0.15, 'sine', 350, 0.25);
  }

  playFold(): void {
    this.playTone(400, 0.2, 'triangle', 200, 0.2);
  }

  playCheck(): void {
    this.playTone(600, 0.1, 'sine', 0.2);
  }

  playCall(): void {
    this.playTone(440, 0.15, 'square', 0.15);
  }

  playBet(): void {
    this.playTone(300, 0.2, 'square', 600, 0.15);
  }

  playRaise(): void {
    this.playTone(200, 0.3, 'square', 800, 0.15);
  }

  playHandStart(): void {
    this.playChord([523, 659], 0.4, 'sine');
  }

  playWin(): void {
    setTimeout(() => this.playTone(523, 0.15, 'square', 0.15), 0);
    setTimeout(() => this.playTone(659, 0.15, 'square', 0.15), 150);
    setTimeout(() => this.playTone(784, 0.3, 'square', 0.25), 300);
  }

  playYourTurn(): void {
    setTimeout(() => this.playTone(523, 0.1, 'sine', 0.1), 0);
    setTimeout(() => this.playTone(659, 0.1, 'sine', 0.1), 100);
    setTimeout(() => this.playTone(784, 0.25, 'sine', 0.2), 200);
  }

  playMenuClick(): void {
    this.playTone(600, 0.08, 'sine', 0.15);
  }

  playMenuNavigate(): void {
    this.playChord([392, 494, 587], 0.25, 'sine');
  }

  playAction(action: string): void {
    switch (action) {
      case 'Fold':
        this.playFold();
        break;
      case 'Check':
        this.playCheck();
        break;
      case 'Call':
        this.playCall();
        break;
      case 'Bet':
        this.playBet();
        break;
      case 'Raise':
        this.playRaise();
        break;
    }
  }
}

export const sounds = new SoundService();