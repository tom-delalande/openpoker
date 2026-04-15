interface TimerProps {
  timeRemaining: number;
  maxTime: number;
  className?: string;
}

export function Timer({ timeRemaining, maxTime, className = '' }: TimerProps) {
  const percent = maxTime > 0 ? (timeRemaining / maxTime) * 100 : 0;
  const seconds = Math.ceil(timeRemaining / 1000);

  const getColor = () => {
    if (percent < 20) return 'bg-red-500';
    if (percent < 50) return 'bg-yellow-500';
    return 'bg-green-500';
  };

  const getGlow = () => {
    if (percent < 20) return 'shadow-red-500/50';
    if (percent < 50) return 'shadow-yellow-500/50';
    return 'shadow-green-500/50';
  };

  return (
    <div className={`flex flex-col items-center gap-1 ${className}`}>
      <div className="text-white font-bold text-lg tabular-nums">
        {seconds}s
      </div>
      <div className={`
        w-32 h-3 bg-[#1a3622] rounded-full overflow-hidden 
        border-2 border-[#2d5a3d] shadow-lg ${getGlow()}
      `}>
        <div 
          className={`h-full transition-all duration-100 rounded-full ${getColor()}`}
          style={{ width: `${Math.max(0, percent)}%` }}
        />
      </div>
    </div>
  );
}
