import { useEffect } from "react";

export function useKeyboardInset() {
  useEffect(() => {
    const root = document.documentElement;

    function updateKeyboardInset() {
      const viewport = window.visualViewport;
      if (!viewport) {
        root.style.setProperty("--keyboard-inset", "0px");
        root.style.setProperty("--visual-viewport-height", `${window.innerHeight}px`);
        return;
      }

      const viewportBottom = viewport.offsetTop + viewport.height;
      const keyboardInset = Math.max(0, window.innerHeight - viewportBottom);
      root.style.setProperty("--keyboard-inset", `${Math.round(keyboardInset)}px`);
      root.style.setProperty("--visual-viewport-height", `${Math.round(viewport.height)}px`);
    }

    updateKeyboardInset();
    window.visualViewport?.addEventListener("resize", updateKeyboardInset);
    window.visualViewport?.addEventListener("scroll", updateKeyboardInset);
    window.addEventListener("orientationchange", updateKeyboardInset);
    window.addEventListener("resize", updateKeyboardInset);

    return () => {
      window.visualViewport?.removeEventListener("resize", updateKeyboardInset);
      window.visualViewport?.removeEventListener("scroll", updateKeyboardInset);
      window.removeEventListener("orientationchange", updateKeyboardInset);
      window.removeEventListener("resize", updateKeyboardInset);
      root.style.setProperty("--keyboard-inset", "0px");
      root.style.removeProperty("--visual-viewport-height");
    };
  }, []);
}
