import { useQuery } from "@tanstack/react-query";
import {
  aiApi,
  type AttritionRisk,
  type PayrollAnomaly,
  type PayrollForecast,
} from "../../../shared/api";

const AI_KEY = ["ai"] as const;

const SWALLOW = [502, 503, 404];

/**
 * Attrition risk list. ai-ml-service is not deployed yet — 502/503/404 yields
 * an empty list with `available:false` so pages render a clean placeholder.
 */
export const useAttritionRisk = (departmentId?: string) =>
  useQuery({
    queryKey: [...AI_KEY, "attrition", departmentId ?? "all"],
    queryFn: async () => {
      try {
        const res = await aiApi.attritionRisk(departmentId);
        return { items: res.data ?? [], available: true };
      } catch (err: any) {
        if (SWALLOW.includes(err?.response?.status)) {
          return { items: [] as AttritionRisk[], available: false };
        }
        throw err;
      }
    },
  });

export const useRecentAnomalies = () =>
  useQuery({
    queryKey: [...AI_KEY, "anomalies"],
    queryFn: async () => {
      try {
        const res = await aiApi.recentAnomalies();
        return { items: res.data ?? [], available: true };
      } catch (err: any) {
        if (SWALLOW.includes(err?.response?.status)) {
          return { items: [] as PayrollAnomaly[], available: false };
        }
        throw err;
      }
    },
  });

/**
 * Payroll cost forecast. When the service is down we return `available:false`
 * and let the page substitute mock data so the chart + horizon controls stay
 * demonstrable (per the Phase 5B spec).
 */
export const usePayrollForecast = (months: number) =>
  useQuery({
    queryKey: [...AI_KEY, "forecast", months],
    queryFn: async () => {
      try {
        const res = await aiApi.payrollForecast(months);
        return { forecast: res.data, available: true };
      } catch (err: any) {
        if (SWALLOW.includes(err?.response?.status)) {
          return { forecast: null as PayrollForecast | null, available: false };
        }
        throw err;
      }
    },
  });