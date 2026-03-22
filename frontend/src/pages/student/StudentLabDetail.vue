<script setup>
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { labs, statusClass, statusText } from "../../mock/labs";

const route = useRoute();
const router = useRouter();

const labId = computed(() => Number(route.params.id));
const lab = computed(() => labs.find((item) => item.id === labId.value) || null);

// 模拟多张实验室图片
const labImages = computed(() => {
  if (!lab.value) return [];
  // 为每个实验室生成多张图片
  return [
    lab.value.image,
    lab.value.image.replace('?w=400', '?w=400&sat=-100'), // 黑白效果
    lab.value.image.replace('?w=400', '?w=400&hue=30'),   // 色调变化
  ];
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

// 模拟评论数据
const comments = ref([
  {
    id: 1,
    userName: "张同学",
    rating: 5,
    content: "设备很新，环境很好，老师也很负责任。",
    time: "2024-03-05 14:30"
  },
  {
    id: 2,
    userName: "李同学",
    rating: 4,
    content: "实验室位置方便，设备齐全，就是有时候人比较多。",
    time: "2024-03-04 10:15"
  },
  {
    id: 3,
    userName: "王同学",
    rating: 5,
    content: "非常适合做项目，网络速度快，空调温度适宜。",
    time: "2024-03-03 16:45"
  }
]);

const newComment = ref("");
const newRating = ref(5);

function submitComment() {
  if (!newComment.value.trim()) return;
  
  comments.value.unshift({
    id: Date.now(),
    userName: "当前用户",
    rating: newRating.value,
    content: newComment.value,
    time: new Date().toLocaleString('zh-CN', { 
      year: 'numeric', 
      month: '2-digit', 
      day: '2-digit', 
      hour: '2-digit', 
      minute: '2-digit' 
    })
  });
  
  newComment.value = "";
  newRating.value = 5;
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
        <span class="rounded-full px-3 py-1 text-xs font-medium" :class="statusClass(lab.status)">
          {{ statusText(lab.status) }}
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
                :disabled="lab.status !== 'AVAILABLE'"
                :class="lab.status !== 'AVAILABLE' ? 'opacity-50 cursor-not-allowed' : ''"
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
            <h3 class="text-base font-semibold text-slate-900">用户评价</h3>
          </div>
          
          <!-- 发表评论 -->
          <div class="flex-shrink-0 border-t border-slate-100 px-5 py-3">
            <div class="flex items-center gap-2">
              <span class="text-xs text-slate-600">评分</span>
              <div class="flex gap-1">
                <button
                  v-for="star in 5"
                  :key="star"
                  @click="newRating = star"
                  class="text-base transition hover:scale-110"
                  :class="star <= newRating ? 'text-amber-400' : 'text-slate-300'"
                >
                  ★
                </button>
              </div>
            </div>
            
            <textarea
              v-model="newComment"
              class="input mt-2 min-h-[50px] resize-none text-sm"
              placeholder="分享你的使用体验..."
            ></textarea>
            
            <button
              @click="submitComment"
              class="btn-primary mt-2 w-full text-sm"
              :disabled="!newComment.trim()"
              :class="!newComment.trim() ? 'opacity-50 cursor-not-allowed' : ''"
            >
              发表评论
            </button>
          </div>

          <!-- 评论列表 -->
          <div class="min-h-0 flex-1 overflow-auto border-t border-slate-100 px-5 py-3">
            <div class="space-y-3">
              <div
                v-for="comment in comments"
                :key="comment.id"
                class="rounded-lg bg-slate-50 p-3"
              >
                <div class="flex items-start gap-2">
                  <div class="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-brand-100 text-sm font-semibold text-brand-700">
                    {{ comment.userName.charAt(0) }}
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center justify-between">
                      <span class="text-sm font-medium text-slate-900">{{ comment.userName }}</span>
                      <div class="flex gap-0.5">
                        <span
                          v-for="star in 5"
                          :key="star"
                          class="text-xs"
                          :class="star <= comment.rating ? 'text-amber-400' : 'text-slate-300'"
                        >
                          ★
                        </span>
                      </div>
                    </div>
                    <p class="mt-1 text-sm leading-relaxed text-slate-700">{{ comment.content }}</p>
                    <p class="mt-1 text-xs text-slate-500">{{ comment.time }}</p>
                  </div>
                </div>
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
