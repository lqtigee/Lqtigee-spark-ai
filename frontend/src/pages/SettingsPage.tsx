import { useState } from "react";
import type { FormEvent } from "react";

const BASE_URL_KEY = "lqtigee_base_url";
const TOKEN_KEY = "lqtigee_token";
const REFRESH_SECONDS_KEY = "lqtigee_refresh_seconds";

export function SettingsPage() {
  const [baseUrl, setBaseUrl] = useState(() => localStorage.getItem(BASE_URL_KEY) ?? "");
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY) ?? "");
  const [refreshSeconds, setRefreshSeconds] = useState(() => localStorage.getItem(REFRESH_SECONDS_KEY) ?? "10");
  const [saved, setSaved] = useState(false);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    localStorage.setItem(BASE_URL_KEY, baseUrl.trim());
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(REFRESH_SECONDS_KEY, refreshSeconds);
    setSaved(true);
  }

  return (
    <section>
      <h2>Settings</h2>
      <form onSubmit={handleSubmit}>
        <label>
          Base URL
          <input value={baseUrl} onChange={(event) => setBaseUrl(event.target.value)} type="url" />
        </label>
        <label>
          Token
          <input value={token} onChange={(event) => setToken(event.target.value)} type="password" />
        </label>
        <label>
          Refresh seconds
          <input
            min="1"
            step="1"
            value={refreshSeconds}
            onChange={(event) => setRefreshSeconds(event.target.value)}
            type="number"
          />
        </label>
        <button type="submit">Save</button>
      </form>
      {saved ? <p>Saved</p> : null}
    </section>
  );
}
