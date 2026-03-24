<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { BOOKING_STATUS, cancelBookingApi, listBookingsApi } from "../../services/bookings";
import { listDevicesApi, listLabsApi } from "../../services/resources";
import { getCurrentUser } from "../../services/session";

const router = useRouter();

// 筛选条件
const filterStatus = ref('all');
const filterType = ref('all');
const searchKeyword = ref('');

const currentUser = computed(() => getCurrentUser());
const currentDbUser = computed(() => currentUser.value || null);
const bookingsSource = ref([]);
const labsSource = ref([]);
const devicesSource = ref([]);

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

const bookings = computed(() => {
  if (!currentDbUser.value) return [];
  const list = bookingsSource.value;
  return list
    .slice()
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .map((b) => {
      const start = formatDateTime(b.startAt);
      const end = formatDateTime(b.endAt);
      const res =
        b.resourceType === "lab"
          ? labsSource.value.find((x) => x.id === b.resourceId)
          : devicesSource.value.find((x) => x.id === b.resourceId);
      const building = b.resourceType === "lab" ? res?.building || "-" : res?.location || "-";
      return {
        id: b.id,
        type: b.resourceType,
        resourceId: b.resourceId,
        resourceName: b.resourceName,
        building,
        date: start.date,
        startTime: start.time,
        endTime: end.time,
        purpose: b.purpose,
        participants: b.participants,
        status: b.status,
        createdAt: b.createdAt,
        isEmergency: b.isEmergency
      };
    });
});

// 状态配置
const statusConfig = {
  [BOOKING_STATUS.PENDING]: { text: "待审核", class: "bg-amber-100 text-amber-700" },
  [BOOKING_STATUS.APPROVED]: { text: "已确认", class: "bg-green-100 text-green-700" },
  [BOOKING_STATUS.REJECTED]: { text: "已驳回", class: "bg-rose-100 text-rose-700" },
  [BOOKING_STATUS.COMPLETED]: { text: "已完成", class: "bg-slate-100 text-slate-700" },
  [BOOKING_STATUS.CANCELLED]: { text: "已取消", class: "bg-red-100 text-red-700" }
};

// 筛选后的预约记录
const filteredBookings = computed(() => {
  return bookings.value.filter((booking) => {
    // 状态筛选
    if (filterStatus.value !== 'all' && booking.status !== filterStatus.value) {
      return false;
    }
    
    // 类型筛选
    if (filterType.value !== 'all' && booking.type !== filterType.value) {
      return false;
    }
    
    // 关键词搜索
    if (searchKeyword.value) {
      const keyword = searchKeyword.value.toLowerCase();
      return booking.resourceName.toLowerCase().includes(keyword) ||
             booking.building.toLowerCase().includes(keyword) ||
             booking.purpose.toLowerCase().includes(keyword);
    }
    
    return true;
  });
});

// 统计数据
const statistics = computed(() => {
  return {
    total: bookings.value.length,
    pending: bookings.value.filter((b) => b.status === BOOKING_STATUS.PENDING).length,
    approved: bookings.value.filter((b) => b.status === BOOKING_STATUS.APPROVED).length,
    rejected: bookings.value.filter((b) => b.status === BOOKING_STATUS.REJECTED).length,
    completed: bookings.value.filter((b) => b.status === BOOKING_STATUS.COMPLETED).length,
    cancelled: bookings.value.filter((b) => b.status === BOOKING_STATUS.CANCELLED).length
  };
});

// 取消预约
async function cancelBooking(booking) {
  if (booking.status === BOOKING_STATUS.COMPLETED || booking.status === BOOKING_STATUS.CANCELLED) {
    ElMessage.warning("该预约无法取消");
    return;
  }
  
  if (confirm(`确定要取消预约"${booking.resourceName}"吗？`)) {
    if (!currentDbUser.value?.id) return ElMessage.warning("未登录或账号无效");
    try {
      await cancelBookingApi(booking.id, { userId: currentDbUser.value.id, reason: "用户取消" });
      ElMessage.success("已取消预约");
      bookingsSource.value = await listBookingsApi({ userId: currentDbUser.value.id });
    } catch (e) {
      ElMessage.error(e.message || "取消失败");
    }
  }
}

// 查看详情
function viewDetail(booking) {
  router.push(`/student/booking-records/${booking.id}`);
}

// 重新预约
function rebookResource(booking) {
  router.push({
    path: '/student/booking-records/new',
    query: {
      type: booking.type,
      [booking.type === 'lab' ? 'labId' : 'deviceId']: booking.resourceId || booking.id,
      [booking.type === 'lab' ? 'labName' : 'deviceName']: booking.resourceName
    }
  });
}

onMounted(async () => {
  if (!currentDbUser.value?.id) return;
  const [bookings, labs, devices] = await Promise.all([
    listBookingsApi({ userId: currentDbUser.value.id }),
    listLabsApi(),
    listDevicesApi()
  ]);
  bookingsSource.value = bookings;
  labsSource.value = labs;
  devicesSource.value = devices;
});
</script>

<template>
  <div class="flex h-full flex-col bg-slate-50">
    <!-- 如果是子路由（新建预约或详情），显示子路由内容 -->
    <router-view v-if="$route.path.includes('/new') || $route.params.id" />
    
    <!-- 否则显示预约记录列表 -->
    <template v-else>
    <!-- 顶部标题栏 -->
    <div class="border-b border-slate-200 bg-white px-6 py-4 shadow-sm">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-xl font-semibold text-slate-900">我的预约记录</h2>
          <p class="mt-1 text-sm text-slate-600">查看和管理您的所有预约</p>
        </div>
        <button class="btn-primary" @click="router.push('/student/booking-records/new')">
          新建预约
        </button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="flex-shrink-0 border-b border-slate-200 bg-white px-6 py-4">
        <div class="grid grid-cols-5 gap-4">
        <div class="rounded-lg border border-slate-200 bg-slate-50 p-3 text-center">
          <div class="text-2xl font-bold text-slate-900">{{ statistics.total }}</div>
          <div class="mt-1 text-xs text-slate-600">总预约</div>
        </div>
        <div class="rounded-lg border border-amber-200 bg-amber-50 p-3 text-center">
          <div class="text-2xl font-bold text-amber-700">{{ statistics.pending }}</div>
          <div class="mt-1 text-xs text-amber-600">待审核</div>
        </div>
        <div class="rounded-lg border border-green-200 bg-green-50 p-3 text-center">
            <div class="text-2xl font-bold text-green-700">{{ statistics.approved }}</div>
          <div class="mt-1 text-xs text-green-600">已确认</div>
        </div>
        <div class="rounded-lg border border-slate-200 bg-slate-50 p-3 text-center">
          <div class="text-2xl font-bold text-slate-700">{{ statistics.completed }}</div>
          <div class="mt-1 text-xs text-slate-600">已完成</div>
        </div>
        <div class="rounded-lg border border-red-200 bg-red-50 p-3 text-center">
          <div class="text-2xl font-bold text-red-700">{{ statistics.cancelled }}</div>
          <div class="mt-1 text-xs text-red-600">已取消</div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="flex-shrink-0 border-b border-slate-200 bg-white px-6 py-3">
      <div class="flex gap-3">
        <select v-model="filterStatus" class="input flex-1">
          <option value="all">全部状态</option>
          <option value="pending">待审核</option>
          <option value="approved">已确认</option>
          <option value="rejected">已驳回</option>
          <option value="completed">已完成</option>
          <option value="cancelled">已取消</option>
        </select>
        
        <select v-model="filterType" class="input flex-1">
          <option value="all">全部类型</option>
          <option value="lab">实验室</option>
          <option value="device">设备</option>
        </select>
        
        <input
          v-model="searchKeyword"
          class="input flex-1"
          placeholder="搜索资源名称、楼宇或目的..."
        />
      </div>
    </div>

    <!-- 预约记录列表 -->
    <div class="min-h-0 flex-1 overflow-auto p-6">
      <div class="space-y-3">
        <div
          v-for="booking in filteredBookings"
          :key="booking.id"
          class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm transition hover:shadow-md"
        >
          <div class="flex items-start justify-between">
            <div class="flex-1">
              <div class="flex items-center gap-2">
                <h3 class="text-base font-semibold text-slate-900">{{ booking.resourceName }}</h3>
                <span
                  class="rounded-full px-2 py-0.5 text-xs font-medium"
                  :class="statusConfig[booking.status].class"
                >
                  {{ statusConfig[booking.status].text }}
                </span>
                <span
                  v-if="booking.isEmergency"
                  class="rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700"
                >
                  紧急
                </span>
                <span
                  class="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700"
                >
                  {{ booking.type === 'lab' ? '实验室' : '设备' }}
                </span>
              </div>
              
              <div class="mt-2 grid grid-cols-2 gap-x-6 gap-y-1 text-sm">
                <div class="flex items-center gap-2 text-slate-600">
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  <span>{{ booking.building }}</span>
                </div>
                
                <div class="flex items-center gap-2 text-slate-600">
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                  <span>{{ booking.date }}</span>
                </div>
                
                <div class="flex items-center gap-2 text-slate-600">
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span>{{ booking.startTime }} - {{ booking.endTime }}</span>
                </div>
                
                <div class="flex items-center gap-2 text-slate-600">
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                  <span>{{ booking.participants }} 人</span>
                </div>
              </div>
              
              <div class="mt-2 text-sm text-slate-600">
                <span class="font-medium">使用目的：</span>{{ booking.purpose }}
              </div>
              
              <div class="mt-1 text-xs text-slate-500">
                预约时间：{{ booking.createdAt }}
              </div>
            </div>
            
            <div class="ml-4 flex flex-col gap-2">
              <button
                @click="viewDetail(booking)"
                class="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-700 transition hover:bg-slate-50"
              >
                查看详情
              </button>
              <button
                v-if="booking.status === BOOKING_STATUS.PENDING || booking.status === BOOKING_STATUS.APPROVED"
                @click="cancelBooking(booking)"
                class="rounded-lg border border-red-200 bg-white px-3 py-1.5 text-xs font-medium text-red-700 transition hover:bg-red-50"
              >
                取消预约
              </button>
              <button
                v-if="booking.status === BOOKING_STATUS.CANCELLED || booking.status === BOOKING_STATUS.COMPLETED"
                @click="rebookResource(booking)"
                class="rounded-lg border border-brand-200 bg-white px-3 py-1.5 text-xs font-medium text-brand-700 transition hover:bg-brand-50"
              >
                重新预约
              </button>
            </div>
          </div>
        </div>
        
        <!-- 空状态 -->
        <div v-if="filteredBookings.length === 0" class="py-12 text-center">
          <svg class="mx-auto h-12 w-12 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
          <p class="mt-2 text-sm text-slate-600">暂无预约记录</p>
          <button @click="router.push('/student/booking-records/new')" class="btn-primary mt-4">
            立即预约
          </button>
        </div>
      </div>
    </div>
    </template>
  </div>
</template>
