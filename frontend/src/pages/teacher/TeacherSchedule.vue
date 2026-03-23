<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { findUserByEmail, getCurrentUser, listBookings, updateBookingStatus } from "../../mock/db";
import { useMockDb } from "../../composables/useMockDb";

useMockDb();

const route = useRoute();

const filterStatus = ref("pending");
const filterResourceType = ref("all");
const keyword = ref("");

const currentUser = computed(() => getCurrentUser());
const currentDbUser = computed(() => (currentUser.value ? findUserByEmail(currentUser.value.email) : null));

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
  return `${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}

const allBookings = computed(() => {
  if (!currentDbUser.value) return [];
  return listBookings({ role: "teacher", userId: currentDbUser.value.id })
    .slice()
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
});

const stats = computed(() => {
  const list = allBookings.value;
  return {
    total: list.length,
    pending: list.filter((b) => b.status === "pending").length,
    emergencyPending: list.filter((b) => b.status === "pending" && b.isEmergency).length,
    approved: list.filter((b) => b.status === "approved").length,
    completed: list.filter((b) => b.status === "completed").length,
    rejected: list.filter((b) => b.status === "rejected").length
  };
});

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return allBookings.value.filter((b) => {
    const byStatus = filterStatus.value === "all" || b.status === filterStatus.value;
    const byType =
      filterResourceType.value === "all" ||
      (filterResourceType.value === "lab" && b.resourceType === "lab") ||
      (filterResourceType.value === "device" && b.resourceType === "device");
    const byKw =
      !kw ||
      String(b.resourceName || "").toLowerCase().includes(kw) ||
      String(b.purpose || "").toLowerCase().includes(kw) ||
      String(b.resourceType || "").toLowerCase().includes(kw);
    return byStatus && byType && byKw;
  });
});

const statusFilters = [
  { value: "pending", label: "待审核", short: "待审" },
  { value: "all", label: "全部", short: "全部" },
  { value: "approved", label: "已确认", short: "已确认" },
  { value: "completed", label: "已完成", short: "完成" },
  { value: "rejected", label: "已驳回", short: "驳回" },
  { value: "cancelled", label: "已取消", short: "取消" }
];

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
  if (status === "pending") return "bg-amber-100 text-amber-800 ring-1 ring-amber-200/60";
  if (status === "approved") return "bg-emerald-100 text-emerald-800 ring-1 ring-emerald-200/60";
  if (status === "rejected") return "bg-rose-100 text-rose-800 ring-1 ring-rose-200/60";
  if (status === "cancelled") return "bg-red-100 text-red-800 ring-1 ring-red-200/60";
  if (status === "completed") return "bg-slate-100 text-slate-700 ring-1 ring-slate-200/80";
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

/** 卡片左侧强调条颜色 */
function cardAccentClass(status) {
  if (status === "pending") return "border-l-amber-400";
  if (status === "approved") return "border-l-emerald-500";
  if (status === "rejected") return "border-l-rose-500";
  if (status === "cancelled") return "border-l-red-400";
  if (status === "completed") return "border-l-slate-300";
  return "border-l-slate-200";
}

const chipBase =
  "inline-flex cursor-pointer items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-medium transition";
</script>

<template>
  <div class="flex h-full w-full flex-col bg-white">
    <div class="border-b border-slate-200 bg-gradient-to-r from-white to-slate-50/80 px-6 py-5">
      <h2 class="text-xl font-semibold text-slate-900">预约监管</h2>
      <p class="mt-1 max-w-2xl text-sm leading-relaxed text-slate-500">
        审批学生提交的实验室与设备预约，处理紧急通道申请；已确认的记录可在使用结束后标记完成。
      </p>
    </div>

    <div class="flex min-h-0 flex-1 flex-col gap-4 overflow-auto p-6">
      <!-- 统计条 -->
      <div
        v-if="currentDbUser"
        class="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6"
      >
        <div class="rounded-xl border border-slate-200/90 bg-white p-3 shadow-sm ring-1 ring-slate-100">
          <div class="text-xs font-medium text-slate-500">全部预约</div>
          <div class="mt-1 text-2xl font-semibold tabular-nums text-slate-900">{{ stats.total }}</div>
        </div>
        <div class="rounded-xl border border-amber-100 bg-amber-50/50 p-3 shadow-sm ring-1 ring-amber-100/80">
          <div class="text-xs font-medium text-amber-800/90">待审核</div>
          <div class="mt-1 text-2xl font-semibold tabular-nums text-amber-900">{{ stats.pending }}</div>
        </div>
        <div class="rounded-xl border border-red-100 bg-red-50/40 p-3 shadow-sm ring-1 ring-red-100/60">
          <div class="text-xs font-medium text-red-800/90">紧急待审</div>
          <div class="mt-1 text-2xl font-semibold tabular-nums text-red-900">{{ stats.emergencyPending }}</div>
        </div>
        <div class="rounded-xl border border-emerald-100 bg-emerald-50/40 p-3 shadow-sm ring-1 ring-emerald-100/60">
          <div class="text-xs font-medium text-emerald-800/90">已确认</div>
          <div class="mt-1 text-2xl font-semibold tabular-nums text-emerald-900">{{ stats.approved }}</div>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50/80 p-3 shadow-sm ring-1 ring-slate-100">
          <div class="text-xs font-medium text-slate-600">已完成</div>
          <div class="mt-1 text-2xl font-semibold tabular-nums text-slate-800">{{ stats.completed }}</div>
        </div>
        <div class="rounded-xl border border-rose-100 bg-rose-50/40 p-3 shadow-sm ring-1 ring-rose-100/60">
          <div class="text-xs font-medium text-rose-800/90">已驳回</div>
          <div class="mt-1 text-2xl font-semibold tabular-nums text-rose-900">{{ stats.rejected }}</div>
        </div>
      </div>

      <!-- 筛选 -->
      <div class="flex flex-col gap-4 rounded-2xl border border-slate-200/90 bg-slate-50/40 p-4 ring-1 ring-slate-100">
        <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <span class="text-xs font-semibold uppercase tracking-wider text-slate-400">状态</span>
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
              placeholder="资源名称、用途、类型（lab / device）..."
            />
          </div>
          <div class="space-y-1.5 lg:w-44">
            <label class="text-xs font-semibold uppercase tracking-wider text-slate-400">资源类型</label>
            <select v-model="filterResourceType" class="input !w-full">
              <option value="all">全部类型</option>
              <option value="lab">仅实验室</option>
              <option value="device">仅设备</option>
            </select>
          </div>
        </div>
      </div>

      <!-- 列表标题 -->
      <div class="flex flex-wrap items-end justify-between gap-2">
        <div>
          <h3 class="text-sm font-semibold text-slate-800">预约列表</h3>
          <p class="mt-0.5 text-xs text-slate-500">共 {{ filtered.length }} 条（当前筛选条件下）</p>
        </div>
      </div>

      <div v-if="!currentDbUser" class="rounded-2xl border border-dashed border-slate-200 bg-slate-50/80 px-6 py-12 text-center">
        <p class="text-sm font-medium text-slate-700">未登录或账号无效</p>
        <p class="mt-1 text-xs text-slate-500">请使用教师账号重新登录后查看待审预约。</p>
      </div>

      <div v-else class="space-y-4">
        <article
          v-for="b in filtered"
          :key="b.id"
          class="overflow-hidden rounded-2xl border border-slate-200/90 bg-white shadow-sm ring-1 ring-slate-100 transition hover:shadow-md"
          :class="['border-l-4', cardAccentClass(b.status)]"
        >
          <div class="flex flex-col gap-4 p-5 lg:flex-row lg:items-stretch lg:gap-6">
            <div class="min-w-0 flex-1 space-y-4">
              <div class="flex flex-wrap items-start gap-2">
                <h4 class="min-w-0 flex-1 text-lg font-semibold leading-snug text-slate-900">
                  {{ b.resourceName }}
                </h4>
                <div class="flex flex-wrap items-center gap-2">
                  <span class="rounded-full px-2.5 py-0.5 text-xs font-semibold" :class="statusPillClass(b.status)">
                    {{ statusText(b.status) }}
                  </span>
                  <span
                    v-if="b.isEmergency"
                    class="rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-semibold text-red-800 ring-1 ring-red-200/60"
                  >
                    紧急预约
                  </span>
                  <span
                    class="rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-semibold text-blue-800 ring-1 ring-blue-100"
                  >
                    {{ b.resourceType === "lab" ? "实验室" : "设备" }}
                  </span>
                </div>
              </div>

              <div class="grid gap-3 rounded-xl bg-slate-50/80 p-4 sm:grid-cols-2">
                <div>
                  <div class="text-xs font-medium text-slate-400">预约时段</div>
                  <div class="mt-1 text-sm font-medium text-slate-800">
                    {{ formatShort(b.startAt) }}
                    <span class="text-slate-400">→</span>
                    {{ formatShort(b.endAt) }}
                  </div>
                </div>
                <div>
                  <div class="text-xs font-medium text-slate-400">人数</div>
                  <div class="mt-1 text-sm font-medium text-slate-800">{{ b.participants }} 人</div>
                </div>
                <div class="sm:col-span-2">
                  <div class="text-xs font-medium text-slate-400">用途说明</div>
                  <div class="mt-1 text-sm leading-relaxed text-slate-800">{{ b.purpose || "—" }}</div>
                </div>
                <div class="sm:col-span-2">
                  <div class="text-xs text-slate-400">提交时间 {{ formatShort(b.createdAt) }}</div>
                </div>
              </div>

              <div class="space-y-1.5">
                <label class="text-sm font-medium text-slate-700">审核意见 <span class="font-normal text-slate-400">（选填）</span></label>
                <input
                  class="input"
                  :value="getNote(b.id)"
                  placeholder="通过或驳回时可填写：如时间合理、请提前到场、与既有预约冲突等"
                  @input="setNote(b.id, $event.target.value)"
                />
              </div>
            </div>

            <div
              class="flex shrink-0 flex-row gap-2 border-t border-slate-100 pt-4 lg:w-36 lg:flex-col lg:border-l lg:border-t-0 lg:pl-6 lg:pt-0"
            >
              <button
                v-if="b.status === 'pending'"
                type="button"
                class="btn-primary flex-1 px-4 py-2.5 text-sm shadow-sm lg:flex-none"
                @click="approve(b.id)"
              >
                通过
              </button>
              <button
                v-if="b.status === 'pending'"
                type="button"
                class="flex-1 rounded-lg border border-rose-200 bg-white px-4 py-2.5 text-sm font-medium text-rose-700 shadow-sm transition hover:bg-rose-50 lg:flex-none"
                @click="reject(b.id)"
              >
                驳回
              </button>
              <button
                v-if="b.status === 'approved'"
                type="button"
                class="btn-secondary flex-1 border-slate-200 px-4 py-2.5 text-sm shadow-sm lg:flex-none"
                @click="markCompleted(b.id)"
              >
                标记完成
              </button>
              <p
                v-if="!['pending', 'approved'].includes(b.status)"
                class="flex flex-1 items-center justify-center rounded-lg border border-dashed border-slate-200 bg-slate-50/50 px-3 py-3 text-center text-xs text-slate-500 lg:flex-none"
              >
                当前状态无需操作
              </p>
            </div>
          </div>
        </article>

        <div
          v-if="filtered.length === 0"
          class="rounded-2xl border border-dashed border-slate-200 bg-gradient-to-b from-slate-50/80 to-white px-6 py-16 text-center"
        >
          <p class="text-sm font-medium text-slate-600">暂无符合条件的预约</p>
          <p class="mt-1 text-xs text-slate-400">尝试切换状态筛选、资源类型或清空搜索关键词。</p>
        </div>
      </div>
    </div>
  </div>
</template>
