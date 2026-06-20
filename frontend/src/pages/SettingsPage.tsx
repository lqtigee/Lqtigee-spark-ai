import { useState } from "react";
import type { FormEvent } from "react";

const BASE_URL_KEY = "lqtigee_base_url";
const TOKEN_KEY = "lqtigee_token";
const REFRESH_SECONDS_KEY = "lqtigee_refresh_seconds";

export function SettingsPage() {
  const [baseUrl, setBaseUrl] = useState(() => localStorage.getItem(BASE_URL_KEY) || window.location.origin);
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY) ?? "");
  const [refreshSeconds, setRefreshSeconds] = useState(() => localStorage.getItem(REFRESH_SECONDS_KEY) ?? "10");
  const [saved, setSaved] = useState(false);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    localStorage.setItem(BASE_URL_KEY, baseUrl.trim() || window.location.origin);
    localStorage.setItem(TOKEN_KEY, token.trim());
    localStorage.setItem(REFRESH_SECONDS_KEY, refreshSeconds);
    setSaved(true);
  }

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">连接</p>
          <h2>设置</h2>
        </div>
      </div>
      <form className="settings-form" onSubmit={handleSubmit}>
        <label className="field">
          <span>服务地址</span>
          <input className="input-control" value={baseUrl} onChange={(event) => setBaseUrl(event.target.value)} type="url" />
        </label>
        <label className="field">
          <span>令牌</span>
          <input className="input-control" value={token} onChange={(event) => setToken(event.target.value)} type="password" />
        </label>
        <label className="field">
          <span>刷新间隔秒数</span>
          <input
            className="input-control"
            min="1"
            step="1"
            value={refreshSeconds}
            onChange={(event) => setRefreshSeconds(event.target.value)}
            type="number"
          />
        </label>
        <button className="button button--primary button--wide" type="submit">
          保存
        </button>
      </form>
      {saved ? <p className="ready-state">已保存</p> : null}
    </section>
  );
}
