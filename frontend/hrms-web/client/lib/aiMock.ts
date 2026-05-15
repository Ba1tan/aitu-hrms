import type { PayrollForecast, PayrollForecastPoint } from "../../shared/api";

/**
 * Deterministic mock payroll-cost forecast used while ai-ml-service is not
 * deployed. The Phase 5B spec explicitly allows mock data so the forecast
 * chart + horizon controls stay demonstrable. Replace with the real endpoint
 * response once the service ships.
 */
export function mockForecast(months: number): PayrollForecast {
  const baseGross = 14_500_000;
  const growth = 0.018; // ~1.8% MoM
  const now = new Date();
  const points: PayrollForecastPoint[] = [];

  for (let i = 1; i <= months; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() + i, 1);
    const gross = Math.round(baseGross * Math.pow(1 + growth, i));
    const net = Math.round(gross * 0.78);
    const band = Math.round(gross * 0.08); // ~80% CI half-width
    points.push({
      month: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`,
      predictedGross: gross,
      predictedNet: net,
      lowerBound: gross - band,
      upperBound: gross + band,
    });
  }

  return {
    horizonMonths: months,
    points,
    assumptions: {
      headcount: 128,
      avgSalary: Math.round(baseGross / 128),
      growthRate: growth,
    },
  };
}
