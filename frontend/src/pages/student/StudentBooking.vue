<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { checkBookingConflictApi, createBookingApi } from "../../services/bookings";
import { listDevicesApi, listLabsApi } from "../../services/resources";
import { APPLICATION_STATUS, listApplicationsApi } from "../../services/applications";
import { getCurrentUser } from "../../services/session";

const route = useRoute();
const router = useRouter();

// 是否从详情页跳转（锁定资源）
const isLocked = ref(false);

// 表单数据
const form = ref({
  resourceType: 'lab',
  resourceId: null,
  resourceName: '',
  startTime: '',
  endTime: '',
  purpose: '',
  participants: 1,
  isEmergency: false
});

// 可用资源列表
const availableResources = ref([]);
const searchKeyword = ref('');

// 时间段选择
const selectedDate = ref('');
const selectedEndDate = ref('');
const selectedStartTime = ref('');
const selectedEndTime = ref('');
const selectedSlotPreset = ref('');

// 冲突检测结果
const conflictCheck = ref(null);
const showConflict = ref(false);

const currentUser = computed(() => getCurrentUser());
const currentDbUser = computed(() => currentUser.value || null);
const labsSource = ref([]);
const devicesSource = ref([]);
const applicationsSource = ref([]);

function parseLabApplyMeta(app) {
  const detail = app?.detail || "";
  const txt = String(detail || "");
  const rows = txt.split("\n");
  const get = (k) => rows.find((r) => r.startsWith(`${k}：`) || r.startsWith(`${k}:`))?.split(/：|:/).slice(1).join(":").trim() || "";
  return {
    labId: Number(get("labId")) || null,
    openDateStart: app?.openDateStart || "",
    openDateEnd: app?.openDateEnd || "",
    dailyStartTime: app?.dailyStartTime || "",
    dailyEndTime: app?.dailyEndTime || ""
  };
}

function inOpenWindow(meta, dateStr, startTime, endTime) {
  if (!meta.openDateStart || !meta.openDateEnd || !meta.dailyStartTime || !meta.dailyEndTime) return false;
  if (dateStr < meta.openDateStart || dateStr > meta.openDateEnd) return false;
  return startTime >= meta.dailyStartTime && endTime <= meta.dailyEndTime && startTime < endTime;
}

const approvedLabApplyMap = computed(() => {
  const map = new Map();
  for (const app of applicationsSource.value || []) {
    if (app.type !== "lab_apply" || app.status !== APPLICATION_STATUS.APPROVED) continue;
    const meta = parseLabApplyMeta(app);
    if (!meta.labId) continue;
    map.set(meta.labId, meta);
  }
  return map;
});

// 从路由参数初始化
onMounted(() => {
  // 检查是否从详情页跳转（带有资源ID）
  if (route.query.labId || route.query.deviceId) {
    isLocked.value = true;
    
    if (route.query.labId) {
      form.value.resourceId = Number(route.query.labId);
      form.value.resourceName = route.query.labName || '';
      form.value.resourceType = route.query.type || 'lab';
    } else if (route.query.deviceId) {
      form.value.resourceId = Number(route.query.deviceId);
      form.value.resourceName = route.query.deviceName || '';
      form.value.resourceType = 'device';
    }
  }
  
  Promise.all([listLabsApi(), listDevicesApi(), listApplicationsApi()]).then(([labs, devices, applications]) => {
    labsSource.value = labs;
    devicesSource.value = devices;
    applicationsSource.value = applications;
    loadAvailableResources();
  });
  
  // 设置默认日期为今天
  selectedDate.value = localDateString(new Date());
  selectedEndDate.value = selectedDate.value;
});

// 加载可用资源
function loadAvailableResources() {
  const keyword = searchKeyword.value.trim();
  if (form.value.resourceType === "lab") {
    const all = labsSource.value;
    availableResources.value = all.filter((lab) => {
      const hasApprovedApply = approvedLabApplyMap.value.has(lab.id);
      const isAvailable = lab.status !== "maintenance";
      const byKeyword =
        !keyword || lab.name.includes(keyword) || lab.building.includes(keyword) || lab.college.includes(keyword);
      return hasApprovedApply && isAvailable && byKeyword;
    });
    return;
  }

  const all = devicesSource.value;
  availableResources.value = all.filter((device) => {
    const isAvailable = device.status === "available";
    const byKeyword = !keyword || device.name.includes(keyword) || device.location.includes(keyword);
    return isAvailable && byKeyword;
  });
}

const slotPresets = [
  { label: "上午第一段（08:00-10:00）", start: "08:00", end: "10:00" },
  { label: "上午第二段（10:00-12:00）", start: "10:00", end: "12:00" },
  { label: "下午第一段（14:00-16:00）", start: "14:00", end: "16:00" },
  { label: "下午第二段（16:00-18:00）", start: "16:00", end: "18:00" },
  { label: "晚间时段（19:00-21:00）", start: "19:00", end: "21:00" }
];

// 生成时间选项（15分钟间隔）
const timeSlots = computed(() => {
  const slots = [];
  for (let hour = 8; hour <= 21; hour++) {
    for (let minute = 0; minute < 60; minute += 15) {
      const timeStr = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
      slots.push(timeStr);
    }
  }
  return slots;
});

const endTimeSlots = computed(() => {
  if (!selectedStartTime.value) return timeSlots.value;
  return timeSlots.value.filter((x) => x > selectedStartTime.value);
});

// 选择资源
function selectResource(resource) {
  if (isLocked.value) return; // 锁定状态不允许切换
  form.value.resourceId = resource.id;
  form.value.resourceName = resource.name;
}

function combineToIso(dateStr, timeStr) {
  // 后端使用 LocalDateTime，避免 toISOString 的时区偏移
  return `${dateStr}T${timeStr}:00`;
}

function localDateString(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function choosePreset() {
  if (!selectedSlotPreset.value) return;
  const preset = slotPresets.find((x) => x.label === selectedSlotPreset.value);
  if (!preset) return;
  selectedStartTime.value = preset.start;
  selectedEndTime.value = preset.end;
}

// 冲突检测
async function checkConflict() {
  if (!validateForm()) return;

  const startAt = combineToIso(selectedDate.value, selectedStartTime.value);
  const endAt = combineToIso(selectedEndDate.value, selectedEndTime.value);
  const result = await checkBookingConflictApi({
    resourceType: form.value.resourceType,
    resourceId: Number(form.value.resourceId),
    startAt,
    endAt
  });

  conflictCheck.value = result;
  showConflict.value = true;
  
  setTimeout(() => {
    showConflict.value = false;
  }, 3000);
}

// 提交预约
async function submitBooking() {
  if (!validateForm()) return;

  if (!currentDbUser.value) {
    ElMessage.error("未登录或账号无效，请重新登录");
    router.replace("/auth/login");
    return;
  }

  const startAt = combineToIso(selectedDate.value, selectedStartTime.value);
  const endAt = combineToIso(selectedEndDate.value, selectedEndTime.value);

  try {
    const purposeWithSlot = selectedSlotPreset.value
      ? `【${selectedSlotPreset.value}】${form.value.purpose}`
      : form.value.purpose;
    await createBookingApi({
      createdByUserId: currentDbUser.value.id,
      resourceType: form.value.resourceType,
      resourceId: Number(form.value.resourceId),
      startAt,
      endAt,
      purpose: purposeWithSlot,
      participants: Number(form.value.participants) || 1,
      isEmergency: !!form.value.isEmergency
    });
    ElMessage.success("预约已提交，等待审核");
    router.push("/student/booking-records");
  } catch (e) {
    ElMessage.error(e.message || "提交失败");
  }
}

// 表单验证
function validateForm() {
  if (!form.value.resourceId) {
    ElMessage.warning("请选择资源");
    return false;
  }
  if (!selectedDate.value || !selectedEndDate.value || !selectedStartTime.value || !selectedEndTime.value) {
    ElMessage.warning("请选择预约时间");
    return false;
  }
  const now = new Date();
  const start = new Date(`${selectedDate.value}T${selectedStartTime.value}:00`);
  const end = new Date(`${selectedEndDate.value}T${selectedEndTime.value}:00`);
  if (start.getTime() <= now.getTime()) {
    ElMessage.warning("开始时间必须晚于当前时间");
    return false;
  }
  if (end.getTime() <= start.getTime()) {
    ElMessage.warning("结束时间必须晚于开始时间");
    return false;
  }
  if (!form.value.purpose.trim()) {
    ElMessage.warning("请填写使用目的");
    return false;
  }
  
  // 组合完整时间
  form.value.startTime = `${selectedDate.value} ${selectedStartTime.value}`;
  form.value.endTime = `${selectedEndDate.value} ${selectedEndTime.value}`;

  if (form.value.resourceType === "lab") {
    if (selectedDate.value !== selectedEndDate.value) {
      ElMessage.warning("实验室预约暂仅支持同一天内时段");
      return false;
    }
    const meta = approvedLabApplyMap.value.get(Number(form.value.resourceId));
    if (!meta) {
      ElMessage.warning("该实验室未在教师开放申请范围内，暂不可预约");
      return false;
    }
    if (!inOpenWindow(meta, selectedDate.value, selectedStartTime.value, selectedEndTime.value)) {
      ElMessage.warning("预约时间不在教师配置的开放时段内");
      return false;
    }
  }
  
  return true;
}

// 切换资源类型
function changeResourceType() {
  if (isLocked.value) return; // 锁定状态不允许切换
  form.value.resourceId = null;
  form.value.resourceName = '';
  searchKeyword.value = '';
  loadAvailableResources();
}

// 取消预约，返回上一页
function cancelBooking() {
  router.back();
}

watch([() => form.value.resourceType, searchKeyword], () => {
  loadAvailableResources();
});

watch(selectedStartTime, (val) => {
  if (!val) return;
  if (selectedDate.value === selectedEndDate.value && selectedEndTime.value && selectedEndTime.value <= val) {
    selectedEndTime.value = "";
  }
});

watch(selectedDate, (val) => {
  if (!val) return;
  if (!selectedEndDate.value || selectedEndDate.value < val) {
    selectedEndDate.value = val;
  }
});

</script>

<template>
  <div class="flex h-full flex-col bg-slate-50">
    <!-- 顶部标题栏 -->
    <div class="border-b border-slate-200 bg-white px-6 py-4 shadow-sm">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-xl font-semibold text-slate-900">
            {{ isLocked ? '预约资源' : '实验室预约' }}
          </h2>
          <p class="mt-1 text-sm text-slate-600">
            {{ isLocked ? '完成预约信息填写' : '选择实验室和时间段进行预约' }}
          </p>
        </div>
        <div class="flex gap-2">
          <button v-if="!isLocked" class="btn-secondary" @click="router.push('/student/booking-records')">
            查看预约记录
          </button>
        </div>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="min-h-0 flex-1 p-4 lg:p-6">
      <div
        class="mx-auto grid min-h-0 w-full max-w-7xl gap-4"
        :class="isLocked ? 'lg:grid-cols-1 lg:justify-items-center' : 'lg:grid-cols-[420px,minmax(720px,1fr)]'"
      >
      <!-- 左侧：资源选择（锁定时隐藏） -->
      <div 
        v-if="!isLocked"
        class="flex min-h-0 flex-col overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm"
      >
        <div class="flex-shrink-0 border-b border-slate-100 px-5 py-4">
          <h3 class="text-base font-semibold text-slate-900">选择资源</h3>
          <p class="mt-1 text-xs text-slate-500">先选择资源，再填写预约信息</p>
        </div>
        
        <div class="flex-shrink-0 border-b border-slate-100 px-5 py-4">
          <div class="grid grid-cols-2 gap-2">
            <button
              type="button"
              class="rounded-lg border px-3 py-2 text-sm font-medium transition"
              :class="form.resourceType === 'lab' ? 'border-brand-200 bg-brand-50 text-brand-700' : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'"
              @click="form.resourceType = 'lab'; changeResourceType()"
            >
              实验室
            </button>
            <button
              type="button"
              class="rounded-lg border px-3 py-2 text-sm font-medium transition"
              :class="form.resourceType === 'device' ? 'border-brand-200 bg-brand-50 text-brand-700' : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'"
              @click="form.resourceType = 'device'; changeResourceType()"
            >
              设备
            </button>
          </div>

          <input
            v-model="searchKeyword"
            @input="loadAvailableResources"
            class="input mt-3 text-sm"
            placeholder="搜索名称 / 楼宇 / 学院..."
          />

          <div class="mt-3 flex items-center justify-between text-xs text-slate-500">
            <span>共 {{ availableResources.length }} 个可用（实验室仅显示教师已开放）</span>
            <button
              type="button"
              class="text-brand-700 hover:text-brand-800"
              @click="searchKeyword=''; loadAvailableResources()"
            >
              清空筛选
            </button>
          </div>
        </div>

        <div class="min-h-0 flex-1 overflow-auto px-5 py-4">
          <div v-if="availableResources.length === 0" class="rounded-lg border border-dashed border-slate-200 bg-slate-50 p-6 text-center">
            <div class="text-sm font-medium text-slate-800">暂无可用资源</div>
            <div class="mt-1 text-xs text-slate-500">尝试更换类型或清空搜索关键词</div>
          </div>

          <div v-else class="space-y-2">
            <button
              v-for="resource in availableResources"
              :key="resource.id"
              @click="selectResource(resource)"
              class="w-full rounded-xl border p-3 text-left transition"
              :class="form.resourceId === resource.id 
                ? 'border-brand-500 bg-brand-50 shadow-sm' 
                : 'border-slate-200 bg-white hover:border-brand-300 hover:shadow-sm'"
            >
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0 flex-1">
                  <div class="truncate font-medium text-slate-900">{{ resource.name }}</div>
                  <div class="mt-1 text-xs text-slate-600">
                    {{ form.resourceType === 'lab' ? resource.building : resource.location }}
                  </div>
                  <div v-if="form.resourceType === 'lab'" class="mt-1 text-xs text-slate-500">
                    容量: {{ resource.capacity }} 人 · {{ resource.college }}
                  </div>
                </div>
                <div
                  class="mt-0.5 inline-flex h-6 items-center rounded-full px-2 text-[11px] font-medium"
                  :class="form.resourceId === resource.id ? 'bg-brand-100 text-brand-700' : 'bg-slate-100 text-slate-600'"
                >
                  {{ form.resourceId === resource.id ? '已选择' : '可预约' }}
                </div>
              </div>
            </button>
          </div>
        </div>
      </div>

      <!-- 右侧：预约表单 -->
      <div 
        class="flex min-h-0 flex-col overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm"
        :class="isLocked ? 'mx-auto w-full max-w-4xl' : ''"
      >
        <div class="flex-shrink-0 border-b border-slate-100 px-6 py-4">
          <div class="flex items-start justify-between gap-4">
            <div>
              <h3 class="text-base font-semibold text-slate-900">预约信息</h3>
              <p class="mt-1 text-xs text-slate-500">填写完成后即可提交预约</p>
            </div>
            <div
              v-if="showConflict"
              class="rounded-lg border px-3 py-2 text-xs font-medium"
              :class="conflictCheck?.hasConflict 
                ? 'border-red-200 bg-red-50 text-red-700' 
                : 'border-green-200 bg-green-50 text-green-700'"
            >
              {{ conflictCheck?.message }}
            </div>
          </div>
        </div>

        <div class="min-h-0 flex-1 overflow-auto px-6 py-5">
          <div class="grid gap-4 lg:grid-cols-2">
            <!-- 资源 -->
            <div class="rounded-xl border border-slate-200 bg-white p-4">
              <div class="flex items-center justify-between">
                <h4 class="text-sm font-semibold text-slate-900">资源</h4>
                <span v-if="isLocked" class="inline-flex items-center gap-1 rounded-full bg-brand-50 px-2 py-0.5 text-xs font-medium text-brand-700">
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                  已锁定
                </span>
              </div>

              <div class="mt-3 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm">
                <div class="text-xs text-slate-500">已选资源</div>
                <div class="mt-1 font-medium text-slate-900">
                  {{ form.resourceName || '请先选择一个资源' }}
                </div>
              </div>

              <div class="mt-3 grid grid-cols-2 gap-3">
                <div>
                  <label class="block text-xs font-medium text-slate-600">参与人数</label>
                  <input
                    v-model.number="form.participants"
                    type="number"
                    min="1"
                    class="input mt-1"
                  />
                </div>
                <div class="flex items-end">
                  <label class="mt-0.5 flex w-full items-center justify-between rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm">
                    <span class="text-slate-700">紧急预约</span>
                    <input v-model="form.isEmergency" type="checkbox" class="h-4 w-4" />
                  </label>
                </div>
              </div>
            </div>

            <!-- 时间 -->
            <div class="rounded-xl border border-slate-200 bg-white p-4">
              <div class="flex items-center justify-between">
                <h4 class="text-sm font-semibold text-slate-900">时间</h4>
                <button type="button" class="text-xs font-medium text-brand-700 hover:text-brand-800" @click="checkConflict">
                  冲突检测
                </button>
              </div>

              <div class="mt-3">
                <label class="block text-xs font-medium text-slate-600">预约日期</label>
                <div class="mt-1 grid grid-cols-2 gap-3">
                  <input
                    v-model="selectedDate"
                    type="date"
                    class="input"
                    :min="localDateString(new Date())"
                  />
                  <input
                    v-model="selectedEndDate"
                    type="date"
                    class="input"
                    :min="selectedDate || localDateString(new Date())"
                  />
                </div>
              </div>

              <div class="mt-3">
                <label class="block text-xs font-medium text-slate-600">时段模板</label>
                <select v-model="selectedSlotPreset" class="input mt-1" @change="choosePreset">
                  <option value="">自定义时段</option>
                  <option v-for="p in slotPresets" :key="p.label" :value="p.label">
                    {{ p.label }}
                  </option>
                </select>
              </div>

              <div class="mt-3 grid grid-cols-2 gap-3">
                <div>
                  <label class="block text-xs font-medium text-slate-600">开始时间</label>
                  <select v-model="selectedStartTime" class="input mt-1">
                    <option value="">选择时间</option>
                    <option v-for="time in timeSlots" :key="time" :value="time">
                      {{ time }}
                    </option>
                  </select>
                </div>
                <div>
                  <label class="block text-xs font-medium text-slate-600">结束时间</label>
                  <select v-model="selectedEndTime" class="input mt-1">
                    <option value="">选择时间</option>
                    <option v-for="time in endTimeSlots" :key="time" :value="time">
                      {{ time }}
                    </option>
                  </select>
                </div>
              </div>

              <div class="mt-3 rounded-lg border border-slate-200 bg-slate-50 p-3 text-xs text-slate-600">
                提示：建议先进行一次冲突检测再提交；时段会随预约一起存储并在记录中展示
              </div>
            </div>

            <!-- 使用信息 -->
            <div class="rounded-xl border border-slate-200 bg-white p-4 lg:col-span-2">
              <h4 class="text-sm font-semibold text-slate-900">使用信息</h4>
              <div class="mt-3">
                <label class="block text-xs font-medium text-slate-600">使用目的</label>
                <textarea
                  v-model="form.purpose"
                  class="input mt-1 min-h-[96px] resize-none"
                  placeholder="例如：课程实验 / 课题研究 / 项目开发..."
                ></textarea>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部操作按钮（仅两个） -->
        <div class="flex-shrink-0 border-t border-slate-200 bg-white/80 px-6 py-4 backdrop-blur">
          <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div class="text-xs text-slate-500">提交前请确认资源与时间信息无误</div>
            <div class="flex gap-3">
              <button @click="cancelBooking" class="btn-secondary">
                取消预约
              </button>
              <button @click="submitBooking" class="btn-primary">
                提交预约
              </button>
            </div>
          </div>
        </div>
      </div>
      </div>
    </div>
  </div>
</template>
