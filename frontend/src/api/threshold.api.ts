import { http } from './http'

export interface ThresholdAlert {
  id: number;
  thresholdId: number;
  deviceId: number;
  deviceName: string;
  fieldName: string;
  actualValue: number;
  thresholdValue: number;
  alertType: "MIN_EXCEEDED" | "MAX_EXCEEDED";
  triggeredAt: string;
  isResolved: boolean;
}

export interface Threshold {
  id: number;
  deviceId: number;
  deviceName: string;
  field: string;
  minValue?: number;
  maxValue?: number;
  createdAt: string;
  updatedAt: string;
}

export const thresholdApi = {
  // Threshold CRUD
  getByDevice: (deviceId: number) =>
    http.get<Threshold[]>(`/thresholds?deviceId=${deviceId}`).then((r) => r.data),

  create: (data: Omit<Threshold, "id" | "createdAt" | "updatedAt">) =>
    http.post<Threshold>("/thresholds", data).then((r) => r.data),

  update: (id: number, data: Partial<Threshold>) =>
    http.put<Threshold>(`/thresholds/${id}`, data).then((r) => r.data),

  delete: (id: number) => http.delete(`/thresholds/${id}`),

  // Alerts
  getAlertsByDevice: (deviceId: number) =>
    http
      .get<ThresholdAlert[]>(`/thresholds/alerts/device/${deviceId}`)
      .then((r) => r.data),

  getUnresolvedAlertsByDevice: (deviceId: number) =>
    http
      .get<ThresholdAlert[]>(`/thresholds/alerts/device/${deviceId}/unresolved`)
      .then((r) => r.data),

  getAllUnresolvedAlerts: () =>
    http
      .get<ThresholdAlert[]>(`/thresholds/alerts/unresolved`)
      .then((r) => r.data),

  resolveAlert: (alertId: number) =>
    http
      .post<ThresholdAlert>(`/thresholds/alerts/${alertId}/resolve`)
      .then((r) => r.data),
};
