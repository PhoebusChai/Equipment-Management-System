import http from "./http";

export async function listNoticesApi() {
  const res = await http.get("/notices");
  return (res.data || []).map((item) => ({
    id: item.id,
    type: String(item.noticeType || "SYSTEM").toUpperCase(),
    targetType: item.targetType,
    title: item.title,
    content: item.content,
    publisherId: item.publisherId,
    publishTime: item.publishTime,
    status: item.status
  }));
}

