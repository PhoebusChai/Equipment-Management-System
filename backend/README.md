# Backend (Spring Boot)

## 1. 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.x（可选，当前骨架可先不连库）

## 2. 启动

```bash
cd backend
mvn spring-boot:run
```

默认端口：`8080`

## 3. 第一批接口

- 健康检查：`GET /api/health`
- 认证登录：`POST /api/auth/login`
- 实验室列表：`GET /api/labs`
- 实验室详情：`GET /api/labs/{id}`
- 设备列表：`GET /api/devices?labId=1`（`labId` 可选）
- 设备详情：`GET /api/devices/{id}`
- 预约冲突检测：`POST /api/bookings/conflict-check`
- 创建预约：`POST /api/bookings`
- 预约列表：`GET /api/bookings?userId=1` 或 `GET /api/bookings?approverId=2`
- 审批通过：`PATCH /api/bookings/{id}/approve`
- 审批驳回：`PATCH /api/bookings/{id}/reject`
- 标记完成：`PATCH /api/bookings/{id}/finish`
- 用户列表：`GET /api/users`
- 新增用户：`POST /api/users`
- 用户状态更新：`PATCH /api/users/{id}/status`
- 重置密码：`PATCH /api/users/{id}/reset-password`

登录示例请求：

```json
{
  "email": "student@example.com",
  "password": "123456"
}
```

预约创建示例：

```json
{
  "userId": 1,
  "resourceType": "LAB",
  "resourceId": 1,
  "bookingScope": "LAB_BOOKING",
  "startTime": "2026-03-24T14:00:00",
  "endTime": "2026-03-24T16:00:00"
}
```

## 4. 配置项

可通过环境变量覆盖 `application.yml`：

- `SERVER_PORT`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

## 5. 第二批接口

- 报修列表：`GET /api/repairs?handlerId=2`（`handlerId` 可选）
- 创建报修：`POST /api/repairs`
- 报修确认：`PATCH /api/repairs/{id}/confirm`
- 开始维修：`PATCH /api/repairs/{id}/start`
- 维修完成：`PATCH /api/repairs/{id}/finish`
- 申请单聚合列表：`GET /api/applications`
- 新建实验室申请：`POST /api/applications/lab`
- 新建设备申请：`POST /api/applications/device`
- 设备报废申请：`POST /api/applications/scrap`
- 审批通过：`PATCH /api/applications/{type}/{id}/approve`（`type` 支持 `lab/device/scrap`）
- 审批驳回：`PATCH /api/applications/{type}/{id}/reject`
- 公告列表：`GET /api/notices`
- 创建公告：`POST /api/notices`
- 发布公告：`PATCH /api/notices/{id}/publish`
- 归档公告：`PATCH /api/notices/{id}/archive`

## 6. 下一步建议

1. 接入 JWT 与 Spring Security，替换当前传 `userId` 方案。
2. 按角色加接口鉴权（教师审批、学生申请、管理员发布公告）。
3. 前端 axios 全量切换到 `/api/repairs`、`/api/applications`、`/api/notices`。

