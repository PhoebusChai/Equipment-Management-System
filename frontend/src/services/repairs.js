import http from "./http";

export const REPAIR_STATUS = {
  SUBMITTED: "submitted",
  CONFIRMED: "confirmed",
  IN_PROGRESS: "in_progress",
  RESOLVED: "resolved"
};

function fromApiStatus(status) {
  const s = String(status || "").toUpperCase();
  if (s === "SUBMITTED") return REPAIR_STATUS.SUBMITTED;
  if (s === "CONFIRMED") return REPAIR_STATUS.CONFIRMED;
  if (s === "IN_REPAIR") return REPAIR_STATUS.IN_PROGRESS;
  if (s === "FINISHED" || s === "REJECTED") return REPAIR_STATUS.RESOLVED;
  return REPAIR_STATUS.SUBMITTED;
}

function toApiType(type) {
  return String(type || "").toUpperCase() === "LAB" ? "LAB" : "DEVICE";
}

function mapRepair(item) {
  return {
    id: item.id,
    requestNo: item.requestNo,
    createdByUserId: item.reporterId,
    handlerUserId: item.confirmerId,
    resourceType: String(item.resourceType || "").toLowerCase(),
    resourceId: item.resourceId,
    resourceName: `${String(item.resourceType || "").toLowerCase() === "lab" ? "实验室" : "设备"} #${item.resourceId}`,
    description: item.faultDesc || "",
    status: fromApiStatus(item.status),
    createdAt: item.createdAt,
    updatedAt: item.finishedAt || item.confirmedAt || item.createdAt
  };
}

export async function listRepairsApi(handlerUserId) {
  const res = await http.get("/repairs", { params: handlerUserId ? { handlerId: handlerUserId } : undefined });
  return (res.data || []).map(mapRepair);
}

export async function createRepairApi(payload) {
  const res = await http.post("/repairs", {
    reporterId: payload.createdByUserId,
    resourceType: toApiType(payload.resourceType),
    resourceId: payload.resourceId,
    faultDesc: payload.description || "",
    urgencyLevel: "NORMAL"
  });
  return mapRepair(res.data);
}

export async function confirmRepairApi(id, confirmerId) {
  const res = await http.patch(`/repairs/${id}/confirm`, { confirmerId });
  return mapRepair(res.data);
}

export async function startRepairApi(id, confirmerId) {
  const res = await http.patch(`/repairs/${id}/start`, { confirmerId });
  return mapRepair(res.data);
}

export async function finishRepairApi(id, confirmerId) {
  const res = await http.patch(`/repairs/${id}/finish`, { confirmerId });
  return mapRepair(res.data);
}

