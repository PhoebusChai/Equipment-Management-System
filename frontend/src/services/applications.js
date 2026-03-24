import http from "./http";

export const APPLICATION_STATUS = {
  SUBMITTED: "submitted",
  APPROVED: "approved",
  REJECTED: "rejected"
};

function fromApiStatus(status) {
  const s = String(status || "").toUpperCase();
  if (s === "APPROVED") return APPLICATION_STATUS.APPROVED;
  if (s === "REJECTED") return APPLICATION_STATUS.REJECTED;
  return APPLICATION_STATUS.SUBMITTED;
}

function toFrontendType(type) {
  const t = String(type || "").toUpperCase();
  if (t === "LAB_APPLY") return "lab_apply";
  if (t === "DEVICE_APPLY") return "device_apply";
  if (t === "SCRAP_APPLY") return "scrap_apply";
  return "unknown";
}

function mapApplication(item) {
  return {
    id: item.id,
    type: toFrontendType(item.type),
    createdByUserId: item.applicantId,
    title: item.title || "",
    detail: item.detail || "",
    openDateStart: item.openDateStart || "",
    openDateEnd: item.openDateEnd || "",
    dailyStartTime: item.dailyStartTime || "",
    dailyEndTime: item.dailyEndTime || "",
    slotPreset: item.slotPreset || "",
    status: fromApiStatus(item.status),
    reviewedByUserId: item.reviewerId,
    reviewNote: item.reviewComment || "",
    createdAt: item.createdAt || item.reviewedAt || new Date().toISOString(),
    updatedAt: item.reviewedAt || item.createdAt || new Date().toISOString()
  };
}

export async function listApplicationsApi() {
  const res = await http.get("/applications");
  return (res.data || []).map(mapApplication);
}

export async function createLabApplicationApi(payload) {
  const res = await http.post("/applications/lab", payload);
  return mapApplication(res.data);
}

export async function createDeviceApplicationApi(payload) {
  const res = await http.post("/applications/device", payload);
  return mapApplication(res.data);
}

export async function approveApplicationApi(type, id, payload) {
  const res = await http.patch(`/applications/${type}/${id}/approve`, payload);
  return mapApplication(res.data);
}

export async function rejectApplicationApi(type, id, payload) {
  const res = await http.patch(`/applications/${type}/${id}/reject`, payload);
  return mapApplication(res.data);
}

