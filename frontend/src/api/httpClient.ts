const BASE_URL_KEY = "lqtigee_base_url";

export async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(toApiUrl(path), init);
  return (await response.json()) as T;
}

function toApiUrl(path: string): string {
  const baseUrl = localStorage.getItem(BASE_URL_KEY) || window.location.origin;
  return `${baseUrl.replace(/\/$/, "")}/${path.replace(/^\//, "")}`;
}
