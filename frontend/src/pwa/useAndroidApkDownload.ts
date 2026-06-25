import { useCallback, useEffect, useMemo, useState } from "react";
import { toApiUrl } from "../api/httpClient";

interface AppVersionManifest {
  versionCode: number;
  versionName: string;
  apkUrl: string;
  sha256: string;
  releaseNotes: string;
}

interface AndroidApkDownloadState {
  loading: boolean;
  versionName: string | null;
  error: string | null;
  downloadApk(): void;
}

export function useAndroidApkDownload(): AndroidApkDownloadState {
  const [manifest, setManifest] = useState<AppVersionManifest | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadManifest() {
      setLoading(true);
      setError(null);
      try {
        const response = await fetch(toApiUrl("/downloads/app-version.json"), {
          cache: "no-store",
          headers: { Accept: "application/json" }
        });
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }
        const nextManifest = (await response.json()) as AppVersionManifest;
        if (!cancelled) {
          setManifest(nextManifest);
        }
      } catch (caughtError) {
        if (!cancelled) {
          setError(caughtError instanceof Error ? caughtError.message : "无法读取 APK 版本");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void loadManifest();
    return () => {
      cancelled = true;
    };
  }, []);

  const apkHref = useMemo(() => {
    if (!manifest) {
      return toApiUrl("/downloads/Lqtigee-debug.apk");
    }
    const path = manifest.apkUrl || "/downloads/Lqtigee-debug.apk";
    const separator = path.includes("?") ? "&" : "?";
    return toApiUrl(`${path}${separator}versionCode=${encodeURIComponent(String(manifest.versionCode))}`);
  }, [manifest]);

  const downloadApk = useCallback(() => {
    window.location.assign(apkHref);
  }, [apkHref]);

  return {
    loading,
    versionName: manifest?.versionName ?? null,
    error,
    downloadApk
  };
}
