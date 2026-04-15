interface InputProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  type?: 'text' | 'number';
  min?: number;
  max?: number;
  step?: number;
  className?: string;
  autoFocus?: boolean;
  disabled?: boolean;
  onKeyDown?: (e: React.KeyboardEvent) => void;
}

export function Input({
  value,
  onChange,
  placeholder,
  type = 'text',
  min,
  max,
  step,
  className = '',
  autoFocus = false,
  disabled = false,
  onKeyDown,
}: InputProps) {
  return (
    <input
      type={type}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      onKeyDown={onKeyDown}
      placeholder={placeholder}
      min={min}
      max={max}
      step={step}
      autoFocus={autoFocus}
      disabled={disabled}
      className={`w-full px-4 py-3 text-base bg-[#1a472a] border-2 border-[#1a5c32] rounded-xl text-white placeholder-gray-400 
        focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 transition-all
        disabled:opacity-50 disabled:cursor-not-allowed ${className}`}
    />
  );
}
