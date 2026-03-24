import http from "./http";

function mapReview(x) {
  if (!x) return null;
  return {
    id: x.id,
    bookingId: x.bookingId,
    resourceType: String(x.resourceType || "").toLowerCase(),
    resourceId: x.resourceId,
    resourceName: x.resourceName || "",
    userId: x.userId,
    userName: x.userName || "",
    rating: Number(x.rating || 0),
    content: x.content || "",
    createdAt: x.createdAt || "",
    updatedAt: x.updatedAt || ""
  };
}

export async function upsertReviewApi(payload) {
  const res = await http.post("/reviews/upsert", payload);
  return mapReview(res.data);
}

export async function listReviewsByResourceApi(resourceType, resourceId) {
  const res = await http.get("/reviews/resource", {
    params: {
      resourceType: String(resourceType || "").toUpperCase(),
      resourceId
    }
  });
  return (res.data || []).map(mapReview);
}

export async function getReviewByBookingApi(bookingId) {
  const res = await http.get(`/reviews/booking/${bookingId}`);
  return mapReview(res.data);
}

export async function listTeacherReviewsApi({ teacherId, resourceType } = {}) {
  const params = { teacherId };
  if (resourceType) params.resourceType = String(resourceType).toUpperCase();
  const res = await http.get("/reviews/teacher", { params });
  return (res.data || []).map(mapReview);
}

export async function listAdminReviewsApi({ resourceType, rating } = {}) {
  const params = {};
  if (resourceType) params.resourceType = String(resourceType).toUpperCase();
  if (rating != null && rating !== "") params.rating = Number(rating);
  const res = await http.get("/reviews/admin", { params });
  return (res.data || []).map(mapReview);
}
