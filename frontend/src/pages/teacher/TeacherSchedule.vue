<script setup>
import { computed, ref } from "vue";
import { ElMessage } from "element-plus";
import { findUserByEmail, getCurrentUser, listBookings, updateBookingStatus } from "../../mock/db";
import { useMockDb } from "../../composables/useMockDb";

useMockDb();

const filterStatus = ref("pending"); // 默认只看待审
const keyword = ref("");

const currentUser = computed(() => getCurrentUser());
const currentDbUser = computed(() => (currentUser.value ? findUserByEmail(currentUser.value.email) : null));

function pad2(n) {
  return String(n).padStart(2, "0");
}

function formatShort(iso) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "-";
  return `${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}

const allBookings = computed(() => {
  if (!currentDbUser.value) return [];
  return listBookings({ role: "teacher", userId: currentDbUser.value.id })
    .slice()
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
});

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return allBookings.value.filter((b) => {
    const byStatus = filterStatus.value === "all" || b.status === filterStatus.value;
    const byKw =
      !kw ||
      String(b.resourceName || "").toLowerCase().includes(kw) ||
      String(b.purpose || "").toLowerCase().includes(kw) ||
      String(b.resourceType || "").toLowerCase().includes(kw);
    return byStatus && byKw;
  });
});

const reviewNoteMap = ref({});

function getNote(bookingId) {
  return reviewNoteMap.value[bookingId] || "";
}

function setNote(bookingId, val) {
  reviewNoteMap.value = { ...reviewNoteMap.value, [bookingId]: val };
}

function approve(bookingId) {
  updateBookingStatus(bookingId, "approved", { reviewNote: getNote(bookingId) });
  ElMessage.success("已通过该预约");
}

function reject(bookingId) {
  updateBookingStatus(bookingId, "rejected", { reviewNote: getNote(bookingId) });
  ElMessage.success("已驳回该预约");
}

function markCompleted(bookingId) {
  updateBookingStatus(bookingId, "completed");
  ElMessage.success("已标记为完成");
}

const statusPillClass = (status) => {
  if (status === "pending") return "bg-amber-100 text-amber-700";
  if (status === "approved") return "bg-green-100 text-green-700";
  if (status === "rejected") return "bg-rose-100 text-rose-700";
  if (status === "cancelled") return "bg-red-100 text-red-700";
  if (status === "completed") return "bg-slate-100 text-slate-700";
  return "bg-slate-100 text-slate-600";
};

const statusText = (status) => {
  if (status === "pending") return "待审核";
  if (status === "approved") return "已确认";
  if (status === "rejected") return "已驳回";
  if (status === "cancelled") return "已取消";
  if (status === "completed") return "已完成";
  return status;
};
</script>

<template>
  <div class="flex h-full flex-col bg-white">
    <div class="border-b border-slate-200 px-6 py-4">
      <h2 class="text-xl font-semibold text-slate-900">预约监管</h2>
      <p class="mt-1 text-sm text-slate-500">审批学生预约（通过/驳回），并支持标记完成。</p>
    </div>

    <div class="flex-1 overflow-auto p-6 space-y-4">
      <div class="flex flex-wrap items-center gap-3">
        <select v-model="filterStatus" class="input w-44">
          <option value="pending">仅待审核</option>
          <option value="all">全部状态</option>
          <option value="approved">已确认</option>
          <option value="rejected">已驳回</option>
          <option value="cancelled">已取消</option>
          <option value="completed">已完成</option>
        </select>
        <input v-model="keyword" class="input flex-1 min-w-[220px]" placeholder="搜索资源/用途/类型..." />
      </div>

      <div>
        <h3 class="mb-3 text-sm font-medium text-slate-700">待审与排期列表</h3>

        <div v-if="!currentDbUser" class="rounded-xl border border-slate-200 bg-slate-50 p-6 text-sm text-slate-600">
          未登录或当前账号无效，请重新登录教师账号。
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="b in filtered"
            :key="b.id"
            class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm"
          >
            <div class="flex items-start justify-between gap-4">
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-2">
                  <div class="truncate text-base font-semibold text-slate-900">{{ b.resourceName }}</div>
                  <span class="rounded-full px-2 py-0.5 text-xs font-medium" :class="statusPillClass(b.status)">
                    {{ statusText(b.status) }}
                  </span>
                  <span v-if="b.isEmergency" class="rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700">
                    紧急
                  </span>
                  <span class="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">
                    {{ b.resourceType === "lab" ? "实验室" : "设备" }}
                  </span>
                </div>

                <div class="mt-2 grid gap-2 text-sm text-slate-600 md:grid-cols-2">
                  <div>时间：{{ formatShort(b.startAt) }} - {{ formatShort(b.endAt) }}</div>
                  <div>人数：{{ b.participants }} 人</div>
                  <div class="md:col-span-2">
                    用途：<span class="text-slate-800">{{ b.purpose || "-" }}</span>
                  </div>
                </div>

                <div class="mt-3">
                  <label class="block text-xs font-medium text-slate-600">审核意见（可选）</label>
                  <input
                    class="input mt-1"
                    :value="getNote(b.id)"
                    placeholder="例如：时间合理/请提前准备/冲突需调整..."
                    @input="setNote(b.id, $event.target.value)"
                  />
                </div>
              </div>

              <div class="flex flex-col gap-2">
                <button
                  v-if="b.status === 'pending'"
                  class="btn-primary px-3 py-2 text-sm"
                  @click="approve(b.id)"
                >
                  通过
                </button>
                <button
                  v-if="b.status === 'pending'"
                  class="btn-secondary px-3 py-2 text-sm border-rose-200 text-rose-700 hover:bg-rose-50"
                  @click="reject(b.id)"
                >
                  驳回
                </button>
                <button
                  v-if="b.status === 'approved'"
                  class="btn-secondary px-3 py-2 text-sm"
                  @click="markCompleted(b.id)"
                >
                  标记完成
                </button>
              </div>
            </div>
          </div>

          <div v-if="filtered.length === 0" class="rounded-xl border border-dashed border-slate-200 bg-slate-50 p-10 text-center text-sm text-slate-500">
            暂无符合条件的预约记录
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
