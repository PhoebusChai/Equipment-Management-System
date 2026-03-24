import http from "./http";
import { setCurrentUser, setToken } from "./session";

export async function loginApi(payload) {
  const res = await http.post("/auth/login", payload);
  const data = res.data || {};
  const user = { id: data.userId, email: data.email, role: String(data.roleCode || "").toLowerCase(), roleCode: data.roleCode };
  setCurrentUser(user);
  setToken(data.token || "");
  return user;
}

export async function registerApi(payload) {
  const res = await http.post("/auth/register", payload);
  const data = res.data || {};
  const user = { id: data.userId, email: data.email, role: String(data.roleCode || "").toLowerCase(), roleCode: data.roleCode };
  setCurrentUser(user);
  setToken(data.token || "");
  return user;
}

export async function resetPasswordApi(payload) {
  const res = await http.post("/auth/reset-password", payload);
  return res.data;
}

