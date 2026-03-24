<script setup>
import { computed, onMounted, ref } from "vue";
import DataTable from "../../components/DataTable.vue";
import { listBookingsApi } from "../../services/bookings";
import { listDevicesApi, listLabsApi } from "../../services/resources";
import { listRepairsApi } from "../../services/repairs";
import { listUsersApi } from "../../services/users";

const rangeDays = ref(30);
const resourceType = ref("all"); // all | lab | device
const highFreqThreshold = ref(3);
const bookingsAll = ref([]);
const labsAll = ref([]);
const devicesAll = ref([]);
const repairsAll = ref([]);
const usersAll = ref([]);

function withinDays(iso, days) {
  const t = new Date(iso).getTime();
  if (!Number.isFinite(t)) return false;
  const now = Date.now();
  const diff = now - t;
  return diff >= 0 && diff <= days * 24 * 60 * 60 * 1000;
}

const bookingsInRange = computed(() => {
  const all = bookingsAll.value;
  return all
    .filter((b) => withinDays(b.createdAt, rangeDays.value))
    .filter((b) => (resourceType.value === "all" ? true : b.resourceType === resourceType.value));
});

const activeBookings = computed(() =>
  bookingsInRange.value.filter((b) => ["pending", "approved", "completed"].includes(b.status))
);

const resourcesCount = computed(() => {
  const labs = labsAll.value.length;
  const devices = devicesAll.value.length;
  if (resourceType.value === "lab") return labs;
  if (resourceType.value === "device") return devices;
  return labs + devices;
});

const bookedResourceSet = computed(() => {
  const set = new Set();
  for (const b of activeBookings.value) set.add(`${b.resourceType}:${b.resourceId}`);
  return set;
});

const overallUsage = computed(() => {
  const total = resourcesCount.value || 1;
  const used = bookedResourceSet.value.size;
  return Math.round((used / total) * 100);
});

const highFreqUsers = computed(() => {
  const map = new Map();
  for (const b of bookingsInRange.value) {
    const k = b.createdByUserId;
    map.set(k, (map.get(k) || 0) + 1);
  }
  const threshold = Number(highFreqThreshold.value) || 3;
  return [...map.entries()].filter(([, cnt]) => cnt >= threshold).length;
});

const warnings = computed(() => {
  const maintenanceDevices = devicesAll.value.filter((d) => d.status === "maintenance").length;
  const openRepairs = repairsAll.value.filter((r) => r.status !== "resolved").length;
  return maintenanceDevices + openRepairs;
});

const usageColumns = [
  { key: "name", label: "实验室" },
  { key: "usage", label: "利用率(近似)" },
  { key: "bookings", label: "预约次数" },
  { key: "status", label: "当前状态" }
];

const usageRows = computed(() => {
  const labs = labsAll.value;
  const bookingMap = new Map();
  for (const b of bookingsInRange.value) {
    if (b.resourceType !== "lab") continue;
    bookingMap.set(b.resourceId, (bookingMap.get(b.resourceId) || 0) + 1);
  }
  const max = Math.max(1, ...[...bookingMap.values(), 1]);
  return labs
    .map((lab) => {
      const cnt = bookingMap.get(lab.id) || 0;
      const usage = Math.round((cnt / max) * 100);
      return {
        name: lab.name,
        usage: `${usage}%`,
        bookings: `${cnt} 次`,
        status: lab.status === "available" ? "可用" : lab.status === "booked" ? "预约中" : "维修中"
      };
    })
    .sort((a, b) => Number(String(b.usage).replace("%", "")) - Number(String(a.usage).replace("%", "")));
});

const userColumns = [
  { key: "user", label: "用户" },
  { key: "role", label: "角色" },
  { key: "count", label: "预约次数" }
];

const userRows = computed(() => {
  const users = usersAll.value;
  const map = new Map();
  for (const b of bookingsInRange.value) map.set(b.createdByUserId, (map.get(b.createdByUserId) || 0) + 1);

  return [...map.entries()]
    .map(([userId, cnt]) => {
      const u = users.find((x) => x.id === userId);
      return {
        user: u?.realName || u?.email || `用户#${userId}`,
        role: u?.role === "student" ? "学生" : u?.role === "teacher" ? "教师" : "管理员",
        count: cnt
      };
    })
    .sort((a, b) => b.count - a.count)
    .slice(0, 10);
});

onMounted(async () => {
  const [bookings, labs, devices, repairs, users] = await Promise.all([
    listBookingsApi(),
    listLabsApi(),
    listDevicesApi(),
    listRepairsApi(),
    listUsersApi()
  ]);
  bookingsAll.value = bookings;
  labsAll.value = labs;
  devicesAll.value = devices;
  repairsAll.value = repairs;
  usersAll.value = users;
});
</script>

<template>
  <div class="flex h-full flex-col bg-white">
    <div class="border-b border-slate-200 px-6 py-4">
      <h2 class="text-xl font-semibold text-slate-900">数据分析</h2>
      <p class="mt-1 text-sm text-slate-500">利用率报表、用户行为分析、预警看板（本地 mock 实时计算）。</p>
    </div>

    <div class="flex-1 overflow-auto p-6 space-y-6">
      <div class="flex flex-wrap items-center gap-3">
        <select v-model.number="rangeDays" class="input w-40">
          <option :value="7">近 7 天</option>
          <option :value="30">近 30 天</option>
          <option :value="90">近 90 天</option>
        </select>
        <select v-model="resourceType" class="input w-40">
          <option value="all">全部资源</option>
          <option value="lab">仅实验室</option>
          <option value="device">仅设备</option>
        </select>
        <div class="flex items-center gap-2 text-sm text-slate-600">
          高频阈值
          <input v-model.number="highFreqThreshold" type="number" min="1" class="input w-24" />
          次
        </div>
      </div>

      <div class="grid gap-4 md:grid-cols-3">
        <div class="card p-4">
          <h3 class="text-sm font-medium text-slate-600">整体利用率</h3>
          <p class="mt-2 text-2xl font-semibold text-brand-700">{{ overallUsage }}%</p>
          <p class="mt-1 text-xs text-slate-500">近 {{ rangeDays }} 天内被预约过的资源占比（近似）</p>
        </div>
        <div class="card p-4">
          <h3 class="text-sm font-medium text-slate-600">高频用户数</h3>
          <p class="mt-2 text-2xl font-semibold text-brand-700">{{ highFreqUsers }}</p>
          <p class="mt-1 text-xs text-slate-500">近 {{ rangeDays }} 天预约次数 ≥ {{ highFreqThreshold }} 次</p>
        </div>
        <div class="card p-4">
          <h3 class="text-sm font-medium text-slate-600">耗材预警项</h3>
          <p class="mt-2 text-2xl font-semibold text-brand-700">{{ warnings }}</p>
          <p class="mt-1 text-xs text-slate-500">口径：维修中设备 + 未解决报修（演示）</p>
        </div>
      </div>

      <div>
        <h3 class="mb-3 text-sm font-medium text-slate-700">实验室利用率明细</h3>
        <DataTable :columns="usageColumns" :rows="usageRows" />
      </div>

      <div>
        <h3 class="mb-3 text-sm font-medium text-slate-700">高频用户 Top10</h3>
        <DataTable :columns="userColumns" :rows="userRows" />
      </div>
    </div>
  </div>
</template>
