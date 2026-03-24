import { createRouter, createWebHistory } from "vue-router";
import MainLayout from "../layouts/MainLayout.vue";
import AuthLayout from "../layouts/AuthLayout.vue";

import LoginPage from "../pages/auth/LoginPage.vue";
import RegisterPage from "../pages/auth/RegisterPage.vue";
import ForgotPasswordPage from "../pages/auth/ForgotPasswordPage.vue";

import StudentDashboard from "../pages/student/StudentDashboard.vue";
import StudentLabsDevices from "../pages/student/StudentLabsDevices.vue";
import StudentLabDetail from "../pages/student/StudentLabDetail.vue";
import StudentProfile from "../pages/student/StudentProfile.vue";
import StudentBooking from "../pages/student/StudentBooking.vue";
import StudentBookingRecords from "../pages/student/StudentBookingRecords.vue";
import StudentBookingDetail from "../pages/student/StudentBookingDetail.vue";
import StudentMessages from "../pages/student/StudentMessages.vue";

import TeacherDashboard from "../pages/teacher/TeacherDashboard.vue";
import TeacherLabs from "../pages/teacher/TeacherLabs.vue";
import TeacherDevices from "../pages/teacher/TeacherDevices.vue";
import TeacherSchedule from "../pages/teacher/TeacherSchedule.vue";
import TeacherRepairs from "../pages/teacher/TeacherRepairs.vue";

import AdminDashboard from "../pages/admin/AdminDashboard.vue";
import AdminUsers from "../pages/admin/AdminUsers.vue";
import AdminAssets from "../pages/admin/AdminAssets.vue";
import AdminReports from "../pages/admin/AdminReports.vue";
import AdminLabInfo from "../pages/admin/AdminLabInfo.vue";
import AdminLabApplications from "../pages/admin/AdminLabApplications.vue";
import AdminDeviceInfo from "../pages/admin/AdminDeviceInfo.vue";
import AdminReviews from "../pages/admin/AdminReviews.vue";
import { getCurrentUser } from "../services/session";

const routes = [
  {
    path: "/",
    redirect: "/auth/login"
  },
  {
    path: "/auth",
    component: AuthLayout,
    children: [
      { path: "login", component: LoginPage },
      { path: "register", component: RegisterPage },
      { path: "forgot-password", component: ForgotPasswordPage }
    ]
  },
  {
    path: "/student",
    component: MainLayout,
    meta: { role: "student" },
    children: [
      { path: "dashboard", component: StudentDashboard, meta: { role: "student" } },
      { path: "labs-devices", component: StudentLabsDevices, meta: { role: "student" } },
      { path: "labs-devices/:id", component: StudentLabDetail, meta: { role: "student" } },
      { 
        path: "booking-records", 
        component: StudentBookingRecords, 
        meta: { role: "student" },
        children: [
          { path: "new", component: StudentBooking, meta: { role: "student" } },
          { path: ":id", component: StudentBookingDetail, meta: { role: "student" } }
        ]
      },
      // 兼容旧路由，重定向到新路由
      { path: "booking", redirect: "/student/booking-records/new" },
      { path: "settings", component: StudentProfile, meta: { role: "student" } },
      { path: "profile", redirect: "/student/settings" },
      { path: "messages", component: StudentMessages, meta: { role: "student" } }
    ]
  },
  {
    path: "/teacher",
    component: MainLayout,
    meta: { role: "teacher" },
    children: [
      { path: "dashboard", component: TeacherDashboard, meta: { role: "teacher" } },
      { path: "labs", redirect: "/teacher/labs/info" },
      { path: "labs/info", component: TeacherLabs, meta: { role: "teacher" } },
      { path: "labs/applications", component: TeacherLabs, meta: { role: "teacher" } },
      { path: "labs/student-applications", component: TeacherSchedule, meta: { role: "teacher" } },
      { path: "devices", component: TeacherDevices, meta: { role: "teacher" } },
      { path: "schedule", redirect: "/teacher/labs/student-applications" },
      { path: "repairs", component: TeacherRepairs, meta: { role: "teacher" } }
    ]
  },
  {
    path: "/admin",
    component: MainLayout,
    meta: { role: "admin" },
    children: [
      { path: "dashboard", component: AdminDashboard, meta: { role: "admin" } },
      { path: "users", component: AdminUsers, meta: { role: "admin" } },
      { path: "devices/info", component: AdminDeviceInfo, meta: { role: "admin" } },
      { path: "devices/applications", component: AdminAssets, meta: { role: "admin" } },
      { path: "reviews", component: AdminReviews, meta: { role: "admin" } },
      { path: "assets", redirect: "/admin/devices/applications" },
      { path: "system", redirect: "/admin/dashboard" },
      { path: "reports", component: AdminReports, meta: { role: "admin" } },
      { path: "labs/info", component: AdminLabInfo, meta: { role: "admin" } },
      { path: "labs/applications", component: AdminLabApplications, meta: { role: "admin" } }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  // 认证页不拦截
  if (to.path.startsWith("/auth")) return true;

  const requiredRole = to.meta?.role;
  if (!requiredRole) return true;

  const current = getCurrentUser();
  if (!current) return { path: "/auth/login" };

  if (current.role !== requiredRole) {
    const redirectMap = {
      student: "/student/dashboard",
      teacher: "/teacher/dashboard",
      admin: "/admin/dashboard"
    };
    return { path: redirectMap[current.role] || "/auth/login" };
  }

  return true;
});

export default router;
