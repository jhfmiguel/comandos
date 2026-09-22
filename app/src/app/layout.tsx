import type { Metadata } from "next";

import "./globals.css";
import "components/common/loader/loader.css";
import { SessionProvider } from "components/auth/session-provider";
import { PreferencesProvider } from "components/settings/preferences-provider";
import { ComandosToaster } from "components/common/toast";

export const metadata: Metadata = {
  title: "Comandos",
  icons: {
    icon: "/comandos-logo-v4.png",
    shortcut: "/comandos-logo-v4.png",
    apple: "/comandos-logo-v4.png"
  },
  description: "COMANDOS - Plataforma de Gestão para Segurança Pública e Privada"
};

export default function RootLayout({
  children
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="pt-BR" className="h-full antialiased">
      <body className="min-h-full flex flex-col">
        <ComandosToaster />
        <PreferencesProvider>
          <SessionProvider>{children}</SessionProvider>
        </PreferencesProvider>
      </body>
    </html>
  );
}
