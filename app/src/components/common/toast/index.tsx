"use client";

import * as React from "react";
import { Check, ExclamationTriangle, InfoCircle, Times } from "components/ui/icons";

export const COMANDOS_TOAST_GROUP = "severity";

export type ComandosToastSeverity =
  | "success" | "info" | "warn" | "error" | "secondary" | "contrast";

export interface ComandosToastOptions {
  title?: React.ReactNode;
  description: React.ReactNode;
  duration?: number;
  onDismiss?: (toastItem: ToastType) => void;
}

export interface ToastType extends ComandosToastOptions {
  id: string;
  severity: ComandosToastSeverity;
}

const listeners = new Set<(items: ToastType[]) => void>();
let items: ToastType[] = [];

function publish() {
  for (const listener of listeners) listener(items);
}

function dismiss(id: string) {
  const item = items.find((entry) => entry.id === id);
  items = items.filter((entry) => entry.id !== id);
  publish();
  if (item) item.onDismiss?.(item);
}

function emitToast(severity: ComandosToastSeverity, options: ComandosToastOptions) {
  const item: ToastType = {
    id: crypto.randomUUID(),
    severity,
    ...options
  };
  items = [...items.slice(-4), item];
  publish();
  window.setTimeout(() => dismiss(item.id), options.duration ?? 4000);
  return item.id;
}

export const notify = {
  success: (options: ComandosToastOptions) => emitToast("success", options),
  info: (options: ComandosToastOptions) => emitToast("info", options),
  warn: (options: ComandosToastOptions) => emitToast("warn", options),
  error: (options: ComandosToastOptions) => emitToast("error", options),
  secondary: (options: ComandosToastOptions) => emitToast("secondary", options),
  contrast: (options: ComandosToastOptions) => emitToast("contrast", options)
};

function ToastIcon({ severity }: { severity: ComandosToastSeverity }) {
  if (severity === "success") return <Check />;
  if (severity === "error") return <Times />;
  if (severity === "warn") return <ExclamationTriangle />;
  return <InfoCircle />;
}

export function ComandosToaster() {
  const [toasts, setToasts] = React.useState<ToastType[]>(() => items);

  React.useEffect(() => {
    listeners.add(setToasts);
    return () => {
      listeners.delete(setToasts);
    };
  }, []);

  return (
    <div className="comandos-toaster" aria-live="polite" aria-relevant="additions">
      {toasts.map((item) => (
        <article key={item.id} className={`comandos-toast comandos-toast-${item.severity}`}>
          <div className="comandos-toast-icon"><ToastIcon severity={item.severity} /></div>
          <div className="comandos-toast-message">
            {item.title && <strong>{item.title}</strong>}
            <div>{item.description}</div>
          </div>
          <button type="button" className="comandos-toast-close" aria-label="Fechar notificação" onClick={() => dismiss(item.id)}>
            <Times />
          </button>
        </article>
      ))}
    </div>
  );
}
