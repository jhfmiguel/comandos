"use client";

import * as React from "react";
import { Dialog as BaseDialog } from "@base-ui/react/dialog";

export interface DialogOpenChangeEvent { value?: boolean }

function Root({ onOpenChange, ...props }: Omit<React.ComponentProps<typeof BaseDialog.Root>, "onOpenChange"> & {
  onOpenChange?: (event: DialogOpenChangeEvent) => void;
}) {
  return <BaseDialog.Root {...props} onOpenChange={(open) => onOpenChange?.({ value: open })} />;
}

const Header = ({ children, ...props }: React.HTMLAttributes<HTMLDivElement>) => <div {...props}>{children}</div>;
const Content = ({ children, ...props }: React.HTMLAttributes<HTMLDivElement>) => <div {...props}>{children}</div>;

export const Dialog = {
  Root,
  Portal: BaseDialog.Portal,
  Backdrop: BaseDialog.Backdrop,
  Positioner: BaseDialog.Viewport,
  Popup: BaseDialog.Popup,
  Header,
  Title: BaseDialog.Title,
  Content,
  Close: BaseDialog.Close
};
