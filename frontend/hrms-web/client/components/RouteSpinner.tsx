import { useTranslation } from "react-i18next";

export function RouteSpinner() {
  const { t } = useTranslation();
  return (
    <div className="flex min-h-[50vh] items-center justify-center">
      <div
        role="status"
        aria-live="polite"
        className="flex items-center gap-3 text-sm text-muted-foreground"
      >
        <span className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
        {t("common.loading")}
      </div>
    </div>
  );
}