<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { listBookingsApi } from "../../services/bookings";
import { listDevicesApi, listLabsApi } from "../../services/resources";
import { getCurrentUser } from "../../services/session";

const route = useRoute();
const router = useRouter();

const booking = ref(null);
const loading = ref(true);

// 状态配置
const statusConfig = {
  pending: { 
    text: '待审核', 
    class: 'bg-amber-100 text-amber-700 border-amber-200',
    icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z'
  },
  approved: { 
    text: '已确认', 
    class: 'bg-green-100 text-green-700 border-green-200',
    icon: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z'
  },
  rejected: {
    text: "已驳回",
    class: "bg-rose-100 text-rose-700 border-rose-200",
    icon: "M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"
  },
  completed: { 
    text: '已完成', 
    class: 'bg-slate-100 text-slate-700 border-slate-200',
    icon: 'M5 13l4 4L19 7'
  },
  cancelled: { 
    text: '已取消', 
    class: 'bg-red-100 text-red-700 border-red-200',
    icon: 'M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z'
  }
};

const currentUser = computed(() => getCurrentUser());
const currentDbUser = computed(() => currentUser.value || null);

onMounted(() => {
  loadBookingDetail();
});

function pad2(n) {
  return String(n).padStart(2, "0");
}

function formatDateTime(iso) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return { date: "-", time: "-" };
  const date = `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
  const time = `${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
  return { date, time };
}

async function loadBookingDetail() {
  loading.value = true;

  const bookingId = Number(route.params.id);
  if (!currentDbUser.value?.id) {
    booking.value = null;
    loading.value = false;
    return;
  }
  const [bookings, labs, devices] = await Promise.all([
    listBookingsApi({ userId: currentDbUser.value.id }),
    listLabsApi(),
    listDevicesApi()
  ]);
  const raw = bookings.find((x) => x.id === bookingId);
  if (!raw) {
    booking.value = null;
    loading.value = false;
    return;
  }

  if (currentDbUser.value && raw.createdByUserId !== currentDbUser.value.id) {
    booking.value = null;
    loading.value = false;
    return;
  }

  const start = formatDateTime(raw.startAt);
  const end = formatDateTime(raw.endAt);
  const res =
    raw.resourceType === "lab"
      ? labs.find((x) => x.id === raw.resourceId)
      : devices.find((x) => x.id === raw.resourceId);

  booking.value = {
    id: raw.id,
    type: raw.resourceType,
    resourceId: raw.resourceId,
    resourceName: raw.resourceName,
    building: raw.resourceType === "lab" ? res?.building || "-" : res?.location || "-",
    floor: raw.resourceType === "lab" ? "-" : "-",
    roomNumber: raw.resourceType === "lab" ? "-" : "-",
    date: start.date,
    startTime: start.time,
    endTime: end.time,
    purpose: raw.purpose,
    participants: raw.participants,
    status: raw.status,
    createdAt: raw.createdAt,
    confirmedAt: raw.status === "approved" ? raw.updatedAt : "",
    isEmergency: raw.isEmergency,
    approver: "教师审核",
    approverComment: raw.reviewNote || "",
    contactPhone: "-",
    contactEmail: currentDbUser.value?.email || "",
    equipment: [],
    notes: "",
    qrCode: `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=BOOKING-${bookingId}`
  };

  loading.value = false;
}

// 取消预约
function cancelBooking() {
  if (booking.value.status === "completed" || booking.value.status === "cancelled") {
    ElMessage.warning("该预约无法取消");
    return;
  }
  
  if (confirm(`确定要取消预约"${booking.value.resourceName}"吗？\n取消后需要重新预约。`)) {
    ElMessage.warning("当前后端未开放学生端取消预约接口，建议联系教师处理");
  }
}

// 打印预约单
function printBooking() {
  window.print();
}

// 导出为PDF
function exportPDF() {
  ElMessage.info("导出PDF功能开发中...");
}

// 重新预约
function rebookResource() {
  router.push({
    path: '/student/booking-records/new',
    query: {
      type: booking.value.type,
      [booking.value.type === "lab" ? "labId" : "deviceId"]: booking.value.resourceId,
      [booking.value.type === "lab" ? "labName" : "deviceName"]: booking.value.resourceName
    }
  });
}

// 计算预约时长
const duration = computed(() => {
  if (!booking.value) return '';
  const start = booking.value.startTime.split(':');
  const end = booking.value.endTime.split(':');
  const startMinutes = parseInt(start[0]) * 60 + parseInt(start[1]);
  const endMinutes = parseInt(end[0]) * 60 + parseInt(end[1]);
  const diff = endMinutes - startMinutes;
  const hours = Math.floor(diff / 60);
  const minutes = diff % 60;
  return hours > 0 ? `${hours}小时${minutes}分钟` : `${minutes}分钟`;
});
</script>

<template>
  <div class="flex h-full flex-col bg-slate-50">
    <!-- 加载状态 -->
    <div v-if="loading" class="flex h-full items-center justify-center">
      <div class="text-center">
        <div class="mx-auto h-12 w-12 animate-spin rounded-full border-4 border-brand-200 border-t-brand-600"></div>
        <p class="mt-4 text-sm text-slate-600">加载中...</p>
      </div>
    </div>

    <!-- 详情内容 -->
    <template v-else-if="booking">
      <!-- 顶部标题栏 -->
      <div class="border-b border-slate-200 bg-white px-6 py-4 shadow-sm">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <button @click="router.back()" class="text-slate-600 hover:text-slate-900">
              <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <div>
              <h2 class="text-xl font-semibold text-slate-900">预约详情</h2>
              <p class="mt-0.5 text-sm text-slate-600">预约编号：#{{ booking.id }}</p>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <button @click="printBooking" class="btn-secondary text-sm">
              打印
            </button>
            <button @click="exportPDF" class="btn-secondary text-sm">
              导出PDF
            </button>
          </div>
        </div>
      </div>

      <!-- 主内容区域 -->
      <div class="flex min-h-0 flex-1 gap-4 overflow-auto p-6">
        <!-- 左侧：详细信息 -->
        <div class="flex min-h-0 flex-1 flex-col gap-4">
          <!-- 状态卡片 -->
        <div class="rounded-xl border bg-white p-6 shadow-sm" :class="statusConfig[booking.status].class">
            <div class="flex items-center gap-3">
              <svg class="h-8 w-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="statusConfig[booking.status].icon" />
              </svg>
              <div>
                <div class="text-lg font-semibold">{{ statusConfig[booking.status].text }}</div>
                <div class="mt-0.5 text-sm opacity-80">
                  {{ booking.status === 'approved' ? '预约已确认，请按时到达' : 
                     booking.status === 'pending' ? '等待老师审核中' :
                     booking.status === 'rejected' ? '预约已被驳回' :
                     booking.status === 'completed' ? '预约已完成' : '预约已取消' }}
                </div>
              </div>
            </div>
          </div>

          <!-- 资源信息 -->
          <div class="rounded-xl border border-slate-200 bg-white shadow-sm">
            <div class="border-b border-slate-100 px-6 py-3">
              <h3 class="font-semibold text-slate-900">资源信息</h3>
            </div>
            <div class="p-6">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="text-xs text-slate-500">资源名称</label>
                  <div class="mt-1 font-medium text-slate-900">{{ booking.resourceName }}</div>
                </div>
                <div>
                  <label class="text-xs text-slate-500">资源类型</label>
                  <div class="mt-1 font-medium text-slate-900">
                  {{ booking.type === 'lab' ? '实验室' : '设备' }}
                  </div>
                </div>
                <div>
                  <label class="text-xs text-slate-500">所在楼宇</label>
                  <div class="mt-1 font-medium text-slate-900">{{ booking.building }}</div>
                </div>
                <div>
                  <label class="text-xs text-slate-500">楼层/房间号</label>
                  <div class="mt-1 font-medium text-slate-900">{{ booking.floor }} / {{ booking.roomNumber }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 预约时间 -->
          <div class="rounded-xl border border-slate-200 bg-white shadow-sm">
            <div class="border-b border-slate-100 px-6 py-3">
              <h3 class="font-semibold text-slate-900">预约时间</h3>
            </div>
            <div class="p-6">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="text-xs text-slate-500">预约日期</label>
                  <div class="mt-1 font-medium text-slate-900">{{ booking.date }}</div>
                </div>
                <div>
                  <label class="text-xs text-slate-500">时间段</label>
                  <div class="mt-1 font-medium text-slate-900">
                    {{ booking.startTime }} - {{ booking.endTime }}
                  </div>
                </div>
                <div>
                  <label class="text-xs text-slate-500">使用时长</label>
                  <div class="mt-1 font-medium text-slate-900">{{ duration }}</div>
                </div>
                <div>
                  <label class="text-xs text-slate-500">参与人数</label>
                  <div class="mt-1 font-medium text-slate-900">{{ booking.participants }} 人</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 使用信息 -->
          <div class="rounded-xl border border-slate-200 bg-white shadow-sm">
            <div class="border-b border-slate-100 px-6 py-3">
              <h3 class="font-semibold text-slate-900">使用信息</h3>
            </div>
            <div class="p-6">
              <div class="space-y-4">
                <div>
                  <label class="text-xs text-slate-500">使用目的</label>
                  <div class="mt-1 text-slate-900">{{ booking.purpose }}</div>
                </div>
                <div v-if="booking.equipment.length > 0">
                  <label class="text-xs text-slate-500">所需设备</label>
                  <div class="mt-2 flex flex-wrap gap-2">
                    <span
                      v-for="(item, index) in booking.equipment"
                      :key="index"
                      class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-700"
                    >
                      {{ item }}
                    </span>
                  </div>
                </div>
                <div v-if="booking.notes">
                  <label class="text-xs text-slate-500">备注说明</label>
                  <div class="mt-1 rounded-lg bg-amber-50 p-3 text-sm text-amber-900">
                    {{ booking.notes }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 审核信息 -->
          <div v-if="booking.status === 'approved' || booking.status === 'completed' || booking.status === 'rejected'" class="rounded-xl border border-slate-200 bg-white shadow-sm">
            <div class="border-b border-slate-100 px-6 py-3">
              <h3 class="font-semibold text-slate-900">审核信息</h3>
            </div>
            <div class="p-6">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="text-xs text-slate-500">审核人</label>
                  <div class="mt-1 font-medium text-slate-900">{{ booking.approver }}</div>
                </div>
                <div>
                  <label class="text-xs text-slate-500">审核时间</label>
                  <div class="mt-1 font-medium text-slate-900">{{ booking.confirmedAt }}</div>
                </div>
                <div class="col-span-2">
                  <label class="text-xs text-slate-500">审核意见</label>
                  <div class="mt-1 text-slate-900">{{ booking.approverComment }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：操作和二维码 -->
        <div class="flex w-80 flex-shrink-0 flex-col gap-4">
          <!-- 二维码 -->
          <div v-if="booking.status === 'approved'" class="rounded-xl border border-slate-200 bg-white p-6 text-center shadow-sm">
            <h3 class="font-semibold text-slate-900">入场二维码</h3>
            <p class="mt-1 text-xs text-slate-600">请在入场时出示此二维码</p>
            <img :src="booking.qrCode" alt="入场二维码" class="mx-auto mt-4 h-48 w-48" />
            <div class="mt-4 rounded-lg bg-slate-50 p-3 text-xs text-slate-600">
              预约编号：#{{ booking.id }}
            </div>
          </div>

          <!-- 联系信息 -->
          <div class="rounded-xl border border-slate-200 bg-white shadow-sm">
            <div class="border-b border-slate-100 px-4 py-3">
              <h3 class="text-sm font-semibold text-slate-900">联系信息</h3>
            </div>
            <div class="p-4">
              <div class="space-y-3">
                <div>
                  <label class="text-xs text-slate-500">联系电话</label>
                  <div class="mt-1 text-sm text-slate-900">{{ booking.contactPhone }}</div>
                </div>
                <div>
                  <label class="text-xs text-slate-500">联系邮箱</label>
                  <div class="mt-1 text-sm text-slate-900">{{ booking.contactEmail }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
            <div class="space-y-2">
              <button
                v-if="booking.status === 'pending' || booking.status === 'approved'"
                @click="cancelBooking"
                class="btn-secondary w-full border-red-200 text-red-700 hover:bg-red-50"
              >
                取消预约
              </button>
              <button
                v-if="booking.status === 'cancelled' || booking.status === 'completed' || booking.status === 'rejected'"
                @click="rebookResource"
                class="btn-primary w-full"
              >
                重新预约
              </button>
            </div>
          </div>

          <!-- 时间线 -->
          <div class="rounded-xl border border-slate-200 bg-white shadow-sm">
            <div class="border-b border-slate-100 px-4 py-3">
              <h3 class="text-sm font-semibold text-slate-900">预约时间线</h3>
            </div>
            <div class="p-4">
              <div class="space-y-3">
                <div class="flex gap-3">
                  <div class="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full bg-brand-100">
                    <div class="h-2 w-2 rounded-full bg-brand-600"></div>
                  </div>
                  <div class="flex-1">
                    <div class="text-sm font-medium text-slate-900">提交预约</div>
                    <div class="text-xs text-slate-500">{{ booking.createdAt }}</div>
                  </div>
                </div>
                <div v-if="booking.confirmedAt" class="flex gap-3">
                  <div class="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full bg-green-100">
                    <div class="h-2 w-2 rounded-full bg-green-600"></div>
                  </div>
                  <div class="flex-1">
                    <div class="text-sm font-medium text-slate-900">审核处理</div>
                    <div class="text-xs text-slate-500">{{ booking.confirmedAt }}</div>
                  </div>
                </div>
                <div v-if="booking.status === 'approved'" class="flex gap-3">
                  <div class="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full bg-slate-100">
                    <div class="h-2 w-2 rounded-full bg-slate-400"></div>
                  </div>
                  <div class="flex-1">
                    <div class="text-sm font-medium text-slate-900">待使用</div>
                    <div class="text-xs text-slate-500">{{ booking.date }} {{ booking.startTime }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 未找到 -->
    <div v-else class="flex h-full items-center justify-center">
      <div class="text-center">
        <svg class="mx-auto h-12 w-12 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <p class="mt-2 text-sm text-slate-600">未找到预约信息</p>
        <button @click="router.back()" class="btn-primary mt-4">
          返回
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
@media print {
  .btn-secondary,
  button {
    display: none;
  }
}
</style>
