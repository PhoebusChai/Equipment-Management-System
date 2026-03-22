<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  LayoutDashboard,
  UserRound,
  CalendarClock,
  Wrench,
  Database,
  ShieldCheck
} from "lucide-vue-next";
import TopNavBar from "../components/TopNavBar.vue";
import SideMenu from "../components/SideMenu.vue";
import { findUserByEmail, getCurrentUser } from "../mock/db";

const route = useRoute();
const router = useRouter();

const role = computed(() => route.meta.role || "student");

const menuMap = {
  student: [
    { name: "仪表盘", icon: LayoutDashboard, to: "/student/dashboard" },
    { name: "实验室与设备", icon: Database, to: "/student/labs-devices" },
    { name: "我的预约", icon: CalendarClock, to: "/student/booking-records" },
    { name: "个人设置", icon: UserRound, to: "/student/settings" }
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

const menus = computed(() => menuMap[role.value] || menuMap.student);

const roleLabel = computed(() => {
  if (role.value === "teacher") return "教师端";
  if (role.value === "admin") return "管理员端";
  return "学生端";
});

const userInfo = computed(() => {
  const current = getCurrentUser();
  const dbUser = current?.email ? findUserByEmail(current.email) : null;

  const defaultNameMap = {
    student: "学生用户",
    teacher: "教师用户",
    admin: "管理员"
  };

  return {
    name: dbUser?.realName || defaultNameMap[role.value] || "用户",
    avatarUrl: ""
  };
});

function navigate(path) {
  router.push(path);
}

function logout() {
  localStorage.removeItem("ems_current_user");
  router.replace("/auth/login");
}
</script>

<template>
  <div class="flex h-screen flex-col overflow-hidden bg-slate-50">
    <TopNavBar :role-label="roleLabel" :user-name="userInfo.name" :user-avatar="userInfo.avatarUrl" />

    <main class="flex flex-1 overflow-hidden">
      <aside class="w-56 border-r border-slate-200 bg-white">
        <SideMenu :menus="menus" :current-path="route.path" @navigate="navigate" @logout="logout" />
      </aside>

      <section class="flex-1 overflow-auto">
        <router-view />
      </section>
    </main>
  </div>
</template>
