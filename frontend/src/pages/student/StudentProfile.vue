<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

// 表单数据
const profile = ref({
  realName: '',
  nickname: '',
  email: '',
  phone: '',
  avatarUrl: '',
  studentId: '',
  major: '',
  grade: ''
});

// 密码修改
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// 头像预览
const avatarPreview = ref('');
const avatarFile = ref(null);

// 加载状态
const saving = ref(false);
const changingPassword = ref(false);

// 标签页
const activeTab = ref('profile');

onMounted(() => {
  loadUserProfile();
});

function loadUserProfile() {
  // 从localStorage加载用户信息
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
    profile.value = {
      realName: userInfo.realName || '张三',
      nickname: userInfo.nickname || '小张',
      email: userInfo.email || 'student@example.com',
      phone: userInfo.phone || '138****5678',
      avatarUrl: userInfo.avatarUrl || '',
      studentId: userInfo.studentId || '2021001',
      major: userInfo.major || '计算机科学与技术',
      grade: userInfo.grade || '2021级'
    };
    avatarPreview.value = profile.value.avatarUrl;
  } catch (e) {
    console.error('加载用户信息失败', e);
  }
}

// 头像上传
function handleAvatarChange(event) {
  const file = event.target.files[0];
  if (!file) return;
  
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件');
    return;
  }
  
  // 验证文件大小（最大2MB）
  if (file.size > 2 * 1024 * 1024) {
    alert('图片大小不能超过2MB');
    return;
  }
  
  avatarFile.value = file;
  
  // 预览
  const reader = new FileReader();
  reader.onload = (e) => {
    avatarPreview.value = e.target.result;
  };
  reader.readAsDataURL(file);
}

// 保存个人资料
async function saveProfile() {
  if (!profile.value.realName.trim()) {
    alert('请输入真实姓名');
    return;
  }
  
  if (!profile.value.email.trim()) {
    alert('请输入邮箱');
    return;
  }
  
  // 验证邮箱格式
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(profile.value.email)) {
    alert('邮箱格式不正确');
    return;
  }
  
  saving.value = true;
  
  try {
    // 模拟上传头像
    if (avatarFile.value) {
      // 这里应该调用API上传头像
      await new Promise(resolve => setTimeout(resolve, 1000));
      profile.value.avatarUrl = avatarPreview.value;
    }
    
    // 模拟保存个人资料
    await new Promise(resolve => setTimeout(resolve, 500));
    
    // 更新localStorage
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
    Object.assign(userInfo, profile.value);
    localStorage.setItem('userInfo', JSON.stringify(userInfo));
    
    alert('保存成功！');
    
    // 刷新页面以更新导航栏显示
    window.location.reload();
  } catch (error) {
    alert('保存失败，请重试');
  } finally {
    saving.value = false;
  }
}

// 修改密码
async function changePassword() {
  if (!passwordForm.value.oldPassword) {
    alert('请输入当前密码');
    return;
  }
  
  if (!passwordForm.value.newPassword) {
    alert('请输入新密码');
    return;
  }
  
  if (passwordForm.value.newPassword.length < 6) {
    alert('新密码长度不能少于6位');
    return;
  }
  
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    alert('两次输入的密码不一致');
    return;
  }
  
  changingPassword.value = true;
  
  try {
    // 模拟修改密码API调用
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    alert('密码修改成功！请重新登录');
    
    // 清除登录信息
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    
    // 跳转到登录页
    router.push('/auth/login');
  } catch (error) {
    alert('密码修改失败，请检查当前密码是否正确');
  } finally {
    changingPassword.value = false;
  }
}

// 重置密码表单
function resetPasswordForm() {
  passwordForm.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  };
}
</script>

<template>
  <div class="flex h-full overflow-hidden bg-slate-50">
    <!-- 左侧菜单 -->
    <aside class="flex h-full w-56 flex-shrink-0 flex-col border-r border-slate-200 bg-white">
      <div class="flex-shrink-0 px-6 py-6">
        <h2 class="text-base font-semibold text-slate-900">个人设置</h2>
      </div>
      <nav class="flex-1 space-y-1 px-3">
        <button
          @click="activeTab = 'profile'"
          class="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-sm font-medium transition"
          :class="activeTab === 'profile' 
            ? 'bg-brand-50 text-brand-700' 
            : 'text-slate-700 hover:bg-slate-50'"
        >
          <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
          </svg>
          个人资料
        </button>
        <button
          @click="activeTab = 'security'"
          class="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-sm font-medium transition"
          :class="activeTab === 'security' 
            ? 'bg-brand-50 text-brand-700' 
            : 'text-slate-700 hover:bg-slate-50'"
        >
          <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
          </svg>
          账户安全
        </button>
      </nav>
    </aside>

    <!-- 右侧内容区域 -->
    <div class="flex h-full min-w-0 flex-1 flex-col overflow-hidden bg-white">
      <!-- 顶部标题 -->
      <div class="flex-shrink-0 border-b border-slate-200 bg-white px-8 py-6">
        <div class="mx-auto max-w-5xl">
          <h2 class="text-xl font-semibold text-slate-900">
            {{ activeTab === 'profile' ? '个人资料' : '账户安全' }}
          </h2>
          <p class="mt-1 text-sm text-slate-600">
            {{ activeTab === 'profile' ? '管理您的个人信息和联系方式' : '修改密码，保护账户安全' }}
          </p>
        </div>
      </div>

      <!-- 内容区域 - 独立滚动 -->
      <div class="flex-1 overflow-y-auto bg-slate-50">
        <div class="px-8 py-6">
          <!-- 个人资料 -->
          <div v-show="activeTab === 'profile'" class="mx-auto max-w-3xl">
            <div class="rounded-xl border border-slate-200 bg-white shadow-sm">
              <!-- 头像上传 -->
              <div class="border-b border-slate-100 px-6 py-5">
                <h3 class="text-sm font-semibold text-slate-900">头像设置</h3>
                <div class="mt-4 flex items-center gap-6">
                  <div class="relative h-20 w-20 flex-shrink-0">
                    <div class="h-full w-full overflow-hidden rounded-full border-2 border-slate-200 bg-slate-100">
                      <img
                        v-if="avatarPreview"
                        :src="avatarPreview"
                        alt="头像"
                        class="h-full w-full object-cover"
                      />
                      <div v-else class="flex h-full w-full items-center justify-center text-2xl font-bold text-slate-400">
                        {{ profile.realName.charAt(0) }}
                      </div>
                    </div>
                  </div>
                  <div class="flex-1">
                    <label class="btn-secondary cursor-pointer text-sm">
                      <input
                        type="file"
                        accept="image/*"
                        class="hidden"
                        @change="handleAvatarChange"
                      />
                      选择图片
                    </label>
                    <p class="mt-2 text-xs text-slate-500">
                      支持 JPG、PNG 格式，文件大小不超过 2MB
                    </p>
                  </div>
                </div>
              </div>

              <!-- 基本信息 -->
              <div class="border-b border-slate-100 px-6 py-5">
                <h3 class="text-sm font-semibold text-slate-900">基本信息</h3>
                <div class="mt-4 grid gap-4 md:grid-cols-2">
                  <div>
                    <label class="block text-xs font-medium text-slate-600">真实姓名</label>
                    <input
                      v-model="profile.realName"
                      type="text"
                      class="input mt-1.5"
                      placeholder="请输入真实姓名"
                    />
                  </div>
                  <div>
                    <label class="block text-xs font-medium text-slate-600">昵称</label>
                    <input
                      v-model="profile.nickname"
                      type="text"
                      class="input mt-1.5"
                      placeholder="请输入昵称"
                    />
                  </div>
                  <div>
                    <label class="block text-xs font-medium text-slate-600">学号</label>
                    <input
                      v-model="profile.studentId"
                      type="text"
                      class="input mt-1.5 bg-slate-50"
                      placeholder="学号"
                      disabled
                    />
                  </div>
                  <div>
                    <label class="block text-xs font-medium text-slate-600">年级</label>
                    <input
                      v-model="profile.grade"
                      type="text"
                      class="input mt-1.5 bg-slate-50"
                      placeholder="年级"
                      disabled
                    />
                  </div>
                  <div class="md:col-span-2">
                    <label class="block text-xs font-medium text-slate-600">专业</label>
                    <input
                      v-model="profile.major"
                      type="text"
                      class="input mt-1.5 bg-slate-50"
                      placeholder="专业"
                      disabled
                    />
                  </div>
                </div>
              </div>

              <!-- 联系方式 -->
              <div class="px-6 py-5">
                <h3 class="text-sm font-semibold text-slate-900">联系方式</h3>
                <div class="mt-4 grid gap-4 md:grid-cols-2">
                  <div>
                    <label class="block text-xs font-medium text-slate-600">邮箱地址</label>
                    <input
                      v-model="profile.email"
                      type="email"
                      class="input mt-1.5"
                      placeholder="请输入邮箱地址"
                    />
                  </div>
                  <div>
                    <label class="block text-xs font-medium text-slate-600">手机号码</label>
                    <input
                      v-model="profile.phone"
                      type="tel"
                      class="input mt-1.5"
                      placeholder="请输入手机号码"
                    />
                  </div>
                </div>
              </div>

              <!-- 保存按钮 -->
              <div class="border-t border-slate-100 px-6 py-4">
                <button
                  @click="saveProfile"
                  class="btn-primary"
                  :disabled="saving"
                  :class="saving ? 'opacity-50 cursor-not-allowed' : ''"
                >
                  {{ saving ? '保存中...' : '保存资料' }}
                </button>
              </div>
            </div>
          </div>

          <!-- 账户安全 -->
          <div v-show="activeTab === 'security'" class="mx-auto max-w-2xl">
            <div class="rounded-xl border border-slate-200 bg-white shadow-sm">
              <div class="px-6 py-5">
                <h3 class="mb-6 text-sm font-semibold text-slate-900">修改密码</h3>
                <p class="mt-1 text-xs text-slate-600">为了您的账户安全，建议定期修改密码</p>
                
                <div class="mt-6 space-y-4">
                  <div>
                    <label class="block text-xs font-medium text-slate-600">当前密码</label>
                    <input
                      v-model="passwordForm.oldPassword"
                      type="password"
                      class="input mt-1.5"
                      placeholder="请输入当前密码"
                    />
                  </div>
                  <div>
                    <label class="block text-xs font-medium text-slate-600">新密码</label>
                    <input
                      v-model="passwordForm.newPassword"
                      type="password"
                      class="input mt-1.5"
                      placeholder="请输入新密码（至少6位）"
                    />
                  </div>
                  <div>
                    <label class="block text-xs font-medium text-slate-600">确认新密码</label>
                    <input
                      v-model="passwordForm.confirmPassword"
                      type="password"
                      class="input mt-1.5"
                      placeholder="请再次输入新密码"
                    />
                  </div>
                </div>
              </div>

              <div class="border-t border-slate-100 px-6 py-4">
                <div class="flex gap-3">
                  <button
                    @click="changePassword"
                    class="btn-primary"
                    :disabled="changingPassword"
                    :class="changingPassword ? 'opacity-50 cursor-not-allowed' : ''"
                  >
                    {{ changingPassword ? '修改中...' : '确认修改' }}
                  </button>
                  <button
                    @click="resetPasswordForm"
                    class="btn-secondary"
                    :disabled="changingPassword"
                  >
                    重置
                  </button>
                </div>
              </div>
            </div>

            <!-- 安全提示 -->
            <div class="mt-4 rounded-xl border border-amber-200 bg-amber-50 p-4">
              <div class="flex gap-3">
                <svg class="h-5 w-5 flex-shrink-0 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
                <div class="flex-1">
                  <h4 class="text-sm font-medium text-amber-900">安全提示</h4>
                  <ul class="mt-2 space-y-1 text-xs text-amber-800">
                    <li>• 密码长度至少6位，建议包含字母、数字和特殊字符</li>
                    <li>• 不要使用过于简单的密码，如生日、手机号等</li>
                    <li>• 定期修改密码可以提高账户安全性</li>
                    <li>• 修改密码后需要重新登录</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
