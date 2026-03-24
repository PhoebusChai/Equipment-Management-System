<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import AppPagination from "../../components/AppPagination.vue";
import { BOOKING_STATUS, listBookingsApi, updateBookingStatusApi } from "../../services/bookings";
import { getCurrentUser } from "../../services/session";
import { listUsersBasicApi } from "../../services/users";
import { listTeacherReviewsApi } from "../../services/reviews";

const route = useRoute();

const filterStatus = ref(BOOKING_STATUS.PENDING);
const filterResourceType = ref("lab");
const keyword = ref("");
const currentPage = ref(1);
const pageSize = ref(8);
const reviewVisible = ref(false);
const reviewRows = ref([]);

const currentUser = computed(() => getCurrentUser());
const currentDbUser = computed(() => currentUser.value || null);
const bookingList = ref([]);
const usersSource = ref([]);

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

const allBookings = computed(() =>
  bookingList.value.slice().sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
);

async function loadBookings() {
  if (!currentDbUser.value?.id) return;
  bookingList.value = await listBookingsApi({ approverId: currentDbUser.value.id });
  const userIds = [...new Set((bookingList.value || []).map((x) => x.userId).filter(Boolean))];
  usersSource.value = await listUsersBasicApi(userIds);
}

async function openReviewPanel() {
  if (!currentDbUser.value?.id) return;
  reviewRows.value = await listTeacherReviewsApi({ teacherId: currentDbUser.value.id, resourceType: filterResourceType.value === "all" ? undefined : filterResourceType.value });
  reviewVisible.value = true;
}

function applicantName(userId) {
  const u = usersSource.value.find((x) => x.id === userId);
  return u?.realName || `用户#${userId || "-"}`;
}

const stats = computed(() => {
  const list = allBookings.value;
  return {
    total: list.length,
    pending: list.filter((b) => b.status === BOOKING_STATUS.PENDING).length,
    emergencyPending: list.filter((b) => b.status === BOOKING_STATUS.PENDING && b.isEmergency).length,
    approved: list.filter((b) => b.status === BOOKING_STATUS.APPROVED).length,
    completed: list.filter((b) => b.status === BOOKING_STATUS.COMPLETED).length,
    rejected: list.filter((b) => b.status === BOOKING_STATUS.REJECTED).length
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
  { value: "all", label: "全部", short: "全部" },
  { value: BOOKING_STATUS.PENDING, label: "待审核", short: "待审" },
  { value: BOOKING_STATUS.APPROVED, label: "已确认", short: "已确认" },
  { value: BOOKING_STATUS.COMPLETED, label: "已完成", short: "完成" },
  { value: BOOKING_STATUS.REJECTED, label: "已驳回", short: "驳回" },
  { value: BOOKING_STATUS.CANCELLED, label: "已取消", short: "取消" }
];

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filtered.value.slice(start, start + pageSize.value);
});

const reviewNoteMap = ref({});

function getNote(bookingId) {
  return reviewNoteMap.value[bookingId] || "";
}

function setNote(bookingId, val) {
  reviewNoteMap.value = { ...reviewNoteMap.value, [bookingId]: val };
}

async function approve(bookingId) {
  await updateBookingStatusApi(bookingId, BOOKING_STATUS.APPROVED, {
    approverId: currentDbUser.value.id,
    reviewNote: getNote(bookingId)
  });
  await loadBookings();
  ElMessage.success("已通过该预约");
}

async function reject(bookingId) {
  await updateBookingStatusApi(bookingId, BOOKING_STATUS.REJECTED, {
    approverId: currentDbUser.value.id,
    reviewNote: getNote(bookingId)
  });
  await loadBookings();
  ElMessage.success("已驳回该预约");
}

async function markCompleted(bookingId) {
  await updateBookingStatusApi(bookingId, BOOKING_STATUS.COMPLETED, {
    approverId: currentDbUser.value.id
  });
  await loadBookings();
  ElMessage.success("已标记为完成");
}

const statusPillClass = (status) => {
  if (status === BOOKING_STATUS.PENDING) return "bg-amber-100 text-amber-800";
  if (status === BOOKING_STATUS.APPROVED) return "bg-emerald-100 text-emerald-800";
  if (status === BOOKING_STATUS.REJECTED) return "bg-rose-100 text-rose-800";
  if (status === BOOKING_STATUS.CANCELLED) return "bg-red-100 text-red-800";
  if (status === BOOKING_STATUS.COMPLETED) return "bg-slate-100 text-slate-700";
  return "bg-slate-100 text-slate-600";
};

const statusText = (status) => {
  if (status === BOOKING_STATUS.PENDING) return "待审核";
  if (status === BOOKING_STATUS.APPROVED) return "已确认";
  if (status === BOOKING_STATUS.REJECTED) return "已驳回";
  if (status === BOOKING_STATUS.CANCELLED) return "已取消";
  if (status === BOOKING_STATUS.COMPLETED) return "已完成";
  return status;
};

/** 卡片左侧强调条颜色 */
function cardAccentClass(status) {
  if (status === BOOKING_STATUS.PENDING) return "border-l-amber-400";
  if (status === BOOKING_STATUS.APPROVED) return "border-l-emerald-500";
  if (status === BOOKING_STATUS.REJECTED) return "border-l-rose-500";
  if (status === BOOKING_STATUS.CANCELLED) return "border-l-red-400";
  if (status === BOOKING_STATUS.COMPLETED) return "border-l-slate-300";
  return "border-l-slate-200";
}

watch([filterStatus, filterResourceType, keyword], () => {
  currentPage.value = 1;
});

onMounted(async () => {
  const q = route.query.q || route.query.keyword;
  if (typeof q === "string" && q.trim()) keyword.value = q.trim();
  await loadBookings();
});
</script>

<template>
  <div class="flex h-full w-full flex-col bg-white">
    <div class="border-b border-slate-200 bg-gradient-to-r from-white to-slate-50/80 px-6 py-5">
      <h2 class="text-xl font-semibold text-slate-900">学生申请管理</h2>
      <p class="mt-1 max-w-2xl text-sm leading-relaxed text-slate-500">
        审批学生提交的预约申请，支持按状态筛选、查看申请人信息并完成审批流转。
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
      <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">
        <div class="grid w-full max-w-full grid-cols-[120px_120px_minmax(0,1fr)] items-center gap-3">
          <select v-model="filterStatus" class="input w-[120px]">
            <option v-for="opt in statusFilters" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
          <select v-model="filterResourceType" class="input w-[120px]">
            <option value="all">全部类型</option>
            <option value="lab">实验室</option>
            <option value="device">设备</option>
          </select>
          <input
            v-model="keyword"
            class="input min-w-0 w-full"
            placeholder="搜索资源名称、申请人、用途..."
          />
        </div>
        <div class="mt-3 flex justify-end">
          <button class="btn-secondary px-3 py-1.5 text-xs" @click="openReviewPanel">查看当前实验室评价</button>
        </div>
      </div>

      <div class="text-sm text-slate-600">共 {{ filtered.length }} 条学生申请（当前筛选条件）</div>

      <div v-if="!currentDbUser" class="rounded-2xl border border-dashed border-slate-200 bg-slate-50/80 px-6 py-12 text-center">
        <p class="text-sm font-medium text-slate-700">未登录或账号无效</p>
        <p class="mt-1 text-xs text-slate-500">请使用教师账号重新登录后查看待审预约。</p>
      </div>

      <div v-else class="min-h-0 flex-1 overflow-auto rounded-xl border border-slate-200 bg-white shadow-sm">
        <table class="w-full border-collapse text-sm">
          <thead class="bg-slate-100">
            <tr>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">资源名称</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">申请人</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">类型</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">预约时段</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">人数</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">用途说明</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">状态</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">审核意见</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr v-for="b in pagedRows" :key="b.id" class="transition hover:bg-slate-50">
              <td class="px-4 py-3 text-slate-900">{{ b.resourceName }}</td>
              <td class="px-4 py-3 text-slate-700">{{ applicantName(b.userId) }}</td>
              <td class="px-4 py-3 text-slate-700">{{ b.resourceType === "lab" ? "实验室" : "设备" }}</td>
              <td class="px-4 py-3 text-slate-700">{{ formatShort(b.startAt) }} ~ {{ formatShort(b.endAt) }}</td>
              <td class="px-4 py-3 text-slate-700">{{ b.participants }}</td>
              <td class="max-w-[260px] truncate px-4 py-3 text-slate-700" :title="b.purpose || '-'">{{ b.purpose || "-" }}</td>
              <td class="px-4 py-3">
                <span class="rounded-full px-2 py-0.5 text-xs font-medium" :class="statusPillClass(b.status)">{{ statusText(b.status) }}</span>
              </td>
              <td class="px-4 py-3">
                <input
                  class="input min-w-[180px]"
                  :value="getNote(b.id)"
                  placeholder="可填写审核意见"
                  @input="setNote(b.id, $event.target.value)"
                />
              </td>
              <td class="px-4 py-3">
                <div class="flex flex-wrap gap-2">
                  <button v-if="b.status === BOOKING_STATUS.PENDING" class="btn-primary px-3 py-1.5 text-xs" @click="approve(b.id)">通过</button>
                  <button
                    v-if="b.status === BOOKING_STATUS.PENDING"
                    class="btn-secondary border-rose-200 px-3 py-1.5 text-xs text-rose-700 hover:bg-rose-50"
                    @click="reject(b.id)"
                  >
                    驳回
                  </button>
                  <button v-if="b.status === BOOKING_STATUS.APPROVED" class="btn-secondary px-3 py-1.5 text-xs" @click="markCompleted(b.id)">
                    标记完成
                  </button>
                  <span v-if="![BOOKING_STATUS.PENDING, BOOKING_STATUS.APPROVED].includes(b.status)" class="text-xs text-slate-400">无需操作</span>
                </div>
              </td>
            </tr>
            <tr v-if="pagedRows.length === 0">
              <td colspan="9" class="px-6 py-12 text-center text-slate-500">暂无符合条件的学生申请</td>
            </tr>
          </tbody>
        </table>
      </div>

      <AppPagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="filtered.length" :page-size-options="[8,12,20]" />
    </div>

    <el-dialog v-model="reviewVisible" title="我的资源评价" width="760px" destroy-on-close>
      <div class="max-h-[520px] overflow-auto">
        <table class="w-full border-collapse text-sm">
          <thead class="bg-slate-100">
            <tr>
              <th class="px-3 py-2 text-left">资源</th>
              <th class="px-3 py-2 text-left">类型</th>
              <th class="px-3 py-2 text-left">用户</th>
              <th class="px-3 py-2 text-left">评分</th>
              <th class="px-3 py-2 text-left">评价</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr v-for="r in reviewRows" :key="r.id">
              <td class="px-3 py-2">{{ r.resourceName }}</td>
              <td class="px-3 py-2">{{ r.resourceType === "lab" ? "实验室" : "设备" }}</td>
              <td class="px-3 py-2">{{ r.userName }}</td>
              <td class="px-3 py-2">{{ r.rating }}</td>
              <td class="px-3 py-2">{{ r.content || "-" }}</td>
            </tr>
            <tr v-if="reviewRows.length === 0">
              <td colspan="5" class="px-6 py-10 text-center text-slate-500">暂无评价数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </el-dialog>
  </div>
</template>
