<script setup>
import { LogOut } from "lucide-vue-next";

defineProps({
  menus: { type: Array, required: true },
  currentPath: { type: String, required: true }
});

const emit = defineEmits(["navigate", "logout"]);
</script>

<template>
  <nav class="flex h-full flex-col p-3">
    <div class="space-y-1">
      <template v-for="item in menus" :key="item.to || item.name">
        <div v-if="item.children?.length" class="space-y-1">
          <div class="flex items-center gap-3 rounded-lg px-4 py-2 text-sm font-semibold text-slate-800">
            <component :is="item.icon" :size="18" />
            {{ item.name }}
          </div>
          <button
            v-for="sub in item.children"
            :key="sub.to"
            class="ml-6 flex w-[calc(100%-1.5rem)] items-center gap-2 rounded-lg px-3 py-2 text-left text-sm font-medium transition"
            :class="currentPath === sub.to ? 'bg-brand-50 text-brand-700 shadow-sm' : 'text-slate-700 hover:bg-slate-50'"
            @click="emit('navigate', sub.to)"
          >
            <span class="h-1.5 w-1.5 rounded-full bg-current opacity-70"></span>
            {{ sub.name }}
          </button>
        </div>

        <button
          v-else
          class="flex w-full items-center gap-3 rounded-lg px-4 py-3 text-left text-sm font-medium transition"
          :class="currentPath === item.to ? 'bg-brand-50 text-brand-700 shadow-sm' : 'text-slate-700 hover:bg-slate-50'"
          @click="emit('navigate', item.to)"
        >
          <component :is="item.icon" :size="20" />
          {{ item.name }}
        </button>
      </template>
    </div>

    <button
      class="mt-auto flex w-full items-center gap-3 rounded-lg px-4 py-3 text-left text-sm font-medium text-red-600 transition hover:bg-red-50"
      @click="emit('logout')"
    >
      <LogOut :size="20" />
      退出登录
    </button>
  </nav>
</template>
