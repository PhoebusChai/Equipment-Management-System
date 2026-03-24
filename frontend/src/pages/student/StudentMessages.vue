<script setup>
import { computed, onMounted, ref } from "vue";
import { listBookingsApi } from "../../services/bookings";
import { listNoticesApi } from "../../services/notices";
import { getCurrentUser } from "../../services/session";

const notices = ref([]);
const bookingNotices = ref([]);

const visibleNotices = computed(() =>
  notices.value
    .filter((n) => n.status === "PUBLISHED")
    .slice()
    .sort((a, b) => new Date(b.publishTime).getTime() - new Date(a.publishTime).getTime())
);

const visibleBookingNotices = computed(() =>
  bookingNotices.value
    .slice()
    .sort((a, b) => new Date(b.time).getTime() - new Date(a.time).getTime())
);

function bookingStatusText(status) {
  if (status === "approved") return "预约已确认";
  if (status === "rejected") return "预约已驳回";
  if (status === "cancelled") return "预约已取消";
  if (status === "completed") return "预约已完成";
  return "预约状态更新";
}

function typeText(type) {
  if (type === "SYSTEM") return "系统";
  if (type === "LAB") return "实验室";
  if (type === "DEVICE") return "设备";
  return "通知";
}

function typeClass(type) {
  if (type === "SYSTEM") return "bg-brand-600 text-white";
  if (type === "LAB") return "bg-indigo-600 text-white";
  if (type === "DEVICE") return "bg-green-600 text-white";
  return "bg-slate-600 text-white";
}

onMounted(async () => {
  notices.value = await listNoticesApi();
  const u = getCurrentUser();
  if (u?.id) {
    const list = await listBookingsApi({ userId: u.id });
    bookingNotices.value = (list || [])
      .filter((b) => ["approved", "rejected", "cancelled", "completed"].includes(b.status))
      .map((b) => ({
        id: `bk-${b.id}`,
        title: `${bookingStatusText(b.status)}：${b.resourceName}`,
        content: b.reviewNote ? `原因/备注：${b.reviewNote}` : `时段：${b.startAt} ~ ${b.endAt}`,
        time: b.updatedAt || b.createdAt || b.endAt || b.startAt
      }));
  }
});
</script>

<template>
  <div class="flex h-full flex-col bg-white">
    <div class="border-b border-slate-200 px-6 py-5">
      <h2 class="text-2xl font-semibold text-slate-900">消息中心</h2>
      <p class="mt-1 text-sm text-slate-600">系统通知、实验室公告、设备公告</p>
    </div>

    <div class="flex-1 overflow-auto p-6">
      <div class="mx-auto max-w-3xl space-y-3">
        <div class="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <div class="text-sm font-semibold text-slate-900">预约结果提醒</div>
          <p class="mt-1 text-xs text-slate-500">展示最近的预约状态变更（确认/驳回/取消/完成）</p>
          <div class="mt-3 space-y-2">
            <div
              v-for="item in visibleBookingNotices"
              :key="item.id"
              class="rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm"
            >
              <div class="font-medium text-slate-900">{{ item.title }}</div>
              <div class="mt-1 text-xs text-slate-600">{{ item.content }}</div>
              <div class="mt-1 text-[11px] text-slate-400">{{ item.time }}</div>
            </div>
            <div
              v-if="visibleBookingNotices.length === 0"
              class="rounded-lg border border-dashed border-slate-200 bg-slate-50 p-6 text-center text-sm text-slate-500"
            >
              暂无预约结果提醒
            </div>
          </div>
        </div>

        <div
          v-for="item in visibleNotices"
          :key="item.id"
          class="rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm shadow-sm transition hover:shadow-md"
        >
          <div class="flex items-center gap-2">
            <span class="rounded px-2 py-0.5 text-xs font-medium" :class="typeClass(item.type)">{{ typeText(item.type) }}</span>
            <span class="text-slate-800">{{ item.title }}</span>
          </div>
          <p class="mt-2 text-slate-600">{{ item.content }}</p>
        </div>
        <div
          v-if="visibleNotices.length === 0"
          class="rounded-lg border border-dashed border-slate-200 bg-slate-50 p-8 text-center text-sm text-slate-500"
        >
          暂无已发布公告
        </div>
      </div>
    </div>
  </div>
</template>
