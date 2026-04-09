'use client';

import { useRouter } from 'next/navigation';

export default function Home() {
  const router = useRouter();

  return (
    <div className="flex flex-1 items-center justify-center bg-[#1a472a]">
      <button
        onClick={() => router.push('/table')}
        className="px-8 py-4 text-xl font-bold text-white bg-[#2d5a3d] rounded-lg border-4 border-[#1a3622] hover:bg-[#3d7a4d] active:translate-y-0.5 transition-colors"
      >
        Join Table
      </button>
    </div>
  );
}
