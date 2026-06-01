import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";

import ru from "./locales/ru.json";
import kk from "./locales/kk.json";
import en from "./locales/en.json";

export const SUPPORTED_LOCALES = ["ru", "kk", "en"] as const;
export type Locale = (typeof SUPPORTED_LOCALES)[number];

export const LOCALE_LABEL: Record<Locale, string> = {
  ru: "Русский",
  kk: "Қазақша",
  en: "English",
};

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      ru: { translation: ru },
      kk: { translation: kk },
      en: { translation: en },
    },
    supportedLngs: [...SUPPORTED_LOCALES],
    fallbackLng: ["en", "ru"],
    nonExplicitSupportedLngs: true,
    interpolation: { escapeValue: false },
    detection: {
      order: ["localStorage", "navigator", "htmlTag"],
      caches: ["localStorage"],
      lookupLocalStorage: "hrms.locale",
    },
    returnNull: false,
    react: { useSuspense: false },
  });

export default i18n;