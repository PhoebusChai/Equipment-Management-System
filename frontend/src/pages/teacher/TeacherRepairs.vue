<script setup>
import { computed, ref } from "vue";
import { ElMessage } from "element-plus";
import { createRepair, findUserByEmail, getCurrentUser, listRepairs, updateRepairStatus } from "../../mock/db";
import { useMockDb } from "../../composables/useMockDb";

useMockDb();

const filter = ref("all");

const currentUser = computed(() => getCurrentUser());
const currentDbUser = computed(() => (currentUser.value ? findUserByEmail(currentUser.value.email) : null));

function pad2(n) {
  return String(n).padStart(2, "0");
}

function formatShort(iso) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "-";
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}

const repairs = computed(() => {
  if (!currentDbUser.value) return [];
  const list = listRepairs({ handlerUserId: currentDbUser.value.id })
    .slice()
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

  if (filter.value === "all") return list;
  return list.filter((r) => r.status === filter.value);
});

const statusPillClass = (status) => {
  if (status === "submitted") return "bg-amber-100 text-amber-700";
  if (status === "confirmed" || status === "in_progress") return "bg-rose-100 text-rose-700";
  if (status === "resolved") return "bg-green-100 text-green-700";
  return "bg-slate-100 text-slate-600";
};

const statusText = (status) => {
  if (status === "submitted") return "待确认";
  if (status === "confirmed") return "已确认";
  if (status === "in_progress") return "维修中";
  if (status === "resolved") return "已解决";
  return status;
};

function confirmRepair(id) {
  updateRepairStatus(id, "confirmed");
  ElMessage.success("已确认报修，资源已暂停预约");
}

function startRepair(id) {
  updateRepairStatus(id, "in_progress");
  ElMessage.success("已开始维修");
}

function resolveRepair(id) {
  updateRepairStatus(id, "resolved");
  ElMessage.success("维修完成，资源已恢复可预约");
}

function createQuickRepair() {
  if (!currentDbUser.value) return;
  const sample = [
    { resourceType: "lab", resourceId: 3, resourceName: "生物实验室B201" },
    { resourceType: "device", resourceId: 1, resourceName: "高性能计算机 #01" }
  ][Math.floor(Math.random() * 2)];

  createRepair({
    createdByUserId: currentDbUser.value.id,
    handlerUserId: currentDbUser.value.id,
    resourceType: sample.resourceType,
    resourceId: sample.resourceId,
    resourceName: sample.resourceName,
    description: "快速创建的示例报修（可在此页流转状态）"
  });
  ElMessage.success("已创建一条示例报修工单");
}
</script>

<template>
  <div class="flex h-full flex-col bg-white">
    <div class="border-b border-slate-200 px-6 py-4">
      <h2 class="text-xl font-semibold text-slate-900">报修与工单</h2>
      <p class="mt-1 text-sm text-slate-500">查看报修、切换资源状态、确认维修完成并恢复预约。</p>
    </div>

    <div class="flex-1 overflow-auto p-6 space-y-4">
      <div class="flex flex-wrap gap-3">
        <select v-model="filter" class="input w-44">
          <option value="all">全部状态</option>
          <option value="submitted">待确认</option>
          <option value="confirmed">已确认</option>
          <option value="in_progress">维修中</option>
          <option value="resolved">已解决</option>
        </select>
        <div class="flex-1"></div>
        <button class="btn-primary" :disabled="!currentDbUser" @click="createQuickRepair">新建报修工单</button>
      </div>

      <div>
        <h3 class="mb-3 text-sm font-medium text-slate-700">当前报修工单</h3>

        <div v-if="!currentDbUser" class="rounded-xl border border-slate-200 bg-slate-50 p-6 text-sm text-slate-600">
          未登录或当前账号无效，请重新登录教师账号。
        </div>

        <div v-else class="space-y-3">
          <div v-for="r in repairs" :key="r.id" class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
            <div class="flex items-start justify-between gap-4">
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-2">
                  <div class="truncate text-base font-semibold text-slate-900">{{ r.resourceName }}</div>
                  <span class="rounded-full px-2 py-0.5 text-xs font-medium" :class="statusPillClass(r.status)">
                    {{ statusText(r.status) }}
                  </span>
                  <span class="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">
                    {{ r.resourceType === "lab" ? "实验室" : "设备" }}
                  </span>
                </div>
                <div class="mt-2 text-sm text-slate-600">报修时间：{{ formatShort(r.createdAt) }}</div>
                <div class="mt-2 text-sm text-slate-700">
                  <span class="font-medium text-slate-800">故障描述：</span>{{ r.description || "-" }}
                </div>
              </div>

              <div class="flex flex-col gap-2">
                <button v-if="r.status === 'submitted'" class="btn-primary px-3 py-2 text-sm" @click="confirmRepair(r.id)">
                  确认报修
                </button>
                <button v-if="r.status === 'confirmed'" class="btn-secondary px-3 py-2 text-sm" @click="startRepair(r.id)">
                  开始维修
                </button>
                <button v-if="r.status === 'in_progress'" class="btn-primary px-3 py-2 text-sm" @click="resolveRepair(r.id)">
                  维修完成
                </button>
              </div>
            </div>
          </div>

          <div v-if="repairs.length === 0" class="rounded-xl border border-dashed border-slate-200 bg-slate-50 p-10 text-center text-sm text-slate-500">
            暂无报修工单
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
