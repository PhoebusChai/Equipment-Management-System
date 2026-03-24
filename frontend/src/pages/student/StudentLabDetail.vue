<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getLabApi } from "../../services/resources";
import { APPLICATION_STATUS, listApplicationsApi } from "../../services/applications";
import { listReviewsByResourceApi } from "../../services/reviews";

const route = useRoute();
const router = useRouter();

const labId = computed(() => Number(route.params.id));
const lab = ref(null);
const isLabOpened = ref(false);
const reviews = ref([]);

function parseLabIdFromDetail(detail) {
  const txt = String(detail || "");
  const rows = txt.split("\n");
  const line = rows.find((r) => r.startsWith("labId：") || r.startsWith("labId:"));
  if (!line) return null;
  const val = line.split(/：|:/).slice(1).join(":").trim();
  const id = Number(val);
  return Number.isFinite(id) ? id : null;
}

const displayStatus = computed(() => {
  if (!lab.value) return "unknown";
  if (lab.value.status === "maintenance") return "maintenance";
  if (!isLabOpened.value) return "unopened";
  return "opened";
});

function statusClass(status) {
  if (status === "opened") return "bg-emerald-100 text-emerald-700 ring-1 ring-emerald-600/20";
  if (status === "unopened") return "bg-slate-100 text-slate-600 ring-1 ring-slate-600/20";
  if (status === "maintenance") return "bg-rose-100 text-rose-700 ring-1 ring-rose-600/20";
  return "bg-slate-100 text-slate-600 ring-1 ring-slate-600/20";
}

function statusText(status) {
  if (status === "opened") return "开放中";
  if (status === "unopened") return "暂未开放";
  if (status === "maintenance") return "维修中";
  return status;
}

const labImages = computed(() => {
  if (!lab.value) return [];
  const urls = Array.isArray(lab.value.imageUrls) ? lab.value.imageUrls : [];
  if (urls.length) return urls;
  return [lab.value.image].filter(Boolean);
});
const avgRating = computed(() => {
  if (!reviews.value.length) return 0;
  const sum = reviews.value.reduce((s, x) => s + Number(x.rating || 0), 0);
  return Math.round((sum / reviews.value.length) * 10) / 10;
});

const currentImageIndex = ref(0);

function nextImage() {
  currentImageIndex.value = (currentImageIndex.value + 1) % labImages.value.length;
}

function prevImage() {
  currentImageIndex.value = (currentImageIndex.value - 1 + labImages.value.length) % labImages.value.length;
}

function goToImage(index) {
  currentImageIndex.value = index;
}

function goToBooking() {
  router.push({
    path: '/student/booking-records/new',
    query: { 
      labId: labId.value, 
      labName: lab.value.name,
      type: 'lab'
    }
  });
}

function goToBookingRecords() {
  router.push('/student/booking-records');
}

onMounted(async () => {
  try {
    const [labRes, apps] = await Promise.all([getLabApi(labId.value), listApplicationsApi()]);
    lab.value = labRes;
    reviews.value = await listReviewsByResourceApi("LAB", labId.value);
    const approvedIds = new Set(
      (apps || [])
        .filter((x) => x.type === "lab_apply" && x.status === APPLICATION_STATUS.APPROVED)
        .map((x) => parseLabIdFromDetail(x.detail))
        .filter(Boolean)
    );
    isLabOpened.value = approvedIds.has(labId.value);
  } catch {
    lab.value = null;
    reviews.value = [];
  }
});
</script>

<template>
  <div v-if="lab" class="flex h-full flex-col bg-slate-50">
    <!-- 顶部标题栏 -->
    <div class="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-4 shadow-sm">
      <div>
        <h2 class="text-xl font-semibold text-slate-900">{{ lab.name }}</h2>
        <p class="mt-0.5 text-sm text-slate-600">{{ lab.building }} · {{ lab.college }}</p>
      </div>
      <div class="flex items-center gap-3">
        <span class="rounded-full px-3 py-1 text-xs font-medium" :class="statusClass(displayStatus)">
          {{ statusText(displayStatus) }}
        </span>
        <button class="btn-secondary" @click="router.push('/student/labs-devices')">
          返回列表
        </button>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="flex min-h-0 flex-1 gap-4 p-4">
      <!-- 左侧：图片轮播 -->
      <div class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <!-- 轮播图主体 -->
        <div class="relative flex-1 overflow-hidden bg-slate-100">
          <img
            :src="labImages[currentImageIndex]"
            :alt="lab.name"
            class="h-full w-full object-cover transition-opacity duration-300"
          />
          
          <!-- 左右切换按钮 -->
          <button
            v-if="labImages.length > 1"
            @click="prevImage"
            class="absolute left-4 top-1/2 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-full bg-white/90 text-slate-700 shadow-lg transition hover:bg-white"
          >
            <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          
          <button
            v-if="labImages.length > 1"
            @click="nextImage"
            class="absolute right-4 top-1/2 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-full bg-white/90 text-slate-700 shadow-lg transition hover:bg-white"
          >
            <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </button>

          <!-- 指示器 -->
          <div v-if="labImages.length > 1" class="absolute bottom-4 left-1/2 flex -translate-x-1/2 gap-2">
            <button
              v-for="(img, index) in labImages"
              :key="index"
              @click="goToImage(index)"
              class="h-2 w-2 rounded-full transition"
              :class="index === currentImageIndex ? 'bg-white w-6' : 'bg-white/50 hover:bg-white/75'"
            ></button>
          </div>
        </div>
      </div>

      <!-- 右侧：信息和评论 -->
      <div class="flex min-h-0 w-96 flex-shrink-0 flex-col gap-4">
        <!-- 实验室信息 -->
        <div class="flex flex-shrink-0 flex-col overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <div class="flex-shrink-0 border-b border-slate-100 px-4 py-2.5">
            <h3 class="text-sm font-semibold text-slate-900">实验室信息</h3>
          </div>
          
          <div class="px-4 py-3">
            <div class="grid grid-cols-2 gap-x-4 gap-y-2.5">
              <div>
                <p class="text-xs text-slate-500">实验室类型</p>
                <p class="mt-0.5 text-sm font-medium text-slate-900">
                  {{ lab.type === "COMPUTER" ? "计算机实验室" : "生物实验室" }}
                </p>
              </div>
              
              <div>
                <p class="text-xs text-slate-500">所属楼宇</p>
                <p class="mt-0.5 text-sm font-medium text-slate-900">{{ lab.building }}</p>
              </div>
              
              <div>
                <p class="text-xs text-slate-500">所属学院</p>
                <p class="mt-0.5 text-sm font-medium text-slate-900">{{ lab.college }}</p>
              </div>
              
              <div>
                <p class="text-xs text-slate-500">负责教师</p>
                <p class="mt-0.5 text-sm font-medium text-slate-900">{{ lab.manager }}</p>
              </div>
              
              <div>
                <p class="text-xs text-slate-500">容量</p>
                <p class="mt-0.5 text-sm font-medium text-slate-900">{{ lab.capacity }} 人</p>
              </div>
              
              <div>
                <p class="text-xs text-slate-500">开放时间</p>
                <p class="mt-0.5 text-sm font-medium text-slate-900">{{ lab.opening }}</p>
              </div>
              
              <div class="col-span-2">
                <p class="text-xs text-slate-500">使用说明</p>
                <p class="mt-0.5 text-sm leading-relaxed text-slate-700">{{ lab.description }}</p>
              </div>
            </div>
          </div>

          <div class="flex-shrink-0 border-t border-slate-100 px-4 py-2.5">
            <div class="flex gap-2">
              <button
                class="btn-primary flex-1"
                :disabled="displayStatus !== 'opened'"
                :class="displayStatus !== 'opened' ? 'opacity-50 cursor-not-allowed' : ''"
                @click="goToBooking"
              >
                立即预约
              </button>
              <button class="btn-secondary flex-1" @click="goToBookingRecords">预约记录</button>
            </div>
          </div>
        </div>

        <!-- 评论区 -->
        <div class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <div class="flex-shrink-0 px-5 py-3">
            <div class="flex items-center justify-between gap-2">
              <h3 class="text-base font-semibold text-slate-900">用户评价</h3>
              <div class="text-xs text-slate-500">平均分：{{ avgRating || "-" }}（{{ reviews.length }}条）</div>
            </div>
          </div>
          <div v-if="reviews.length === 0" class="flex min-h-0 flex-1 items-center justify-center border-t border-slate-100 px-5 py-6">
            <div class="text-center text-sm text-slate-500">暂无评价数据</div>
          </div>
          <div v-else class="min-h-0 flex-1 overflow-auto border-t border-slate-100 px-5 py-3">
            <div class="space-y-3">
              <div v-for="item in reviews" :key="item.id" class="rounded-lg bg-slate-50 p-3">
                <div class="flex items-center justify-between">
                  <div class="text-sm font-medium text-slate-900">{{ item.userName || `用户#${item.userId}` }}</div>
                  <div class="text-amber-500">
                    <span v-for="star in 5" :key="star" class="text-xs" :class="star <= item.rating ? 'text-amber-400' : 'text-slate-300'">★</span>
                  </div>
                </div>
                <p class="mt-1 text-sm text-slate-700">{{ item.content || "（未填写评价内容）" }}</p>
                <p class="mt-1 text-xs text-slate-500">{{ item.createdAt?.replace("T", " ") || "-" }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 未找到信息 -->
  <div v-else class="flex h-full items-center justify-center bg-slate-50">
    <p class="text-slate-400">未找到该实验室信息</p>
  </div>
</template>
