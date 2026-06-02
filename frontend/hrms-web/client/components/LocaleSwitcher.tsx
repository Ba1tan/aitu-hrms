import { Languages } from "lucide-react";
import { useTranslation } from "react-i18next";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { LOCALE_LABEL, SUPPORTED_LOCALES, type Locale } from "@/i18n";

export function LocaleSwitcher() {
  const { i18n, t } = useTranslation();
  const current = (i18n.resolvedLanguage as Locale) ?? "ru";

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          aria-label={t("common.language")}
          className="inline-flex h-9 items-center gap-2 rounded-full border border-border/40 bg-background/60 px-3 text-xs font-semibold uppercase text-foreground hover:bg-accent/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring transition-colors"
        >
          <Languages size={14} />
          {current.toUpperCase()}
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-44">
        {SUPPORTED_LOCALES.map((lng) => (
          <DropdownMenuItem
            key={lng}
            onClick={() => void i18n.changeLanguage(lng)}
            className={current === lng ? "font-semibold" : undefined}
          >
            <span className="w-6 text-xs uppercase text-muted-foreground">
              {lng}
            </span>
            {LOCALE_LABEL[lng]}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}