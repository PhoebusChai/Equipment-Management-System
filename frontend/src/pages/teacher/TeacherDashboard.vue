<script setup>
import { computed, onMounted, ref } from "vue";
import StatCard from "../../components/StatCard.vue";
import DataTable from "../../components/DataTable.vue";
import { listBookingsApi, BOOKING_STATUS } from "../../services/bookings";
import { listLabsApi } from "../../services/resources";
import { listRepairsApi } from "../../services/repairs";
import { getCurrentUser } from "../../services/session";

const currentUser = computed(() => getCurrentUser());
const currentDbUser = computed(() => currentUser.value || null);
const repairs = ref([]);
const bookings = ref([]);
const labs = ref([]);

const teacherBookings = computed(() => {
  if (!currentDbUser.value) return [];
  return bookings.value;
});

const pendingCount = computed(() => teacherBookings.value.filter((b) => b.status === BOOKING_STATUS.PENDING).length);
const emergencyPendingCount = computed(
  () => teacherBookings.value.filter((b) => b.status === BOOKING_STATUS.PENDING && b.isEmergency).length
);

const managedLabsCount = computed(() => labs.value.length);

const repairsCount = computed(() => {
  if (!currentDbUser.value) return 0;
  return repairs.value.filter((r) => r.status !== "resolved").length;
});

const stats = computed(() => [
  { title: "待审核预约", value: pendingCount.value, desc: `含紧急预约${emergencyPendingCount.value}条` },
  { title: "在管实验室", value: managedLabsCount.value, desc: "计算机 / 生物等" },
  { title: "待处理报修", value: repairsCount.value, desc: "含已确认与维修中" }
]);

const bookingColumns = [
  { key: "resource", label: "资源" },
  { key: "time", label: "预约时间" },
  { key: "type", label: "类型" },
  { key: "status", label: "状态" }
];

function pad2(n) {
  return String(n).padStart(2, "0");
}

function formatRange(startAt, endAt) {
  const s = new Date(startAt);
  const e = new Date(endAt);
  if (Number.isNaN(s.getTime()) || Number.isNaN(e.getTime())) return "-";
  return `${pad2(s.getMonth() + 1)}-${pad2(s.getDate())} ${pad2(s.getHours())}:${pad2(s.getMinutes())}-${pad2(e.getHours())}:${pad2(e.getMinutes())}`;
}

const bookingRows = computed(() =>
  teacherBookings.value
    .filter((b) => b.status === BOOKING_STATUS.PENDING)
    .slice()
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 5)
    .map((b) => ({
      resource: b.resourceName,
      time: formatRange(b.startAt, b.endAt),
      type: b.purpose || "-",
      status: b.isEmergency ? "紧急待审核" : "待审核"
    }))
);

onMounted(async () => {
  if (!currentDbUser.value) return;
  const [repairsData, bookingData, labsData] = await Promise.all([
    listRepairsApi(currentDbUser.value.id),
    listBookingsApi({ approverId: currentDbUser.value.id }),
    listLabsApi()
  ]);
  repairs.value = repairsData;
  bookings.value = bookingData;
  labs.value = labsData;
});
</script>

<template>
  <div class="flex h-full flex-col bg-white">
    <div class="border-b border-slate-200 px-6 py-5">
      <h1 class="text-2xl font-semibold text-slate-900">教师端总览</h1>
      <p class="mt-1 text-sm text-slate-600">实验室管理、预约审核、维修工单与公告发布</p>
    </div>

    <div class="flex-1 overflow-auto p-6 space-y-6">
      <div class="grid gap-4 md:grid-cols-3">
        <StatCard v-for="item in stats" :key="item.title" :title="item.title" :value="item.value" :desc="item.desc" />
      </div>

      <div>
        <h2 class="mb-3 text-sm font-medium text-slate-700">最近待处理预约</h2>
        <DataTable :columns="bookingColumns" :rows="bookingRows" />
      </div>
    </div>
  </div>
</template>
