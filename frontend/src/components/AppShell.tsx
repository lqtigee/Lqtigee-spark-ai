import type { ReactNode } from "react";
import { BottomNav } from "./BottomNav";
import { SideNav } from "./SideNav";
import { useAndroidApkDownload } from "../pwa/useAndroidApkDownload";
import { useKeyboardInset } from "../pwa/useKeyboardInset";

const TOKEN_KEY = "lqtigee_token";

interface AppShellProps {
  children: ReactNode;
}

export function AppShell({ children }: AppShellProps) {
  useKeyboardInset();
  const hasToken = Boolean((localStorage.getItem(TOKEN_KEY) ?? "").trim());
  const apkDownload = useAndroidApkDownload();
  const installButtonTitle = apkDownload.error
    ? `版本读取失败：${apkDownload.error}，点击仍会下载 APK`
    : apkDownload.versionName
      ? `下载 Lqtigee Android ${apkDownload.versionName}`
      : "下载 Lqtigee Android APK";

  return (
    <div className="app-shell">
      <header className="app-shell__header">
        <div>
          <p className="app-shell__kicker">AI 工作台</p>
          <h1>Lqtigee</h1>
        </div>
        <div className="app-shell__actions">
          <button
            className="app-install-button app-install-button--ready"
            onClick={apkDownload.downloadApk}
            title={installButtonTitle}
            type="button"
          >
            {apkDownload.loading ? "读取版本" : apkDownload.versionName ? `下载 ${apkDownload.versionName}` : "下载 APK"}
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
