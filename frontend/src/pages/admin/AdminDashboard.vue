<script setup>
import { computed } from "vue";
import StatCard from "../../components/StatCard.vue";
import DataTable from "../../components/DataTable.vue";
import { listApplications, listBookings, listDevices, listLabs, listUsers } from "../../mock/db";
import { useMockDb } from "../../composables/useMockDb";

useMockDb();

const users = computed(() => listUsers());
const apps = computed(() => listApplications());
const bookings = computed(() => listBookings({ role: "admin" }));

const pendingAppsCount = computed(() => apps.value.filter((a) => a.status === "submitted").length);

const usagePercent = computed(() => {
  const resources = listLabs().length + listDevices().length || 1;
  const set = new Set();
  for (const b of bookings.value) {
    if (["pending", "approved", "completed"].includes(b.status)) set.add(`${b.resourceType}:${b.resourceId}`);
  }
  return `${Math.round((set.size / resources) * 100)}%`;
});

const stats = computed(() => [
  {
    title: "系统用户数",
    value: users.value.length,
    desc: `学生${users.value.filter((u) => u.role === "student").length} / 教师${users.value.filter((u) => u.role === "teacher").length} / 管理员${users.value.filter((u) => u.role === "admin").length}`
  },
  { title: "待审申请", value: pendingAppsCount.value, desc: "实验室 / 设备 / 报废申请" },
  { title: "设备利用率", value: usagePercent.value, desc: "按近似口径实时计算" }
]);

const pendingColumns = [
  { key: "type", label: "申请类型" },
  { key: "detail", label: "内容" },
  { key: "time", label: "提交时间" },
  { key: "status", label: "当前状态" }
];

function pad2(n) {
  return String(n).padStart(2, "0");
}

function formatDateTime(iso) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "-";
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}

function typeText(type) {
  if (type === "lab_apply") return "实验室申请";
  if (type === "device_apply") return "设备申请";
  if (type === "scrap_apply") return "报废申请";
  return type;
}

function statusText(status) {
  if (status === "submitted") return "待审核";
  if (status === "approved") return "已通过";
  if (status === "rejected") return "已驳回";
  return status;
}

const pendingRows = computed(() =>
  apps.value
    .slice()
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 8)
    .map((a) => ({
      type: typeText(a.type),
      detail: a.title,
      time: formatDateTime(a.createdAt),
      status: statusText(a.status)
    }))
);
</script>

<template>
  <div class="flex h-full flex-col bg-white">
    <div class="border-b border-slate-200 px-6 py-5">
      <h1 class="text-2xl font-semibold text-slate-900">管理员总览</h1>
      <p class="mt-1 text-sm text-slate-600">系统管理、资产管理、报废处理与数据分析</p>
    </div>

    <div class="flex-1 overflow-auto p-6 space-y-6">
      <div class="grid gap-4 md:grid-cols-3">
        <StatCard v-for="item in stats" :key="item.title" :title="item.title" :value="item.value" :desc="item.desc" />
      </div>

      <div>
        <h2 class="mb-3 text-sm font-medium text-slate-700">待处理流程一览</h2>
        <DataTable :columns="pendingColumns" :rows="pendingRows" />
      </div>
    </div>
  </div>
</template>
