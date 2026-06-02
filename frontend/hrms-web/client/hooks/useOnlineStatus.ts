import { useEffect, useRef } from "react";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";

/**
 * Subscribes to navigator online/offline events and fires sonner toasts.
 * Mount once near the app root.
 */
export function useOnlineStatus() {
  const { t } = useTranslation();
  const wasOffline = useRef(false);

  useEffect(() => {
    const onOffline = () => {
      wasOffline.current = true;
      toast.error(t("common.offline"), { id: "net-status", duration: Infinity });
    };
    const onOnline = () => {
      if (!wasOffline.current) return;
      wasOffline.current = false;
      toast.success(t("common.online"), { id: "net-status", duration: 3000 });
    };

    if (typeof navigator !== "undefined" && navigator.onLine === false) {
      onOffline();
    }

    window.addEventListener("offline", onOffline);
    window.addEventListener("online", onOnline);
    return () => {
      window.removeEventListener("offline", onOffline);
      window.removeEventListener("online", onOnline);
    };
  }, [t]);
}