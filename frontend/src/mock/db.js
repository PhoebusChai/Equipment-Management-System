import { createSeedData } from "./seed";

const STORAGE_KEY = "ems_mock_db_v1";
const SUBSCRIBERS_KEY = "__ems_mock_db_subscribers__";

// 前端统一状态枚举（用于页面与 mock 逻辑）
export const USER_STATUS = {
  ACTIVE: "active",
  DISABLED: "disabled"
};

export const BOOKING_STATUS = {
  PENDING: "pending",
  APPROVED: "approved",
  REJECTED: "rejected",
  CANCELLED: "cancelled",
  COMPLETED: "completed"
};

export const REPAIR_STATUS = {
  SUBMITTED: "submitted",
  CONFIRMED: "confirmed",
  IN_PROGRESS: "in_progress",
  RESOLVED: "resolved"
};

function safeParse(json, fallback) {
  try {
    return JSON.parse(json);
  } catch {
    return fallback;
  }
}

function deepClone(obj) {
  return JSON.parse(JSON.stringify(obj));
}

function nowIso() {
  return new Date().toISOString();
}

function ensureSubscriberHub() {
  if (!window[SUBSCRIBERS_KEY]) window[SUBSCRIBERS_KEY] = new Set();
  return window[SUBSCRIBERS_KEY];
}

function notify() {
  const hub = ensureSubscriberHub();
  for (const cb of hub) cb();
}

export function subscribeDb(cb) {
  const hub = ensureSubscriberHub();
  hub.add(cb);
  return () => hub.delete(cb);
}

export function resetDb() {
  const seed = createSeedData();
  localStorage.setItem(STORAGE_KEY, JSON.stringify(seed));
  notify();
  return seed;
}

export function getDb() {
  const cached = safeParse(localStorage.getItem(STORAGE_KEY) || "", null);
  if (cached && cached.version === 1) return cached;
  return resetDb();
}

export function setDb(nextDb) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(nextDb));
  notify();
}

function nextId(list) {
  return (list.reduce((max, item) => Math.max(max, Number(item.id) || 0), 0) || 0) + 1;
}

export function getCurrentUser() {
  const raw = localStorage.getItem("ems_current_user");
  const user = safeParse(raw || "{}", {});
  return user && user.email ? user : null;
}

export function setCurrentUser(user) {
  if (!user) {
    localStorage.removeItem("ems_current_user");
    return;
  }
  localStorage.setItem("ems_current_user", JSON.stringify({ email: user.email, role: user.role }));
}

export function clearCurrentUser() {
  localStorage.removeItem("ems_current_user");
}

export function authenticateUser(email, password) {
  const normalized = String(email || "").trim().toLowerCase();
  const pwd = String(password || "");
  const user = listUsers().find((u) => String(u.email || "").toLowerCase() === normalized);
  if (!user) throw new Error("账号不存在");
  if (user.status !== USER_STATUS.ACTIVE) throw new Error("账号已被禁用，请联系管理员");
  if (user.password !== pwd) throw new Error("密码错误");
  return user;
}

export function findUserByEmail(email) {
  const db = getDb();
  return db.users.find((u) => u.email === email) || null;
}

export function listUsers() {
  return getDb().users.slice().sort((a, b) => (a.id || 0) - (b.id || 0));
}

export function createUser(payload) {
  const db = deepClone(getDb());
  const exists = db.users.some((u) => u.email === payload.email);
  if (exists) throw new Error("邮箱已存在");
  const now = nowIso();
  const user = {
    id: nextId(db.users),
    email: payload.email,
    password: payload.password || "123456",
    role: payload.role,
    realName: payload.realName || payload.email,
    status: payload.status || "active",
    createdAt: now,
    updatedAt: now
  };
  db.users.push(user);
  setDb(db);
  return user;
}

export function updateUser(userId, patch) {
  const db = deepClone(getDb());
  const idx = db.users.findIndex((u) => u.id === userId);
  if (idx < 0) throw new Error("用户不存在");
  db.users[idx] = { ...db.users[idx], ...patch, updatedAt: nowIso() };
  setDb(db);
  return db.users[idx];
}

export function listLabs() {
  return getDb().labs.slice();
}

export function listDevices() {
  return getDb().devices.slice();
}

export function getResource(resourceType, resourceId) {
  const db = getDb();
  if (resourceType === "lab") return db.labs.find((x) => x.id === resourceId) || null;
  if (resourceType === "device") return db.devices.find((x) => x.id === resourceId) || null;
  return null;
}

export function updateResourceStatus(resourceType, resourceId, status) {
  const db = deepClone(getDb());
  const list = resourceType === "lab" ? db.labs : db.devices;
  const idx = list.findIndex((x) => x.id === resourceId);
  if (idx < 0) throw new Error("资源不存在");
  list[idx] = { ...list[idx], status, updatedAt: nowIso() };
  setDb(db);
  return list[idx];
}

function overlaps(aStart, aEnd, bStart, bEnd) {
  return aStart < bEnd && bStart < aEnd;
}

export function checkBookingConflict({ resourceType, resourceId, startAt, endAt, excludeBookingId }) {
  const db = getDb();
  const start = new Date(startAt).getTime();
  const end = new Date(endAt).getTime();
  if (!Number.isFinite(start) || !Number.isFinite(end) || start >= end) {
    return { hasConflict: true, conflicts: [], message: "时间范围无效" };
  }

  const conflicts = db.bookings
    .filter((b) => (excludeBookingId ? b.id !== excludeBookingId : true))
    .filter((b) => b.resourceType === resourceType && b.resourceId === resourceId)
    .filter((b) => ["pending", "approved"].includes(b.status))
    .filter((b) => overlaps(start, end, new Date(b.startAt).getTime(), new Date(b.endAt).getTime()))
    .map((b) => ({
      bookingId: b.id,
      startAt: b.startAt,
      endAt: b.endAt,
      status: b.status
    }));

  return {
    hasConflict: conflicts.length > 0,
    conflicts,
    message: conflicts.length > 0 ? "检测到时间冲突" : "无冲突"
  };
}

export function listBookings({ role, userId }) {
  const db = getDb();
  if (role === "student") return db.bookings.filter((b) => b.createdByUserId === userId);
  if (role === "teacher") return db.bookings.filter((b) => b.reviewerUserId === userId);
  if (role === "admin") return db.bookings.slice();
  return db.bookings.slice();
}

export function getBookingById(bookingId) {
  const db = getDb();
  return db.bookings.find((b) => b.id === bookingId) || null;
}

export function createBooking(payload) {
  const db = deepClone(getDb());
  const now = nowIso();

  const booking = {
    id: nextId(db.bookings),
    createdByUserId: payload.createdByUserId,
    resourceType: payload.resourceType,
    resourceId: payload.resourceId,
    resourceName: payload.resourceName,
    startAt: payload.startAt,
    endAt: payload.endAt,
    purpose: payload.purpose || "",
    participants: payload.participants || 1,
    isEmergency: !!payload.isEmergency,
    status: "pending",
    reviewerUserId: payload.reviewerUserId || 2,
    reviewNote: "",
    createdAt: now,
    updatedAt: now
  };

  // 冲突：非紧急预约不允许；紧急允许但保留冲突信息给老师处理
  const conflict = checkBookingConflict({
    resourceType: booking.resourceType,
    resourceId: booking.resourceId,
    startAt: booking.startAt,
    endAt: booking.endAt
  });

  if (conflict.hasConflict && !booking.isEmergency) {
    throw new Error("存在时间冲突，请选择其他时段或使用紧急预约");
  }

  db.bookings.push(booking);
  setDb(db);
  return { booking, conflict };
}

export function updateBookingStatus(bookingId, status, patch = {}) {
  const db = deepClone(getDb());
  const idx = db.bookings.findIndex((b) => b.id === bookingId);
  if (idx < 0) throw new Error("预约不存在");

  db.bookings[idx] = { ...db.bookings[idx], status, ...patch, updatedAt: nowIso() };

  // 简化资源状态联动：批准后标记 booked，取消/驳回/完成后尝试恢复 available（若无其他有效预约）
  const b = db.bookings[idx];
  const resourceType = b.resourceType;
  const resourceId = b.resourceId;

  if (status === "approved") {
    const list = resourceType === "lab" ? db.labs : db.devices;
    const rIdx = list.findIndex((x) => x.id === resourceId);
    if (rIdx >= 0 && list[rIdx].status !== "maintenance") {
      list[rIdx] = { ...list[rIdx], status: "booked", updatedAt: nowIso() };
    }
  }

  if (["cancelled", "rejected", "completed"].includes(status)) {
    const hasAnyActive = db.bookings
      .filter((x) => x.resourceType === resourceType && x.resourceId === resourceId)
      .some((x) => ["pending", "approved"].includes(x.status));

    const list = resourceType === "lab" ? db.labs : db.devices;
    const rIdx = list.findIndex((x) => x.id === resourceId);
    if (rIdx >= 0 && list[rIdx].status !== "maintenance" && !hasAnyActive) {
      list[rIdx] = { ...list[rIdx], status: "available", updatedAt: nowIso() };
    }
  }

  setDb(db);
  return db.bookings[idx];
}

export function listRepairs({ handlerUserId } = {}) {
  const db = getDb();
  if (handlerUserId) return db.repairs.filter((r) => r.handlerUserId === handlerUserId);
  return db.repairs.slice();
}

export function createRepair(payload) {
  const db = deepClone(getDb());
  const now = nowIso();
  const repair = {
    id: nextId(db.repairs),
    createdByUserId: payload.createdByUserId,
    resourceType: payload.resourceType,
    resourceId: payload.resourceId,
    resourceName: payload.resourceName,
    description: payload.description || "",
    status: "submitted",
    handlerUserId: payload.handlerUserId || 2,
    createdAt: now,
    updatedAt: now
  };
  db.repairs.push(repair);
  setDb(db);
  return repair;
}

export function updateRepairStatus(repairId, status, patch = {}) {
  const db = deepClone(getDb());
  const idx = db.repairs.findIndex((r) => r.id === repairId);
  if (idx < 0) throw new Error("报修不存在");
  db.repairs[idx] = { ...db.repairs[idx], status, ...patch, updatedAt: nowIso() };

  const r = db.repairs[idx];
  if (status === "confirmed" || status === "in_progress") {
    const list = r.resourceType === "lab" ? db.labs : db.devices;
    const rIdx = list.findIndex((x) => x.id === r.resourceId);
    if (rIdx >= 0) list[rIdx] = { ...list[rIdx], status: "maintenance", updatedAt: nowIso() };
  }

  if (status === "resolved") {
    const list = r.resourceType === "lab" ? db.labs : db.devices;
    const rIdx = list.findIndex((x) => x.id === r.resourceId);
    if (rIdx >= 0) list[rIdx] = { ...list[rIdx], status: "available", updatedAt: nowIso() };
  }

  setDb(db);
  return db.repairs[idx];
}

export function listApplications() {
  return getDb().applications.slice().sort((a, b) => (b.id || 0) - (a.id || 0));
}

export function createApplication(payload) {
  const db = deepClone(getDb());
  const now = nowIso();
  const app = {
    id: nextId(db.applications),
    type: payload.type,
    createdByUserId: payload.createdByUserId,
    title: payload.title || "未命名申请",
    detail: payload.detail || "",
    status: "submitted",
    reviewedByUserId: null,
    reviewNote: "",
    createdAt: now,
    updatedAt: now
  };
  db.applications.push(app);
  setDb(db);
  return app;
}

export function updateApplicationStatus(appId, status, patch = {}) {
  const db = deepClone(getDb());
  const idx = db.applications.findIndex((a) => a.id === appId);
  if (idx < 0) throw new Error("申请单不存在");
  db.applications[idx] = { ...db.applications[idx], status, ...patch, updatedAt: nowIso() };
  setDb(db);
  return db.applications[idx];
}

// ------------------------------
// 前端字段 <-> 后端 SQL 字段适配层
// ------------------------------

const BOOKING_STATUS_TO_SQL = {
  [BOOKING_STATUS.PENDING]: "PENDING",
  [BOOKING_STATUS.APPROVED]: "APPROVED",
  [BOOKING_STATUS.REJECTED]: "REJECTED",
  [BOOKING_STATUS.CANCELLED]: "CANCELLED",
  [BOOKING_STATUS.COMPLETED]: "FINISHED"
};

const BOOKING_STATUS_FROM_SQL = {
  PENDING: BOOKING_STATUS.PENDING,
  APPROVED: BOOKING_STATUS.APPROVED,
  REJECTED: BOOKING_STATUS.REJECTED,
  CANCELLED: BOOKING_STATUS.CANCELLED,
  IN_USE: BOOKING_STATUS.APPROVED,
  FINISHED: BOOKING_STATUS.COMPLETED
};

const REPAIR_STATUS_TO_SQL = {
  [REPAIR_STATUS.SUBMITTED]: "SUBMITTED",
  [REPAIR_STATUS.CONFIRMED]: "CONFIRMED",
  [REPAIR_STATUS.IN_PROGRESS]: "IN_REPAIR",
  [REPAIR_STATUS.RESOLVED]: "FINISHED"
};

const REPAIR_STATUS_FROM_SQL = {
  SUBMITTED: REPAIR_STATUS.SUBMITTED,
  CONFIRMED: REPAIR_STATUS.CONFIRMED,
  IN_REPAIR: REPAIR_STATUS.IN_PROGRESS,
  FINISHED: REPAIR_STATUS.RESOLVED,
  REJECTED: REPAIR_STATUS.RESOLVED
};

const USER_STATUS_TO_SQL = {
  [USER_STATUS.ACTIVE]: "ACTIVE",
  [USER_STATUS.DISABLED]: "LOCKED"
};

const USER_STATUS_FROM_SQL = {
  ACTIVE: USER_STATUS.ACTIVE,
  LOCKED: USER_STATUS.DISABLED
};

export function toSqlBookingStatus(status) {
  return BOOKING_STATUS_TO_SQL[status] || "PENDING";
}

export function fromSqlBookingStatus(status) {
  return BOOKING_STATUS_FROM_SQL[status] || BOOKING_STATUS.PENDING;
}

export function toSqlRepairStatus(status) {
  return REPAIR_STATUS_TO_SQL[status] || "SUBMITTED";
}

export function fromSqlRepairStatus(status) {
  return REPAIR_STATUS_FROM_SQL[status] || REPAIR_STATUS.SUBMITTED;
}

export function toSqlUserStatus(status) {
  return USER_STATUS_TO_SQL[status] || "ACTIVE";
}

export function fromSqlUserStatus(status) {
  return USER_STATUS_FROM_SQL[status] || USER_STATUS.ACTIVE;
}

export function toSqlUser(user) {
  return {
    id: user.id,
    email: user.email,
    password_hash: user.password,
    role_code: user.role,
    real_name: user.realName,
    status: toSqlUserStatus(user.status),
    created_at: user.createdAt,
    updated_at: user.updatedAt
  };
}

export function fromSqlUser(row) {
  return {
    id: row.id,
    email: row.email,
    password: row.password_hash,
    role: row.role_code,
    realName: row.real_name,
    status: fromSqlUserStatus(row.status),
    createdAt: row.created_at,
    updatedAt: row.updated_at
  };
}

export function toSqlBooking(booking) {
  return {
    id: booking.id,
    user_id: booking.createdByUserId,
    booking_scope: booking.resourceType === "lab" ? "LAB" : "DEVICE",
    lab_id: booking.resourceType === "lab" ? booking.resourceId : null,
    device_id: booking.resourceType === "device" ? booking.resourceId : null,
    start_time: booking.startAt,
    end_time: booking.endAt,
    duration_minutes: Math.max(
      0,
      Math.floor((new Date(booking.endAt).getTime() - new Date(booking.startAt).getTime()) / 60000)
    ),
    status: toSqlBookingStatus(booking.status),
    emergency_flag: booking.isEmergency ? 1 : 0,
    review_user_id: booking.reviewerUserId || null,
    review_time: booking.status === BOOKING_STATUS.PENDING ? null : booking.updatedAt || booking.createdAt,
    reject_reason: booking.status === BOOKING_STATUS.REJECTED ? booking.reviewNote || "" : null,
    created_at: booking.createdAt,
    updated_at: booking.updatedAt
  };
}

export function toSqlRepairRequest(repair) {
  return {
    id: repair.id,
    reporter_id: repair.createdByUserId,
    lab_id: repair.resourceType === "lab" ? repair.resourceId : null,
    device_id: repair.resourceType === "device" ? repair.resourceId : null,
    fault_desc: repair.description || "",
    status: toSqlRepairStatus(repair.status),
    confirmer_id: repair.handlerUserId || null,
    confirm_time: [REPAIR_STATUS.CONFIRMED, REPAIR_STATUS.IN_PROGRESS, REPAIR_STATUS.RESOLVED].includes(repair.status)
      ? repair.updatedAt
      : null,
    created_at: repair.createdAt,
    updated_at: repair.updatedAt
  };
}

