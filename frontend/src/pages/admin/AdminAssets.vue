<script setup>
import { computed, ref } from "vue";
import { ElMessage } from "element-plus";
import DataTable from "../../components/DataTable.vue";
import { findUserByEmail, getCurrentUser, listApplications, listDevices, updateApplicationStatus } from "../../mock/db";
import { useMockDb } from "../../composables/useMockDb";

useMockDb();

const currentUser = computed(() => getCurrentUser());
const currentDbUser = computed(() => (currentUser.value ? findUserByEmail(currentUser.value.email) : null));

// 资产台账概览（从 db 动态计算：按 category 聚合）
const assetColumns = [
  { key: "category", label: "资产类别" },
  { key: "count", label: "数量" },
  { key: "status", label: "状态" }
];

const assetRows = computed(() => {
  const devices = listDevices();
  const map = new Map();
  for (const d of devices) {
    const key = d.category || "其他";
    const bucket = map.get(key) || { category: key, count: 0, maintenance: 0 };
    bucket.count += 1;
    if (d.status === "maintenance") bucket.maintenance += 1;
    map.set(key, bucket);
  }
  return [...map.values()].map((x) => ({
    category: x.category,
    count: `${x.count} 台`,
    status: x.maintenance > 0 ? `部分在修（${x.maintenance}）` : "在用"
  }));
});

// 申请单审核
const filterType = ref("all");
const filterStatus = ref("submitted");
const noteMap = ref({});

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

function statusPill(status) {
  if (status === "submitted") return "bg-amber-100 text-amber-700";
  if (status === "approved") return "bg-green-100 text-green-700";
  if (status === "rejected") return "bg-rose-100 text-rose-700";
  return "bg-slate-100 text-slate-600";
}

const applications = computed(() => {
  let list = listApplications();
  if (filterType.value !== "all") list = list.filter((a) => a.type === filterType.value);
  if (filterStatus.value !== "all") list = list.filter((a) => a.status === filterStatus.value);
  return list;
});

function getNote(appId) {
  return noteMap.value[appId] || "";
}

function setNote(appId, val) {
  noteMap.value = { ...noteMap.value, [appId]: val };
}

function approve(app) {
  if (!currentDbUser.value) return;
  updateApplicationStatus(app.id, "approved", { reviewedByUserId: currentDbUser.value.id, reviewNote: getNote(app.id) });
  ElMessage.success("已通过申请");
}

function reject(app) {
  if (!currentDbUser.value) return;
  updateApplicationStatus(app.id, "rejected", { reviewedByUserId: currentDbUser.value.id, reviewNote: getNote(app.id) });
  ElMessage.success("已驳回申请");
}

const flowColumns = [
  { key: "type", label: "申请类型" },
  { key: "title", label: "标题" },
  { key: "time", label: "提交时间" },
  { key: "status", label: "状态" }
];

function pad2(n) {
  return String(n).padStart(2, "0");
}

function formatDate(iso) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "-";
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
}

const flowRows = computed(() =>
  listApplications()
    .slice()
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 6)
    .map((a) => ({
      type: typeText(a.type),
      title: a.title,
      time: formatDate(a.createdAt),
      status: statusText(a.status)
    }))
);
</script>

<template>
  <div class="flex h-full flex-col bg-white">
    <div class="border-b border-slate-200 px-6 py-4">
      <h2 class="text-xl font-semibold text-slate-900">设备资产管理</h2>
      <p class="mt-1 text-sm text-slate-500">资产入库、维修工单审核跟踪、报废与采购流程管理。</p>
    </div>

    <div class="flex-1 overflow-auto p-6 space-y-6">
      <div class="flex flex-wrap gap-3">
        <button class="btn-secondary">资产入库登记</button>
        <button class="btn-secondary">维修工单审核</button>
        <button class="btn-primary">申请单审核</button>
      </div>

      <div>
        <h3 class="mb-3 text-sm font-medium text-slate-700">资产台账概览</h3>
        <DataTable :columns="assetColumns" :rows="assetRows" />
      </div>

      <div>
        <h3 class="mb-3 text-sm font-medium text-slate-700">近期申请单流转</h3>
        <DataTable :columns="flowColumns" :rows="flowRows" />
      </div>

      <div>
        <div class="mb-3 flex flex-wrap items-center justify-between gap-3">
          <h3 class="text-sm font-medium text-slate-700">申请单审核（可操作）</h3>
          <div class="flex flex-wrap gap-2">
            <select v-model="filterType" class="input w-40">
              <option value="all">全部类型</option>
              <option value="lab_apply">实验室申请</option>
              <option value="device_apply">设备申请</option>
              <option value="scrap_apply">报废申请</option>
            </select>
            <select v-model="filterStatus" class="input w-36">
              <option value="submitted">仅待审核</option>
              <option value="all">全部状态</option>
              <option value="approved">已通过</option>
              <option value="rejected">已驳回</option>
            </select>
          </div>
        </div>

        <div v-if="!currentDbUser" class="rounded-xl border border-slate-200 bg-slate-50 p-6 text-sm text-slate-600">
          未登录或当前账号无效，请重新登录管理员账号。
        </div>

        <div v-else class="space-y-3">
          <div v-for="app in applications" :key="app.id" class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
            <div class="flex items-start justify-between gap-4">
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-2">
                  <div class="truncate text-base font-semibold text-slate-900">{{ app.title }}</div>
                  <span class="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">{{ typeText(app.type) }}</span>
                  <span class="rounded-full px-2 py-0.5 text-xs font-medium" :class="statusPill(app.status)">{{ statusText(app.status) }}</span>
                </div>
                <div class="mt-2 text-sm text-slate-700">
                  <span class="font-medium text-slate-800">详情：</span>{{ app.detail || "-" }}
                </div>
                <div class="mt-2 text-xs text-slate-500">提交时间：{{ formatDate(app.createdAt) }}</div>

                <div class="mt-3">
                  <label class="block text-xs font-medium text-slate-600">审核意见（可选）</label>
                  <input
                    class="input mt-1"
                    :value="getNote(app.id)"
                    placeholder="例如：材料齐全/需补充附件/不符合报废条件..."
                    @input="setNote(app.id, $event.target.value)"
                  />
                </div>

                <div v-if="app.reviewNote" class="mt-3 rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
                  <span class="font-medium text-slate-800">已审核意见：</span>{{ app.reviewNote }}
                </div>
              </div>

              <div class="flex flex-col gap-2">
                <button v-if="app.status === 'submitted'" class="btn-primary px-3 py-2 text-sm" @click="approve(app)">通过</button>
                <button
                  v-if="app.status === 'submitted'"
                  class="btn-secondary px-3 py-2 text-sm border-rose-200 text-rose-700 hover:bg-rose-50"
                  @click="reject(app)"
                >
                  驳回
                </button>
              </div>
            </div>
          </div>

          <div v-if="applications.length === 0" class="rounded-xl border border-dashed border-slate-200 bg-slate-50 p-10 text-center text-sm text-slate-500">
            暂无符合条件的申请单
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
