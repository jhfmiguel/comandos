import type { Metadata } from "next";

import "./globals.css";
import "components/common/loader/loader.css";
import { SessionProvider } from "components/auth/session-provider";
import { PreferencesProvider } from "components/settings/preferences-provider";
import { ComandosToaster } from "components/common/toast";
import { productDefinition } from "platform/product";

export const metadata: Metadata = {
  title: productDefinition.name,
  icons: {
    icon: productDefinition.iconPath,
    shortcut: productDefinition.iconPath,
    apple: productDefinition.iconPath
  },
  description: `${productDefinition.name} - ${productDefinition.description}`
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
