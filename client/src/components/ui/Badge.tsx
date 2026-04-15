interface BadgeProps {
  children: React.ReactNode;
  variant?: 'default' | 'dealer' | 'turn' | 'winner' | 'folded';
  className?: string;
}

export function Badge({ children, variant = 'default', className = '' }: BadgeProps) {
  const baseStyles = 'w-7 h-7 rounded-full flex items-center justify-center text-sm font-bold shadow-md';

  const variantStyles = {
    default: 'bg-white/20 text-white border-2 border-white/30',
    dealer: 'bg-white text-[#1e5c32] border-2 border-[#1e5c32]',
    turn: 'bg-yellow-400 text-[#1e5c32] border-2 border-yellow-500 animate-pulse shadow-lg shadow-yellow-400/50',
    winner: 'bg-yellow-400 text-yellow-800 border-2 border-yellow-600 shadow-lg shadow-yellow-400/50',
    folded: 'bg-red-500/80 text-white border-2 border-red-600 text-xs',
  };

  return (
    <div className={`${baseStyles} ${variantStyles[variant]} ${className}`}>
      {children}
    </div>
  );
}
