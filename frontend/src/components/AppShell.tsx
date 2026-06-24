import type { ReactNode } from "react";
import { BottomNav } from "./BottomNav";
import { SideNav } from "./SideNav";
import { isSecureContextForInstall } from "../pwa/secureContext";
import { useInstallPrompt } from "../pwa/useInstallPrompt";

const TOKEN_KEY = "lqtigee_token";

interface AppShellProps {
  children: ReactNode;
}

export function AppShell({ children }: AppShellProps) {
  const hasToken = Boolean((localStorage.getItem(TOKEN_KEY) ?? "").trim());
  const installPrompt = useInstallPrompt();
  const installButtonDisabled = installPrompt.installed || !installPrompt.canPrompt;
  const installButtonTitle = installPrompt.installed
    ? "Lqtigee 已安装"
    : installPrompt.canPrompt
      ? "安装 Lqtigee App"
      : isSecureContextForInstall()
        ? "浏览器准备好安装入口后可用"
        : "安装 App 需要 HTTPS 或 localhost";

  return (
    <div className="app-shell">
      <header className="app-shell__header">
        <div>
          <p className="app-shell__kicker">AI 工作台</p>
          <h1>Lqtigee</h1>
        </div>
        <div className="app-shell__actions">
          <button
            className={installPrompt.canPrompt ? "app-install-button app-install-button--ready" : "app-install-button"}
            disabled={installButtonDisabled}
            onClick={() => void installPrompt.promptInstall()}
            title={installButtonTitle}
            type="button"
          >
            {installPrompt.installed ? "已安装" : "安装 App"}
          </button>
          <span className={hasToken ? "app-shell__token app-shell__token--ready" : "app-shell__token"}>
            {hasToken ? "令牌已保存" : "缺少令牌"}
          </span>
        </div>
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
