<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  LayoutDashboard,
  UserRound,
  CalendarClock,
  Wrench,
  Bell,
  Database,
  ShieldCheck
} from "lucide-vue-next";

const route = useRoute();
const router = useRouter();

const role = computed(() => route.meta.role || "student");

const menuMap = {
  student: [
    { name: "学生首页", icon: LayoutDashboard, to: "/student/dashboard" },
    { name: "用户中心", icon: UserRound, to: "/student/profile" },
    { name: "预约中心", icon: CalendarClock, to: "/student/booking" },
    { name: "消息通知", icon: Bell, to: "/student/messages" }
  ],
  teacher: [
    { name: "教师首页", icon: LayoutDashboard, to: "/teacher/dashboard" },
    { name: "实验室管理", icon: Database, to: "/teacher/labs" },
    { name: "预约监管", icon: CalendarClock, to: "/teacher/schedule" },
    { name: "报修工单", icon: Wrench, to: "/teacher/repairs" }
  ],
  admin: [
    { name: "管理员首页", icon: LayoutDashboard, to: "/admin/dashboard" },
    { name: "系统管理", icon: ShieldCheck, to: "/admin/system" },
    { name: "设备资产", icon: Database, to: "/admin/assets" },
    { name: "数据分析", icon: CalendarClock, to: "/admin/reports" }
  ]
};

const menus = computed(() => menuMap[role.value]);

const roleLabel = computed(() => {
  if (role.value === "teacher") return "教师端";
  if (role.value === "admin") return "管理员端";
  return "学生端";
});

function isActive(path) {
  return route.path === path;
}
</script>

<template>
  <div class="min-h-screen bg-app">
    <header class="sticky top-0 z-20 border-b border-slate-200 bg-white/95 backdrop-blur">
      <div class="mx-auto flex h-16 max-w-7xl items-center justify-between px-4">
        <div class="flex items-center gap-3">
          <div class="h-9 w-9 rounded-lg bg-brand-600" />
          <div>
            <div class="text-sm text-slate-500">Equipment Management</div>
            <div class="text-base font-semibold text-slate-900">{{ roleLabel }}</div>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <button class="btn-secondary text-xs" @click="router.push('/student/dashboard')">学生端</button>
          <button class="btn-secondary text-xs" @click="router.push('/teacher/dashboard')">教师端</button>
          <button class="btn-secondary text-xs" @click="router.push('/admin/dashboard')">管理员端</button>
        </div>
      </div>
    </header>

    <main class="mx-auto grid max-w-7xl grid-cols-12 gap-6 px-4 py-6">
      <aside class="col-span-12 lg:col-span-3">
        <nav class="card p-3">
          <button
            v-for="item in menus"
            :key="item.to"
            class="mb-1 flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left text-sm transition"
            :class="isActive(item.to) ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-50'"
            @click="router.push(item.to)"
          >
            <component :is="item.icon" :size="18" />
            {{ item.name }}
          </button>
        </nav>
      </aside>

      <section class="col-span-12 lg:col-span-9">
        <router-view />
      </section>
    </main>
  </div>
</template>
