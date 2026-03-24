<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { getCurrentUser } from "../../services/session";
import {
  REPAIR_STATUS,
  confirmRepairApi,
  createRepairApi,
  finishRepairApi,
  listRepairsApi,
  startRepairApi
} from "../../services/repairs";

const route = useRoute();

const filterStatus = ref("all");
const filterResourceType = ref("all");
const keyword = ref("");

const currentUser = computed(() => getCurrentUser());
const currentDbUser = computed(() => currentUser.value || null);

onMounted(() => {
  const q = route.query.q || route.query.keyword;
  if (typeof q === "string" && q.trim()) keyword.value = q.trim();
});

watch(
  () => route.query.q || route.query.keyword,
  (q) => {
    if (typeof q === "string" && q.trim()) keyword.value = q.trim();
  }
);

function pad2(n) {
  return String(n).padStart(2, "0");
}

function formatShort(iso) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "-";
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}

const repairs = ref([]);
const allRepairs = computed(() =>
  repairs.value.slice().sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
);

async function loadRepairs() {
  if (!currentDbUser.value) {
    repairs.value = [];
    return;
  }
  repairs.value = await listRepairsApi(currentDbUser.value.id);
}

const stats = computed(() => {
  const list = allRepairs.value;
  return {
    total: list.length,
    submitted: list.filter((r) => r.status === REPAIR_STATUS.SUBMITTED).length,
    confirmed: list.filter((r) => r.status === REPAIR_STATUS.CONFIRMED).length,
    inProgress: list.filter((r) => r.status === REPAIR_STATUS.IN_PROGRESS).length,
    resolved: list.filter((r) => r.status === REPAIR_STATUS.RESOLVED).length
  };
});

const filteredRepairs = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return allRepairs.value.filter((r) => {
    const byStatus = filterStatus.value === "all" || r.status === filterStatus.value;
    const byType =
      filterResourceType.value === "all" ||
      (filterResourceType.value === "lab" && r.resourceType === "lab") ||
      (filterResourceType.value === "device" && r.resourceType === "device");
    const byKw =
      !kw ||
      String(r.resourceName || "").toLowerCase().includes(kw) ||
      String(r.description || "").toLowerCase().includes(kw) ||
      String(r.resourceType || "").toLowerCase().includes(kw) ||
      String(r.id).includes(kw);
    return byStatus && byType && byKw;
  });
});

const statusFilters = [
  { value: "all", label: "全部" },
  { value: REPAIR_STATUS.SUBMITTED, label: "待确认" },
  { value: REPAIR_STATUS.CONFIRMED, label: "已确认" },
  { value: REPAIR_STATUS.IN_PROGRESS, label: "维修中" },
  { value: REPAIR_STATUS.RESOLVED, label: "已解决" }
];

const statusPillClass = (status) => {
  if (status === REPAIR_STATUS.SUBMITTED) return "bg-amber-100 text-amber-800 ring-1 ring-amber-200/60";
  if (status === REPAIR_STATUS.CONFIRMED) return "bg-orange-100 text-orange-900 ring-1 ring-orange-200/60";
  if (status === REPAIR_STATUS.IN_PROGRESS) return "bg-rose-100 text-rose-800 ring-1 ring-rose-200/60";
  if (status === REPAIR_STATUS.RESOLVED) return "bg-emerald-100 text-emerald-800 ring-1 ring-emerald-200/60";
  return "bg-slate-100 text-slate-600";
};

const statusText = (status) => {
  if (status === REPAIR_STATUS.SUBMITTED) return "待确认";
  if (status === REPAIR_STATUS.CONFIRMED) return "已确认";
  if (status === REPAIR_STATUS.IN_PROGRESS) return "维修中";
  if (status === REPAIR_STATUS.RESOLVED) return "已解决";
  return status;
};

function cardAccentClass(status) {
  if (status === REPAIR_STATUS.SUBMITTED) return "border-l-amber-400";
  if (status === REPAIR_STATUS.CONFIRMED) return "border-l-orange-400";
  if (status === REPAIR_STATUS.IN_PROGRESS) return "border-l-rose-500";
  if (status === REPAIR_STATUS.RESOLVED) return "border-l-emerald-500";
  return "border-l-slate-200";
}

const chipBase =
  "inline-flex cursor-pointer items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-medium transition";

async function confirmRepair(id) {
  if (!currentDbUser.value) return;
  await confirmRepairApi(id, currentDbUser.value.id);
  await loadRepairs();
  ElMessage.success("已确认报修，相关资源已进入维护状态");
}

async function startRepair(id) {
  if (!currentDbUser.value) return;
  await startRepairApi(id, currentDbUser.value.id);
  await loadRepairs();
  ElMessage.success("已开始维修");
}

async function resolveRepair(id) {
  if (!currentDbUser.value) return;
  await finishRepairApi(id, currentDbUser.value.id);
  await loadRepairs();
  ElMessage.success("已标记解决，资源恢复可预约");
}

function openCreateRepairDialog() {
  if (!currentDbUser.value) {
    ElMessage.error("请先登录教师账号");
    return;
  }
  ElMessageBox.prompt("填写故障描述后将创建一条报修工单（演示：资源随机抽取）", "新建报修工单", {
    confirmButtonText: "创建",
    cancelButtonText: "取消",
    inputType: "textarea",
    inputPlaceholder: "例如：设备无法开机、实验室空调异响、需上门检修…"
  })
    .then(async ({ value }) => {
      const desc = String(value || "").trim();
      if (!desc) {
        ElMessage.warning("请填写故障描述");
        return;
      }
      const sample = [
        { resourceType: "lab", resourceId: 3, resourceName: "生物实验室B201" },
        { resourceType: "device", resourceId: 1, resourceName: "高性能计算机 #01" },
        { resourceType: "device", resourceId: 2, resourceName: "显微镜 BIO-02" }
      ][Math.floor(Math.random() * 3)];

      await createRepairApi({
        createdByUserId: currentDbUser.value.id,
        resourceType: sample.resourceType,
        resourceId: sample.resourceId,
        description: desc
      });
      await loadRepairs();
      ElMessage.success(`工单已创建：${sample.resourceName}`);
    })
    .catch(() => {});
}

async function createDemoRepair() {
  if (!currentDbUser.value) return;
  const sample = [
    { resourceType: "lab", resourceId: 3, resourceName: "生物实验室B201" },
    { resourceType: "device", resourceId: 1, resourceName: "高性能计算机 #01" }
  ][Math.floor(Math.random() * 2)];

  await createRepairApi({
    createdByUserId: currentDbUser.value.id,
    resourceType: sample.resourceType,
    resourceId: sample.resourceId,
    description: "【示例】快速演示工单，可在此页流转状态"
  });
  await loadRepairs();
  ElMessage.success("已创建示例报修工单");
}

onMounted(loadRepairs);
</script>

<template>
  <div class="flex h-full w-full flex-col bg-white">
    <div class="border-b border-slate-200 bg-gradient-to-r from-white via-slate-50/50 to-rose-50/20 px-6 py-5">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 class="text-xl font-semibold text-slate-900">报修工单</h2>
          <p class="mt-1 max-w-2xl text-sm leading-relaxed text-slate-500">
            确认报修、推进维修流程并在完成后恢复资源可预约状态。支持按状态、类型与关键词筛选。
          </p>
        </div>
        <div class="flex shrink-0 flex-wrap gap-2">
          <button
            type="button"
            class="btn-secondary text-sm shadow-sm"
            :disabled="!currentDbUser"
            @click="createDemoRepair"
          >
            示例工单
          </button>
          <button type="button" class="btn-primary text-sm shadow-sm" :disabled="!currentDbUser" @click="openCreateRepairDialog">
            新建报修
          </button>
        </div>
      </div>
    </div>

    <div class="flex min-h-0 flex-1 flex-col gap-4 overflow-auto p-6">
      <!-- 统计 -->
      <div v-if="currentDbUser" class="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
        <div class="rounded-xl border border-slate-200/90 bg-white p-3 shadow-sm ring-1 ring-slate-100">
          <div class="text-xs font-medium text-slate-500">全部工单</div>
          <div class="mt-1 text-2xl font-semibold tabular-nums text-slate-900">{{ stats.total }}</div>
        </div>
        <div class="rounded-xl border border-amber-100 bg-amber-50/50 p-3 shadow-sm ring-1 ring-amber-100/80">
          <div class="text-xs font-medium text-amber-900/80">待确认</div>
          <div class="mt-1 text-2xl font-semibold tabular-nums text-amber-950">{{ stats.submitted }}</div>
        </div>
        <div class="rounded-xl border border-orange-100 bg-orange-50/40 p-3 shadow-sm ring-1 ring-orange-100/70">
          <div class="text-xs font-medium text-orange-900/80">已确认</div>
          <div class="mt-1 text-2xl font-semibold tabular-nums text-orange-950">{{ stats.confirmed }}</div>
        </div>
        <div class="rounded-xl border border-rose-100 bg-rose-50/40 p-3 shadow-sm ring-1 ring-rose-100/70">
          <div class="text-xs font-medium text-rose-900/80">维修中</div>
          <div class="mt-1 text-2xl font-semibold tabular-nums text-rose-950">{{ stats.inProgress }}</div>
        </div>
        <div class="rounded-xl border border-emerald-100 bg-emerald-50/40 p-3 shadow-sm ring-1 ring-emerald-100/70 sm:col-span-2 lg:col-span-1">
          <div class="text-xs font-medium text-emerald-900/80">已解决</div>
          <div class="mt-1 text-2xl font-semibold tabular-nums text-emerald-950">{{ stats.resolved }}</div>
        </div>
      </div>

      <!-- 筛选 -->
      <div class="flex flex-col gap-4 rounded-2xl border border-slate-200/90 bg-slate-50/40 p-4 ring-1 ring-slate-100">
        <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <span class="text-xs font-semibold uppercase tracking-wider text-slate-400">工单状态</span>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="opt in statusFilters"
              :key="opt.value"
              type="button"
              :class="[
                chipBase,
                filterStatus === opt.value
                  ? 'border-brand-500 bg-brand-50 text-brand-800 shadow-sm'
                  : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:bg-slate-50'
              ]"
              @click="filterStatus = opt.value"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>
        <div class="h-px bg-slate-200/80" />
        <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:gap-4">
          <div class="flex-1 space-y-1.5">
            <label class="text-xs font-semibold uppercase tracking-wider text-slate-400">搜索</label>
            <input
              v-model="keyword"
              class="input !w-full"
              placeholder="资源名称、故障描述、工单号、类型…"
            />
          </div>
          <div class="space-y-1.5 lg:w-44">
            <label class="text-xs font-semibold uppercase tracking-wider text-slate-400">资源类型</label>
            <select v-model="filterResourceType" class="input !w-full">
              <option value="all">全部类型</option>
              <option value="lab">实验室</option>
              <option value="device">设备</option>
            </select>
          </div>
        </div>
      </div>

      <div class="flex flex-wrap items-end justify-between gap-2">
        <div>
          <h3 class="text-sm font-semibold text-slate-800">工单列表</h3>
          <p class="mt-0.5 text-xs text-slate-500">当前筛选共 {{ filteredRepairs.length }} 条</p>
        </div>
      </div>

      <div v-if="!currentDbUser" class="rounded-2xl border border-dashed border-slate-200 bg-slate-50/80 px-6 py-12 text-center">
        <p class="text-sm font-medium text-slate-700">未登录或账号无效</p>
        <p class="mt-1 text-xs text-slate-500">请使用教师账号登录后查看负责处理的报修工单。</p>
      </div>

      <div v-else class="space-y-4">
        <article
          v-for="r in filteredRepairs"
          :key="r.id"
          class="overflow-hidden rounded-2xl border border-slate-200/90 bg-white shadow-sm ring-1 ring-slate-100 transition hover:shadow-md"
          :class="['border-l-4', cardAccentClass(r.status)]"
        >
          <div class="flex flex-col gap-4 p-5 lg:flex-row lg:items-stretch lg:gap-6">
            <div class="min-w-0 flex-1 space-y-4">
              <div class="flex flex-wrap items-start gap-2">
                <div class="min-w-0 flex-1">
                  <div class="flex flex-wrap items-center gap-2">
                    <span class="rounded-md bg-slate-100 px-2 py-0.5 font-mono text-xs font-medium text-slate-600">
                      #{{ r.id }}
                    </span>
                    <h4 class="text-lg font-semibold leading-snug text-slate-900">
                      {{ r.resourceName }}
                    </h4>
                  </div>
                </div>
                <div class="flex flex-wrap items-center gap-2">
                  <span class="rounded-full px-2.5 py-0.5 text-xs font-semibold" :class="statusPillClass(r.status)">
                    {{ statusText(r.status) }}
                  </span>
                  <span
                    class="rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-semibold text-blue-800 ring-1 ring-blue-100"
                  >
                    {{ r.resourceType === "lab" ? "实验室" : "设备" }}
                  </span>
                </div>
              </div>

              <div class="rounded-xl bg-slate-50/80 p-4">
                <div class="text-xs font-medium text-slate-400">故障 / 报修描述</div>
                <p class="mt-2 text-sm leading-relaxed text-slate-800">
                  {{ r.description || "—" }}
                </p>
                <div class="mt-3 text-xs text-slate-400">提交时间 {{ formatShort(r.createdAt) }}</div>
              </div>

              <div class="rounded-lg border border-slate-100 bg-slate-50/50 px-3 py-2 text-xs text-slate-500">
                <span class="font-medium text-slate-600">流程提示：</span>
                待确认 → 已确认（资源维护）→ 维修中 → 已解决（恢复可预约）
              </div>
            </div>

            <div
              class="flex shrink-0 flex-row gap-2 border-t border-slate-100 pt-4 lg:w-40 lg:flex-col lg:border-l lg:border-t-0 lg:pl-6 lg:pt-0"
            >
              <button
                v-if="r.status === REPAIR_STATUS.SUBMITTED"
                type="button"
                class="btn-primary flex-1 px-4 py-2.5 text-sm shadow-sm lg:flex-none"
                @click="confirmRepair(r.id)"
              >
                确认报修
              </button>
              <button
                v-if="r.status === REPAIR_STATUS.CONFIRMED"
                type="button"
                class="btn-secondary flex-1 border-slate-200 px-4 py-2.5 text-sm shadow-sm lg:flex-none"
                @click="startRepair(r.id)"
              >
                开始维修
              </button>
              <button
                v-if="r.status === REPAIR_STATUS.IN_PROGRESS"
                type="button"
                class="btn-primary flex-1 px-4 py-2.5 text-sm shadow-sm lg:flex-none"
                @click="resolveRepair(r.id)"
              >
                维修完成
              </button>
              <p
                v-if="r.status === REPAIR_STATUS.RESOLVED"
                class="flex flex-1 items-center justify-center rounded-lg border border-dashed border-emerald-200 bg-emerald-50/40 px-3 py-3 text-center text-xs font-medium text-emerald-800 lg:flex-none"
              >
                已闭环
              </p>
            </div>
          </div>
        </article>

        <div
          v-if="filteredRepairs.length === 0"
          class="rounded-2xl border border-dashed border-slate-200 bg-gradient-to-b from-slate-50/80 to-white px-6 py-16 text-center"
        >
          <p class="text-sm font-medium text-slate-600">暂无符合条件的工单</p>
          <p class="mt-1 text-xs text-slate-400">可调整状态筛选、资源类型，或点击「新建报修」创建工单。</p>
        </div>
      </div>
    </div>
  </div>
</template>
