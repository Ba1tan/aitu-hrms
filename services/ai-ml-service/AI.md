# AI/ML Service.md

**Port:** 8086 | **No database** (stateless) | **Owner:** Nursultan | **Tech:** Python 3.11, FastAPI, scikit-learn, XGBoost, face_recognition/InsightFace

## Responsibility
ML inference for: face recognition (enrollment + verification), payroll anomaly detection, attendance fraud detection, employee attrition prediction, payroll cost forecasting. Stateless — models and embeddings loaded at startup from disk. Training happens offline.

## Project Structure
```
services/ai-ml-service/
├── Dockerfile
├── requirements.txt
├── app/
│   ├── main.py                    # FastAPI app, CORS, lifespan (load models + embeddings)
│   ├── config.py                  # pydantic-settings
│   ├── routers/
│   │   ├── biometric.py           # POST /verify, /enroll, /delete, GET /status
│   │   ├── payroll_anomaly.py     # POST /detect, /detect/batch
│   │   ├── attendance_fraud.py    # POST /fraud-detect
│   │   ├── attrition.py           # GET /risk, /risk/{id}, /dashboard
│   │   └── forecast.py            # GET /forecast
│   ├── models/
│   │   ├── face_recognition_model.py   # FaceNet/ArcFace wrapper
│   │   ├── payroll_model.py            # Isolation Forest
│   │   ├── attendance_model.py         # Isolation Forest
│   │   ├── attrition_model.py          # XGBoost
│   │   └── forecast_model.py           # Prophet
│   ├── schemas/
│   │   ├── biometric.py           # Pydantic request/response
│   │   ├── payroll.py
│   │   ├── attendance.py
│   │   └── attrition.py
│   ├── store/
│   │   └── embedding_store.py     # In-memory embedding store + .npy persistence
│   └── ml/
│       ├── models/                # Serialized .joblib model files
│       └── training/
│           ├── train_payroll_anomaly.py
│           ├── train_attendance_fraud.py
│           └── generate_synthetic_data.py
├── tests/
└── data/                          # Training CSVs (not in Docker image)
```

## Storage Paths
```
/data/hrms/uploads/employees/{employeeId}/biometric/   # Enrollment photos (read-only)
/data/hrms/ai-models/embeddings/{employeeId}.npy       # Face embeddings (read/write)
/data/hrms/ai-models/payroll_anomaly_v1.joblib         # Trained models
/data/hrms/ai-models/attendance_fraud_v1.joblib
/data/hrms/ai-models/attrition_v1.joblib
```

## Endpoints (12)

```
# Face Recognition — Biometric
POST /v1/ai/biometric/enroll              # Multipart: employeeId + 3-5 face photos → extract & store embedding
POST /v1/ai/biometric/verify              # Multipart: single face photo → match against all embeddings
DELETE /v1/ai/biometric/{employeeId}      # Remove embedding (on termination)
GET  /v1/ai/biometric/status              # {enrolledCount, modelVersion, lastEnrollment}

# Payroll Anomaly Detection
POST /v1/ai/payroll/detect                # Single payslip → {anomaly_score, is_anomaly, flags}
POST /v1/ai/payroll/detect/batch          # Array of payslips → array of results

# Attendance Fraud Detection (behavioral — runs AFTER face match)
POST /v1/ai/attendance/fraud-detect       # Check-in features → {fraud_probability, is_fraud, flags}

# Attrition Prediction
GET  /v1/ai/attrition/risk                # ?departmentId= → [{employeeId, risk, level, factors}]
GET  /v1/ai/attrition/risk/employee/{id}  # Individual risk
GET  /v1/ai/attrition/dashboard           # Company summary

# Forecast
GET  /v1/ai/payroll/forecast              # ?months=3 → predicted payroll costs

# Health
GET  /v1/ai/health                        # {models, embeddingsLoaded, status}
```

## Face Recognition — Full Implementation

### Tech Choice
```
# requirements.txt
fastapi==0.111.0
uvicorn==0.30.0
python-multipart==0.0.9
numpy==1.26.4
opencv-python-headless==4.9.0.80
insightface==0.7.3              # ArcFace model — better accuracy than face_recognition
onnxruntime==1.17.3
scikit-learn==1.5.1
xgboost==2.0.3
pandas==2.2.2
joblib==1.4.2
```

### Embedding Store (in-memory + disk)
```python
# app/store/embedding_store.py
import numpy as np
from pathlib import Path
from threading import Lock

class EmbeddingStore:
    def __init__(self, embeddings_dir: str = "/data/hrms/ai-models/embeddings"):
        self.dir = Path(embeddings_dir)
        self.dir.mkdir(parents=True, exist_ok=True)
        self.embeddings: dict[str, np.ndarray] = {}
        self.lock = Lock()
        self.load_all()

    def load_all(self):
        """Load all .npy embeddings into memory at startup."""
        for f in self.dir.glob("*.npy"):
            self.embeddings[f.stem] = np.load(f)

    def enroll(self, employee_id: str, embedding: np.ndarray):
        """Store embedding to disk and memory."""
        with self.lock:
            np.save(self.dir / f"{employee_id}.npy", embedding)
            self.embeddings[employee_id] = embedding

    def delete(self, employee_id: str):
        """Remove embedding from disk and memory."""
        with self.lock:
            path = self.dir / f"{employee_id}.npy"
            if path.exists():
                path.unlink()
            self.embeddings.pop(employee_id, None)

    def find_closest(self, embedding: np.ndarray) -> tuple[str | None, float]:
        """Compare against all enrolled faces. Returns (employeeId, similarity)."""
        if not self.embeddings:
            return None, 0.0
        best_id, best_score = None, 0.0
        for emp_id, stored in self.embeddings.items():
            score = float(np.dot(embedding, stored) / (
                np.linalg.norm(embedding) * np.linalg.norm(stored) + 1e-8))
            if score > best_score:
                best_id, best_score = emp_id, score
        return best_id, best_score

    @property
    def count(self) -> int:
        return len(self.embeddings)
```

### Face Recognition Model
```python
# app/models/face_recognition_model.py
import cv2
import numpy as np
from insightface.app import FaceAnalysis

class FaceRecognitionModel:
    def __init__(self):
        self.app = FaceAnalysis(name="buffalo_l", providers=["CPUExecutionProvider"])
        self.app.prepare(ctx_id=0, det_size=(640, 640))

    def detect_and_encode(self, image_bytes: bytes) -> tuple[np.ndarray | None, str]:
        """
        Detect face and extract 512-dim embedding.
        Returns (embedding, error_message).
        """
        nparr = np.frombuffer(image_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if img is None:
            return None, "invalid_image"

        faces = self.app.get(img)
        if len(faces) == 0:
            return None, "no_face_detected"
        if len(faces) > 1:
            return None, "multiple_faces"

        embedding = faces[0].embedding  # 512-dim vector
        # Normalize
        embedding = embedding / np.linalg.norm(embedding)
        return embedding, ""

    def detect_liveness(self, image_bytes: bytes) -> tuple[bool, float]:
        """
        Basic liveness detection — checks face quality score.
        Returns (is_live, confidence).
        InsightFace provides det_score which partially catches photos.
        """
        nparr = np.frombuffer(image_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        faces = self.app.get(img)
        if not faces:
            return False, 0.0
        det_score = float(faces[0].det_score)
        return det_score > 0.7, det_score
```

### Enrollment Endpoint
```python
# app/routers/biometric.py
from fastapi import APIRouter, UploadFile, File, Form, HTTPException
from typing import List

router = APIRouter(prefix="/v1/ai/biometric", tags=["Biometric"])

@router.post("/enroll")
async def enroll_face(
    employee_id: str = Form(...),
    files: List[UploadFile] = File(...)
):
    """
    Receive 3-5 face photos, extract embeddings, average them, store.
    Called by employee-service during biometric enrollment.
    """
    if len(files) < 3:
        raise HTTPException(400, "At least 3 photos required for enrollment")
    if len(files) > 5:
        raise HTTPException(400, "Maximum 5 photos for enrollment")

    embeddings = []
    for i, photo in enumerate(files):
        image_bytes = await photo.read()
        embedding, error = face_model.detect_and_encode(image_bytes)
        if embedding is None:
            raise HTTPException(400, f"Photo {i+1}: {error}")
        embeddings.append(embedding)

    # Average all embeddings for robustness
    avg_embedding = np.mean(embeddings, axis=0)
    avg_embedding = avg_embedding / np.linalg.norm(avg_embedding)

    # Store
    embedding_store.enroll(employee_id, avg_embedding)

    return {
        "enrolled": True,
        "employeeId": employee_id,
        "photosProcessed": len(files),
        "embeddingDimension": len(avg_embedding)
    }

@router.post("/verify")
async def verify_face(file: UploadFile = File(...)):
    """
    Receive single face photo, match against all enrolled embeddings.
    Called by attendance-service during check-in.
    Response time target: <200ms for 500 employees.
    """
    image_bytes = await file.read()

    # Liveness check
    is_live, liveness_score = face_model.detect_liveness(image_bytes)
    if not is_live:
        return {
            "matched": False,
            "reason": "liveness_failed",
            "livenessScore": liveness_score
        }

    # Extract embedding
    embedding, error = face_model.detect_and_encode(image_bytes)
    if embedding is None:
        return {"matched": False, "reason": error}

    # Find closest match
    employee_id, similarity = embedding_store.find_closest(embedding)

    if similarity > 0.85:
        return {
            "matched": True,
            "employeeId": employee_id,
            "confidence": round(similarity, 4),
            "livenessScore": round(liveness_score, 4),
            "method": "FACE_RECOGNITION"
        }
    elif similarity > 0.5:
        return {
            "matched": False,
            "reason": "low_confidence",
            "bestScore": round(similarity, 4),
            "message": "Face detected but confidence too low. Try again or contact HR."
        }
    else:
        return {
            "matched": False,
            "reason": "no_match",
            "bestScore": round(similarity, 4)
        }

@router.delete("/{employee_id}")
async def delete_enrollment(employee_id: str):
    """Remove face enrollment. Called on employee termination."""
    embedding_store.delete(employee_id)
    return {"deleted": True, "employeeId": employee_id}

@router.get("/status")
async def biometric_status():
    return {
        "enrolledCount": embedding_store.count,
        "modelName": "buffalo_l (ArcFace)",
        "embeddingDimension": 512,
        "similarityThreshold": 0.85
    }
```

### FastAPI Lifespan (load everything at startup)
```python
# app/main.py
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Load on startup
    global face_model, embedding_store, payroll_model, attendance_model
    from app.models.face_recognition_model import FaceRecognitionModel
    from app.store.embedding_store import EmbeddingStore

    face_model = FaceRecognitionModel()
    embedding_store = EmbeddingStore()
    # ... load other models
    yield
    # Cleanup on shutdown (nothing needed)

app = FastAPI(title="HRMS AI/ML Service", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

from app.routers import biometric, payroll_anomaly, attendance_fraud, attrition, forecast
app.include_router(biometric.router)
app.include_router(payroll_anomaly.router)
app.include_router(attendance_fraud.router)
app.include_router(attrition.router)
app.include_router(forecast.router)

@app.get("/v1/ai/health")
async def health():
    return {
        "status": "healthy",
        "enrolledFaces": embedding_store.count,
        "models": ["face_recognition", "payroll_anomaly", "attendance_fraud", "attrition"]
    }
```

## Payroll Anomaly — Isolation Forest (unchanged)
**Features (14):** earned_salary, net_salary, allowances, deductions, work_ratio, salary_zscore, months_employed, allowance_ratio, deduction_ratio, ipn_deviation, opv_deviation, is_new_employee, previous_month_salary, salary_change_pct
**Thresholds:** <0.3 NORMAL, 0.3-0.65 WARNING, >0.65 REVIEW

## Attendance Fraud — Isolation Forest (behavioral, runs AFTER face match)
**Features (10):** time_diff_minutes, location_distance_meters, device_switch, hour_of_day, day_of_week, historical_checkin_hour_avg/std, checkins_today_count, days_since_last_checkin, is_remote_worker
**Thresholds:** <0.3 ALLOW, 0.3-0.65 FLAG, >0.65 BLOCK

## Attrition — XGBoost (unchanged)
**Features (14):** tenure, salary ratio, growth rate, promotion gap, leave usage, sick frequency, late rate, overtime, team turnover, manager changes, age, dept tenure, performance, engagement

## CRITICAL: Non-Blocking Pattern
AI service failures must NEVER block business operations:
```java
try { result = aiClient.verify(photo); }
catch (Exception e) { log.warn("AI unavailable"); /* fallback to manual check-in */ }
```

## Dockerfile
```dockerfile
FROM python:3.11-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1-mesa-glx libglib2.0-0 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .
EXPOSE 8086
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8086"]
```

## Docker Volume Mounts
```yaml
ai-ml-service:
  volumes:
    - hrms-uploads:/data/hrms/uploads:ro       # read enrollment photos
    - hrms-ai-models:/data/hrms/ai-models       # read/write embeddings + model files
```

## Performance Targets
- Face verification: <200ms per request (500 embeddings comparison <1ms, face detection ~150ms)
- Payroll anomaly: <100ms
- Attendance fraud: <100ms
- Enrollment: <2s for 3-5 photos
