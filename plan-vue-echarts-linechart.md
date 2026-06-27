# Plan: Vue-ECharts LineChart with Time-Range Filter

## ✅ Check: Does `GET /api/sensor-data/device/{id}?from=&to=` exist?

**No.** Current backend only supports `?limit=N` (default 50). The repository only has `findLatestByDeviceId` with no time filtering.

---

## Changes needed

### 1. Backend — Add time-range query

**Files:**

| File | Change |
|------|--------|
| `iot-backend/.../repository/SensorDataRepository.java` | Add query `findByDeviceIdAndRecordedAtBetween` |
| `iot-backend/.../controller/SensorDataController.java` | Add `@RequestParam(required=false) LocalDateTime from, to` |

**Input:** `?from=2026-06-27T00:00:00&to=2026-06-27T23:59:59`
**Output:** Filtered sensor data list ordered by `recordedAt DESC`

### 2. Frontend — Install vue-echarts

```bash
npm install echarts vue-echarts
```

### 3. Frontend — Update API layer

**File:** `frontend/src/api/sensorData.api.ts`

Add `from`/`to` optional params to `getByDevice`.

### 4. Frontend — Create `LineChart.vue` component

**New file:** `frontend/src/components/LineChart.vue`

vue-echarts line chart wrapper accepting:
- `data: SensorData[]`
- `field: 'temperature' | 'humidity'`
- `title: string`

Uses ECharts `line` series with time X-axis, smooth curve, tooltip, and responsive sizing.

### 5. Frontend — Update `DeviceDetailView.vue`

**File:** `frontend/src/views/DeviceDetailView.vue`

| Change | Detail |
|--------|--------|
| Add time-range tabs | Buttons: **1h / 6h / 24h / 7d / Custom** |
| Add date inputs | Two `<input type="datetime-local">` for custom range |
| Wire to store | Call `fetchByDevice(deviceId, { from, to })` on tab change |
| Replace `SensorChart` | Use new `LineChart` for temperature & humidity |

### 6. Frontend — Update store

**File:** `frontend/src/stores/sensorData.store.ts`

Accept optional `from`/`to` params in `fetchByDevice` and pass to API.

### 7. Cleanup (optional)

**File:** `frontend/src/components/SensorChart.vue` — keep or remove (no longer used)

---

## Data flow

```
User clicks "6h" tab
  → DeviceDetailView sets from=now-6h, to=now
  → sensorDataStore.fetchByDevice(id, { from, to })
  → sensorDataApi.getByDevice(id, { from, to })
  → GET /api/sensor-data/device/{id}?from=...&to=...
  → SensorDataRepository query with BETWEEN
  → Response → store → LineChart.vue renders 2 charts
```

---

## Files changed/modified summary

| Action | File |
|--------|------|
| Modify | `iot-backend/.../SensorDataRepository.java` |
| Modify | `iot-backend/.../SensorDataController.java` |
| Install | `echarts` + `vue-echarts` in `frontend/package.json` |
| Modify | `frontend/src/api/sensorData.api.ts` |
| Modify | `frontend/src/stores/sensorData.store.ts` |
| Create | `frontend/src/components/LineChart.vue` |
| Modify | `frontend/src/views/DeviceDetailView.vue` |
| Optional | Delete `SensorChart.vue` |
