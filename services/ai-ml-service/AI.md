# AI/ML Service.md

**Port:** 8086 | **No database** (stateless) | **Owner:** Nursultan | **Tech:** Python 3.11, FastAPI, scikit-learn, XGBoost

## Responsibility
ML inference for payroll anomaly detection, attendance fraud detection, employee attrition prediction, payroll cost forecasting. Stateless — models loaded at startup from .joblib files. Training happens offline.

## Project Structure
```
services/ai-ml-service/
├── Dockerfile
├── requirements.txt          # fastapi, uvicorn, scikit-learn, xgboost, pandas, numpy, joblib, prophet
├── app/
│   ├── main.py               # FastAPI app, CORS, lifespan (model loading)
│   ├── config.py              # pydantic-settings
│   ├── routers/
│   │   ├── payroll_anomaly.py
│   │   ├── attendance_fraud.py
│   │   ├── attrition.py
│   │   └── forecast.py
│   ├── models/
│   │   ├── payroll_model.py   # IsolationForest wrapper
│   │   ├── attendance_model.py
│   │   ├── attrition_model.py # XGBoost wrapper
│   │   └── forecast_model.py  # Prophet wrapper
│   ├── schemas/               # Pydantic request/response models
│   └── ml/
│       ├── models/            # Serialized .joblib files
│       └── training/          # Offline training scripts
├── tests/
└── data/                      # Training CSVs (not in Docker image)
```

## Endpoints (8)

```
# Payroll Anomaly (called by payroll-service during generation)
POST /v1/ai/payroll/detect              Single payslip → {anomaly_score, is_anomaly, flags, recommendation}
POST /v1/ai/payroll/detect/batch        Array of payslips → array of results

# Attendance Fraud (called by attendance-service on suspicious check-in)
POST /v1/ai/attendance/fraud-detect     Single check-in → {fraud_probability, is_fraud, flags, recommendation}

# Attrition (called by HR dashboard)
GET  /v1/ai/attrition/risk              ?departmentId= → [{employeeId, risk, level, factors, actions}]
GET  /v1/ai/attrition/risk/employee/{id}  Individual risk assessment
GET  /v1/ai/attrition/dashboard         Company summary {highRisk, medRisk, lowRisk, topFactors}

# Forecast (called by finance dashboard)
GET  /v1/ai/payroll/forecast            ?months=3 → [{month, predictedGross, predictedNet, confidence}]

# Health
GET  /v1/ai/health                      {models: [{name, version, loadedAt}], status: "healthy"}
```

## Model Details

### Payroll Anomaly — Isolation Forest
**Features (14):** earned_salary, net_salary, allowances, deductions, work_ratio, salary_zscore, months_employed, allowance_ratio, deduction_ratio, ipn_deviation, opv_deviation, is_new_employee, previous_month_salary, salary_change_pct
**Thresholds:** <0.3 NORMAL, 0.3-0.65 WARNING, >0.65 REVIEW
**Training:** ~10K historical payslips + ~500 injected anomalies, contamination=0.05

### Attendance Fraud — Isolation Forest
**Features (10):** time_diff_minutes, location_distance_meters, device_switch, hour_of_day, day_of_week, historical_checkin_hour_avg/std, checkins_today_count, days_since_last_checkin, is_remote_worker
**Thresholds:** <0.3 ALLOW, 0.3-0.65 FLAG, >0.65 BLOCK

### Attrition — XGBoost
**Features (14):** tenure_months, salary_vs_position_avg, salary_growth_rate, months_since_promotion, leave_usage_rate, sick_leave_frequency, late_attendance_rate, overtime_hours_avg, team_turnover_6m, manager_change_count, age, dept_avg_tenure, performance_rating, engagement_proxy
**Target metrics:** AUC≥0.85, F1≥0.80

### Forecast — Prophet/ARIMA
**Input:** 12+ months historical monthly payroll totals
**Output:** next N months predicted gross/net with confidence intervals

## CRITICAL: Non-Blocking Pattern
AI service failures must NEVER block business operations. Java callers wrap in try/catch:
```java
try { result = aiClient.detect(request); }
catch (Exception e) { log.warn("AI unavailable — skipping"); }
```

## Dockerfile
```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8086
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8086"]
```
