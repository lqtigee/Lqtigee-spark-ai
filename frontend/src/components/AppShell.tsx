import type { ReactNode } from "react";
import { BottomNav } from "./BottomNav";
import { SideNav } from "./SideNav";

interface AppShellProps {
  children: ReactNode;
}

export function AppShell({ children }: AppShellProps) {
  return (
    <div className="app-shell">
      <header className="app-shell__header">
        <h1>Lqtigee</h1>
      </header>
      <aside className="app-shell__side-nav">
        <SideNav />
      </aside>
      <main className="app-shell__main">{children}</main>
      <div className="app-shell__bottom-nav">
        <BottomNav />
      </div>
    </div>
  );
}
