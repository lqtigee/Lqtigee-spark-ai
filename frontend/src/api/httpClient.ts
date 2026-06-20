const BASE_URL_KEY = "lqtigee_base_url";
const TOKEN_KEY = "lqtigee_token";

export async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const token = localStorage.getItem(TOKEN_KEY);
  if (requiresToken(path) && !token) {
    throw new Error("Authentication token is required");
  }

  const response = await fetch(toApiUrl(path), withAuthHeader(init, token));
  return (await response.json()) as T;
}

function requiresToken(path: string): boolean {
  return normalizePath(path) !== "/api/health";
}

function toApiUrl(path: string): string {
  const baseUrl = localStorage.getItem(BASE_URL_KEY) || window.location.origin;
  return `${baseUrl.replace(/\/$/, "")}${normalizePath(path)}`;
}

function normalizePath(path: string): string {
  return `/${path.replace(/^\//, "")}`;
}

function withAuthHeader(init: RequestInit | undefined, token: string | null): RequestInit | undefined {
  if (!token) {
    return init;
  }

  const headers = new Headers(init?.headers);
  headers.set("Authorization", `Bearer ${token}`);
  return { ...init, headers };
}
