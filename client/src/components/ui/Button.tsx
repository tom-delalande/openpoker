interface ButtonProps {
  children: React.ReactNode;
  onClick?: () => void;
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  disabled?: boolean;
  className?: string;
  fullWidth?: boolean;
}

export function Button({
  children,
  onClick,
  variant = 'primary',
  size = 'md',
  disabled = false,
  className = '',
  fullWidth = false,
}: ButtonProps) {
  const baseStyles = 'font-bold rounded-xl transition-all duration-150 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:active:scale-100 flex items-center justify-center gap-2';

  const variantStyles = {
    primary: 'bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-900/30 border-2 border-blue-700 hover:border-blue-500',
    secondary: 'bg-[#1e5c32] hover:bg-[#2a7040] text-white shadow-lg shadow-green-900/30 border-2 border-[#0d3d22] hover:border-[#1a5c32]',
    danger: 'bg-red-600 hover:bg-red-500 text-white shadow-lg shadow-red-900/30 border-2 border-red-700 hover:border-red-500',
    ghost: 'bg-transparent hover:bg-white/10 text-white border-2 border-transparent hover:border-white/20',
  };

  const sizeStyles = {
    sm: 'px-3 py-1.5 text-sm min-h-[36px]',
    md: 'px-4 py-2.5 text-base min-h-[48px]',
    lg: 'px-6 py-4 text-lg min-h-[56px]',
  };

  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`${baseStyles} ${variantStyles[variant]} ${sizeStyles[size]} ${fullWidth ? 'w-full' : ''} ${className}`}
    >
      {children}
    </button>
  );
}
