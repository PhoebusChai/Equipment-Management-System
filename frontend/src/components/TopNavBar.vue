<script setup>
import { computed, ref, watch } from "vue";
import { Bell, MonitorCog } from "lucide-vue-next";
import AppPagination from "./AppPagination.vue";

const props = defineProps({
  roleLabel: { type: String, required: true },
  userName: { type: String, required: true },
  userAvatar: { type: String, default: "" }
});

const avatarLoadFailed = ref(false);

watch(
  () => props.userAvatar,
  () => {
    avatarLoadFailed.value = false;
  }
);

const avatarText = computed(() => {
  const name = (props.userName || "").trim();
  return name ? name.charAt(0).toUpperCase() : "U";
});

const showAvatarImage = computed(() => !!props.userAvatar && !avatarLoadFailed.value);
const messageDialogVisible = ref(false);

const noticeList = ref([
  { id: 1, title: "计算机实验室A301维护通知", time: "今天 10:20" },
  { id: 2, title: "生物实验室本周消杀安排", time: "昨天 16:40" }
]);

const messageList = ref([
  { id: 1, title: "你的预约已审核通过", time: "今天 09:15" },
  { id: 2, title: "设备报修状态已更新", time: "昨天 18:02" }
]);

const unreadCount = computed(() => noticeList.value.length + messageList.value.length);
const mixedNoticeList = computed(() => [
  ...noticeList.value.map((item) => ({ ...item, type: "系统公告" })),
  ...messageList.value.map((item) => ({ ...item, type: "我的消息" }))
]);
const currentPage = ref(1);
const pageSize = ref(5);
const pagedNoticeList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return mixedNoticeList.value.slice(start, start + pageSize.value);
});
const filledNoticeList = computed(() => {
  const items = [...pagedNoticeList.value];
  const missing = pageSize.value - items.length;
  for (let i = 0; i < missing; i += 1) {
    items.push({ id: `blank-${i}`, isPlaceholder: true });
  }
  return items;
});

watch([mixedNoticeList, pageSize], () => {
  const totalPages = Math.max(1, Math.ceil(mixedNoticeList.value.length / pageSize.value));
  if (currentPage.value > totalPages) currentPage.value = totalPages;
});

function handleAvatarError() {
  avatarLoadFailed.value = true;
}

function openMessageDialog() {
  messageDialogVisible.value = true;
}
</script>

<template>
  <header class="z-20 flex h-16 items-center justify-between border-b border-slate-200 bg-white px-6 shadow-sm">
    <div class="flex items-center gap-3">
      <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-brand-600 text-white shadow-md">
        <MonitorCog :size="20" />
      </div>
      <div>
        <div class="text-base font-semibold text-slate-900">实验室设备管理系统</div>
      </div>
    </div>

    <div class="flex items-center gap-3">
      <button
        class="relative flex h-10 w-10 items-center justify-center rounded-full text-slate-600 transition hover:bg-slate-100"
        @click="openMessageDialog"
      >
        <Bell :size="20" />
        <span
          v-if="unreadCount > 0"
          class="absolute right-0 top-0 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-[10px] font-medium text-white shadow"
        >
          {{ unreadCount > 99 ? "99+" : unreadCount }}
        </span>
      </button>

      <div class="flex h-10 w-10 items-center justify-center overflow-hidden rounded-full bg-brand-100 text-sm font-semibold text-brand-700">
        <img
          v-if="showAvatarImage"
          :src="userAvatar"
          alt="avatar"
          class="h-full w-full object-cover"
          @error="handleAvatarError"
        />
        <span v-else>{{ avatarText }}</span>
      </div>
      <div class="text-left">
        <div class="text-sm font-medium text-slate-900">{{ userName }}</div>
        <div class="text-xs text-slate-500">{{ roleLabel }}</div>
      </div>
    </div>
  </header>

  <div
    v-if="messageDialogVisible"
    class="fixed inset-0 z-40 flex items-center justify-center bg-slate-900/30 px-4"
    @click.self="messageDialogVisible = false"
  >
    <div class="w-full max-w-2xl rounded-lg border border-slate-200 bg-white shadow-xl">
      <div class="flex items-center justify-between border-b border-slate-200 px-4 py-3">
        <h3 class="text-base font-semibold text-slate-800">公告与消息</h3>
        <button class="rounded-md px-2 py-1 text-sm text-slate-500 hover:bg-slate-100" @click="messageDialogVisible = false">
          关闭
        </button>
      </div>

      <div class="p-4">
        <div class="max-h-[360px] space-y-2 overflow-y-auto pr-1">
          <div
            v-for="(item, idx) in filledNoticeList"
            :key="`${item.type}-${item.id}-${idx}`"
            class="rounded-md px-3 py-2"
            :class="item.isPlaceholder ? 'border border-transparent' : 'border border-slate-200'"
          >
            <div v-if="!item.isPlaceholder" class="flex items-center justify-between gap-2">
              <span
                class="rounded px-2 py-0.5 text-xs"
                :class="item.type === '系统公告' ? 'bg-brand-50 text-brand-700' : 'bg-slate-100 text-slate-600'"
              >
                {{ item.type }}
              </span>
              <span class="text-xs text-slate-500">{{ item.time }}</span>
            </div>
            <p v-if="!item.isPlaceholder" class="mt-1 text-sm text-slate-800">{{ item.title }}</p>
            <div v-else class="h-[44px]"></div>
          </div>
        </div>

        <div class="mt-3">
          <AppPagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="mixedNoticeList.length"
            :page-size-options="[5]"
          />
        </div>
      </div>
    </div>
  </div>
</template>
