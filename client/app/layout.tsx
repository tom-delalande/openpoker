import type { Metadata } from "next";
import { Geist, Geist_Mono, Fredoka } from "next/font/google";
import "./globals.css";
import App from "./App";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

const boogaloo = Fredoka({
  variable: "--font-fredoka",
  weight: "400",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "TinyPoker",
  description: "Online poker table",
};

export default function RootLayout() {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} ${boogaloo.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col"><App /></body>
    </html>
  );
}
