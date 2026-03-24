const SESSION_KEY = "ems_current_user";
const TOKEN_KEY = "token";

export function getCurrentUser() {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    const user = raw ? JSON.parse(raw) : null;
    return user && user.email ? user : null;
  } catch {
    return null;
  }
}

export function setCurrentUser(user) {
  if (!user) {
    clearCurrentUser();
    return;
  }
  localStorage.setItem(SESSION_KEY, JSON.stringify(user));
}

export function clearCurrentUser() {
  localStorage.removeItem(SESSION_KEY);
  localStorage.removeItem(TOKEN_KEY);
}

export function setToken(token) {
  if (!token) return;
  localStorage.setItem(TOKEN_KEY, token);
}

