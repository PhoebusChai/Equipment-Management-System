import http from "./http";

export const BOOKING_STATUS = {
  PENDING: "pending",
  APPROVED: "approved",
  REJECTED: "rejected",
  CANCELLED: "cancelled",
  COMPLETED: "completed"
};

function toApiStatus(status) {
  const s = String(status || "").toLowerCase();
  if (s === BOOKING_STATUS.APPROVED) return "APPROVED";
  if (s === BOOKING_STATUS.REJECTED) return "REJECTED";
  if (s === BOOKING_STATUS.CANCELLED) return "CANCELLED";
  if (s === BOOKING_STATUS.COMPLETED) return "FINISHED";
  return "PENDING";
}

function fromApiStatus(status) {
  const s = String(status || "").toUpperCase();
  if (s === "APPROVED" || s === "IN_USE") return BOOKING_STATUS.APPROVED;
  if (s === "REJECTED") return BOOKING_STATUS.REJECTED;
  if (s === "CANCELLED") return BOOKING_STATUS.CANCELLED;
  if (s === "FINISHED") return BOOKING_STATUS.COMPLETED;
  return BOOKING_STATUS.PENDING;
}

function toApiType(type) {
  return String(type || "").toUpperCase() === "LAB" ? "LAB" : "DEVICE";
}

function fromApiType(type) {
  return String(type || "").toUpperCase() === "LAB" ? "lab" : "device";
}

function mapBooking(x) {
  return {
    id: x.id,
    bookingNo: x.bookingNo,
    createdByUserId: x.userId,
    userId: x.userId,
    resourceType: fromApiType(x.resourceType),
    resourceId: x.resourceId,
    resourceName: x.resourceName || `${fromApiType(x.resourceType) === "lab" ? "实验室" : "设备"} #${x.resourceId}`,
    bookingScope: x.bookingScope,
    startAt: x.startTime,
    endAt: x.endTime,
    startTime: x.startTime,
    endTime: x.endTime,
    durationMinutes: x.durationMinutes,
    status: fromApiStatus(x.status),
    approverId: x.approverId,
    reviewNote: x.rejectReason || "",
    purpose: x.bookingScope || "",
    participants: 1,
    isEmergency: false,
    createdAt: x.createdAt || x.startTime,
    updatedAt: x.updatedAt || x.endTime
  };
}

export async function listBookingsApi({ userId, approverId } = {}) {
  const params = {};
  if (userId) params.userId = userId;
  if (approverId) params.approverId = approverId;
  const res = await http.get("/bookings", { params });
  return (res.data || []).map(mapBooking);
}

export async function checkBookingConflictApi(payload) {
  const res = await http.post("/bookings/conflict-check", {
    resourceType: toApiType(payload.resourceType),
    resourceId: payload.resourceId,
    startTime: payload.startAt,
    endTime: payload.endAt
  });
  return res.data;
}

export async function createBookingApi(payload) {
  const res = await http.post("/bookings", {
    userId: payload.createdByUserId,
    resourceType: toApiType(payload.resourceType),
    resourceId: payload.resourceId,
    bookingScope: payload.purpose || "NORMAL",
    startTime: payload.startAt,
    endTime: payload.endAt
  });
  return mapBooking(res.data);
}

export async function updateBookingStatusApi(id, status, patch = {}) {
  const apiStatus = toApiStatus(status);
  if (apiStatus === "APPROVED") {
    const res = await http.patch(`/bookings/${id}/approve`, { approverId: patch.approverId, reason: patch.reviewNote || "" });
    return mapBooking(res.data);
  }
  if (apiStatus === "REJECTED") {
    const res = await http.patch(`/bookings/${id}/reject`, { approverId: patch.approverId, reason: patch.reviewNote || "" });
    return mapBooking(res.data);
  }
  if (apiStatus === "FINISHED") {
    const res = await http.patch(`/bookings/${id}/finish`, { approverId: patch.approverId, reason: patch.reviewNote || "" });
    return mapBooking(res.data);
  }
  throw new Error("当前后端暂不支持该状态变更");
}

export async function cancelBookingApi(id, payload) {
  const res = await http.patch(`/bookings/${id}/cancel`, payload);
  return mapBooking(res.data);
}

