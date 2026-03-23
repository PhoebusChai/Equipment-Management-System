# Equipment Management System

高校实验室与设备预约管理系统（学生端 + 教师端 + 管理员端）。

## 1. 项目目标

本项目用于实现实验室与设备的统一预约、审核、运维与数据分析，覆盖三类角色：

- 学生：查询实验室/设备并发起预约、反馈与报修。
- 教师：管理实验室与设备、审核预约、处理报修与保养。
- 管理员：系统配置、全局审核、资产与报废管理、数据分析。

业务范围固定包含两类实验室：

- 计算机实验室
- 生物实验室

## 2. 技术栈

### 前端（Web）

- Vue 3（JavaScript）
- Tailwind CSS
- Lucide 图标库
- Axios（异步请求）

### 后端（API）

- Java
- Spring Boot
- Spring MVC / Spring Data JPA（或 MyBatis）

### 数据库

- MySQL 8.x

## 3. 预约功能三大范围

系统预约能力围绕以下三大范围构建：

1. 实验室预约  
   以实验室为资源单位，按时间段预约。
2. 设备预约  
   以设备为资源单位，按时间段预约。
3. 紧急预约  
   在满足规则的前提下，提供快速通道并触发审批/提醒。

## 4. 核心功能模块

详细功能请见：

- `docs/需求文档.md`
- `docs/数据库设计.md`

概览：

- 学生端：用户中心、实验室与设备查询、预约、消息、评价与报修
- 教师端：实验室管理、预约监管、使用监控、报修处理、公告发布、报废申请
- 管理员端：用户/实验室/设备全局管理、审核流、维护与报废、数据分析

## 5. 推荐目录结构

```text
EquipmentManagementSystem/
├─ README.md
├─ docs/
│  ├─ 需求文档.md
│  └─ 数据库设计.md
├─ frontend/                 # Vue3 + Tailwind + Axios + Lucide
└─ backend/                  # Spring Boot
```

## 6. 开发约定（建议）

- 时间粒度：15 分钟（预约开始/结束时间按 15 分钟对齐）。
- 状态机管理：预约、设备、报修、审批流程均使用可追踪状态流转。
- 冲突检测：同一资源在同一时间段只允许一个有效预约。
- 软删除优先：关键业务数据不做物理删除，保留审计字段。

## 7. API 设计建议（简要）

- 认证：`/api/auth/register`、`/api/auth/login`
- 实验室：`/api/labs`、`/api/labs/{id}`
- 设备：`/api/devices`、`/api/devices/{id}`
- 预约：`/api/bookings`、`/api/bookings/conflict-check`
- 报修：`/api/repairs`、`/api/repairs/{id}/confirm`
- 公告：`/api/notices`

## 8. 快速启动

### 8.1 前端启动（已实现基础骨架）

前端目录：`frontend/`

```bash
cd frontend
npm install
npm run dev
```

访问开发地址（默认）：`http://localhost:5173`

本地 Mock 登录账号（邮箱/密码）：

- 学生：`student@example.com` / `123456`
- 教师：`teacher@example.com` / `123456`
- 管理员：`admin@example.com` / `123456`

已完成内容：

- Vue3 + JavaScript + Vite 工程化配置
- TailwindCSS 蓝色主题（Google 风格：简洁卡片、圆角、浅阴影）
- Lucide 图标库接入
- Axios 请求封装（含拦截器）
- 学生端/教师端/管理员端页面骨架与路由

### 8.2 后端启动（待实现）

建议后续按以下顺序实施：

1. 初始化 Spring Boot 基础工程与鉴权模块。
2. 接入 MySQL 并执行 `docs/schema.sql`。
3. 优先实现预约链路（查询 -> 下单 -> 冲突检测 -> 审批 -> 结果）。
4. 完成报修与公告功能，最后补齐数据分析报表。

## 9. 相关文档

- 需求说明：`docs/需求文档.md`
- 数据库设计：`docs/数据库设计.md`
