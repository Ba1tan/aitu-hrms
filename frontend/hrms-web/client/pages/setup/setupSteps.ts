export interface SetupStep {
  path: string;
  title: string;
  description?: string;
}

/**
 * Order of the setup wizard. The router file mirrors this list.
 */
export const SETUP_STEPS: SetupStep[] = [
  { path: "welcome", title: "Добро пожаловать" },
  { path: "company", title: "Компания" },
  { path: "work-schedule", title: "График работы" },
  { path: "holidays", title: "Праздники" },
  { path: "attendance-methods", title: "Способы отметки" },
  { path: "department", title: "Подразделение" },
  { path: "integrations", title: "Интеграции" },
  { path: "review", title: "Готово" },
];

export function stepIndex(path: string): number {
  return SETUP_STEPS.findIndex((s) => s.path === path);
}
