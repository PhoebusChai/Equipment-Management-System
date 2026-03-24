import http from "./http";

export function fromApiResourceStatus(status) {
  const s = String(status || "").toUpperCase();
  if (s === "AVAILABLE") return "available";
  if (s === "BOOKED" || s === "IN_USE") return "booked";
  if (s === "MAINTENANCE") return "maintenance";
  return "available";
}

export async function listLabsApi() {
  const res = await http.get("/labs");
  return (res.data || []).map((x) => ({
    id: x.id,
    labCode: x.labCode,
    name: x.name,
    type: x.type,
    building: x.building || "-",
    college: x.college || "-",
    capacity: x.capacity || 0,
    status: fromApiResourceStatus(x.status),
    description: x.description || "",
    imageUrls: Array.isArray(x.imageUrls) ? x.imageUrls : [],
    image: Array.isArray(x.imageUrls) && x.imageUrls.length ? x.imageUrls[0] : "data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20width='240'%20height='240'%3E%3Crect%20width='100%25'%20height='100%25'%20fill='%23f1f5f9'/%3E%3Ctext%20x='50%25'%20y='50%25'%20dominant-baseline='middle'%20text-anchor='middle'%20fill='%2364758b'%20font-size='14'%20font-family='Arial'%3EEMS%20Lab%3C/text%3E%3C/svg%3E",
    opening: "08:00-21:00",
    manager: "教师"
  }));
}

export async function getLabApi(id) {
  const res = await http.get(`/labs/${id}`);
  const x = res.data || {};
  const imageUrls = Array.isArray(x.imageUrls) ? x.imageUrls : [];
  return {
    id: x.id,
    labCode: x.labCode,
    name: x.name,
    type: x.type,
    building: x.building || "-",
    college: x.college || "-",
    capacity: x.capacity || 0,
    status: fromApiResourceStatus(x.status),
    description: x.description || "",
    imageUrls,
    image: imageUrls.length ? imageUrls[0] : "data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20width='480'%20height='360'%3E%3Crect%20width='100%25'%20height='100%25'%20fill='%23f1f5f9'/%3E%3Ctext%20x='50%25'%20y='50%25'%20dominant-baseline='middle'%20text-anchor='middle'%20fill='%2364758b'%20font-size='16'%20font-family='Arial'%3EEMS%20Lab%3C/text%3E%3C/svg%3E",
    opening: "08:00-21:00",
    manager: "教师"
  };
}

export async function createLabApi(payload) {
  const form = new FormData();
  form.append("labCode", payload.labCode);
  form.append("name", payload.name);
  form.append("type", payload.type);
  form.append("building", payload.building);
  if (payload.college) form.append("college", payload.college);
  if (payload.capacity != null) form.append("capacity", String(payload.capacity));
  if (payload.description) form.append("description", payload.description);
  for (const f of payload.images || []) {
    form.append("images", f);
  }

  const res = await http.post("/labs/with-images", form);
  const x = res.data || {};
  return {
    id: x.id,
    labCode: x.labCode,
    name: x.name,
    type: x.type,
    building: x.building || "-",
    college: x.college || "-",
    capacity: x.capacity || 0,
    status: fromApiResourceStatus(x.status),
    description: x.description || "",
    imageUrls: Array.isArray(x.imageUrls) ? x.imageUrls : []
  };
}

export async function listDevicesApi(labId) {
  const res = await http.get("/devices", { params: labId ? { labId } : undefined });
  return (res.data || []).map((x) => ({
    id: x.id,
    labId: x.labId,
    deviceCode: x.deviceCode,
    name: x.name,
    category: x.category || "",
    location: x.location || "-",
    status: fromApiResourceStatus(x.status)
  }));
}

export async function createDeviceApi(payload) {
  const res = await http.post("/devices", {
    labId: payload.labId,
    deviceCode: payload.deviceCode,
    name: payload.name,
    category: payload.category || "",
    location: payload.location || "",
    status: String(payload.status || "AVAILABLE").toUpperCase()
  });
  const x = res.data || {};
  return {
    id: x.id,
    labId: x.labId,
    deviceCode: x.deviceCode,
    name: x.name,
    category: x.category || "",
    location: x.location || "-",
    status: fromApiResourceStatus(x.status)
  };
}

