'use client';

import { useState, useCallback, useEffect } from 'react';
import { sounds } from '../../lib/sounds';

function SpeakerIcon({ muted }: { muted: boolean }) {
  if (muted) {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="w-6 h-6">
        <path d="M11 5L6 9H2v6h4l5 4V5z" />
        <line x1="23" y1="9" x2="17" y2="15" />
        <line x1="17" y1="9" x2="23" y2="15" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="w-6 h-6">
      <path d="M11 5L6 9H2v6h4l5 4V5z" />
      <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
      <path d="M19.07 4.93a10 10 0 0 1 0 14.14" />
    </svg>
  );
}

export function SoundToggle() {
  const [enabled, setEnabled] = useState(sounds.enabled);

  useEffect(() => {
    setEnabled(sounds.enabled);
  }, []);

  const handleToggle = useCallback(() => {
    const newState = sounds.toggle();
    if (newState) {
      sounds.playMenuClick();
    }
    setEnabled(newState);
  }, []);

  return (
    <button
      onClick={handleToggle}
      className="fixed top-4 right-4 z-50 p-3 rounded-full bg-[#1a3622]/90 backdrop-blur-md border-2 border-[#2d5a3d] text-white shadow-lg hover:bg-[#2d5a3d] transition-colors"
      title={enabled ? 'Mute sounds' : 'Enable sounds'}
    >
      <SpeakerIcon muted={!enabled} />
    </button>
  );
}