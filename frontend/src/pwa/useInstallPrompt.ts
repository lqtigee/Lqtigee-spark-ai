import { useCallback, useEffect, useState } from "react";
import { isSecureContextForInstall } from "./secureContext";

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>;
  userChoice: Promise<{ outcome: "accepted" | "dismissed"; platform: string }>;
}

interface InstallPromptState {
  canPrompt: boolean;
  installed: boolean;
  promptInstall(): Promise<void>;
}

export function useInstallPrompt(): InstallPromptState {
  const [promptEvent, setPromptEvent] = useState<BeforeInstallPromptEvent | null>(null);
  const [installed, setInstalled] = useState(false);

  useEffect(() => {
    function handleBeforeInstallPrompt(event: Event) {
      event.preventDefault();
      if (isSecureContextForInstall()) {
        setPromptEvent(event as BeforeInstallPromptEvent);
      }
    }

    window.addEventListener("beforeinstallprompt", handleBeforeInstallPrompt);
    return () => {
      window.removeEventListener("beforeinstallprompt", handleBeforeInstallPrompt);
    };
  }, []);

  const promptInstall = useCallback(async () => {
    if (!promptEvent) {
      return;
    }

    await promptEvent.prompt();
    const choice = await promptEvent.userChoice;
    setPromptEvent(null);
    if (choice.outcome === "accepted") {
      setInstalled(true);
    }
  }, [promptEvent]);

  return {
    canPrompt: promptEvent !== null,
    installed,
    promptInstall
  };
}
