import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Sân Chơi Tuổi Thơ — Cờ Tỷ Phú",
  description: "Game Cờ Tỷ Phú multiplayer: server quản lý toàn bộ trạng thái",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi">
      <body>{children}</body>
    </html>
  );
}
