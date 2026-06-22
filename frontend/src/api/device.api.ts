import axios from "axios";
import type { Device } from "../utils";

const http = axios.create({ baseURL: "http://localhost:8080/api" });

export const deviceApi = {
  getAll: () => http.get<Device[]>("/devices").then((r) => r.data),

  create: (data: Partial<Device>) =>
    http.post<Device>("/devices", data).then((r) => r.data),

  update: (id: number, data: Partial<Device>) =>
    http.put<Device>(`/devices/${id}`, data).then((r) => r.data),

  delete: (id: number) => http.delete(`/devices/${id}`),

  sendCommand: (deviceId: string, command: Record<string, unknown>) =>
    http.post(`/devices/${deviceId}/command`, command),
};
