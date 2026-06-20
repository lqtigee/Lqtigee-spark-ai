import type { ReactNode } from "react";
import { BottomNav } from "./BottomNav";
import { SideNav } from "./SideNav";

const TOKEN_KEY = "lqtigee_token";

interface AppShellProps {
  children: ReactNode;
}

export function AppShell({ children }: AppShellProps) {
  const hasToken = Boolean((localStorage.getItem(TOKEN_KEY) ?? "").trim());

  return (
    <div className="app-shell">
      <header className="app-shell__header">
        <div>
          <p className="app-shell__kicker">Remote Console</p>
          <h1>Lqtigee</h1>
        </div>
        <span className={hasToken ? "app-shell__token app-shell__token--ready" : "app-shell__token"}>
          {hasToken ? "Token saved" : "Token missing"}
        </span>
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
