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
      <button
        v-for="item in menus"
        :key="item.to"
        class="flex w-full items-center gap-3 rounded-lg px-4 py-3 text-left text-sm font-medium transition"
        :class="currentPath === item.to ? 'bg-brand-50 text-brand-700 shadow-sm' : 'text-slate-700 hover:bg-slate-50'"
        @click="emit('navigate', item.to)"
      >
        <component :is="item.icon" :size="20" />
        {{ item.name }}
      </button>
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
