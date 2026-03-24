import http from "./http";

export const USER_STATUS = {
  ACTIVE: "active",
  DISABLED: "disabled"
};

function fromApiStatus(status) {
  return String(status || "").toUpperCase() === "LOCKED" ? USER_STATUS.DISABLED : USER_STATUS.ACTIVE;
}

function mapUser(x) {
  return {
    id: x.id,
    email: x.email,
    realName: x.realName,
    role: String(x.role || "").toLowerCase(),
    status: fromApiStatus(x.status),
    createdAt: x.createdAt,
    avatarUrl: x.avatarUrl || ""
  };
}

export async function listUsersApi() {
  const res = await http.get("/users");
  return (res.data || []).map(mapUser);
}

export async function createUserApi(payload) {
  const res = await http.post("/users", {
    email: payload.email,
    realName: payload.realName,
    role: payload.role
  });
  return mapUser(res.data);
}

export async function updateUserStatusApi(id, status) {
  const apiStatus = status === USER_STATUS.DISABLED ? "LOCKED" : "ACTIVE";
  const res = await http.patch(`/users/${id}/status`, { status: apiStatus });
  return mapUser(res.data);
}

export async function updateUserApi(id, payload) {
  const res = await http.patch(`/users/${id}`, {
    realName: payload.realName,
    role: payload.role
  });
  return mapUser(res.data);
}

export async function resetUserPasswordApi(id, newPassword = "123456") {
  const res = await http.patch(`/users/${id}/reset-password`, { newPassword });
  return mapUser(res.data);
}

