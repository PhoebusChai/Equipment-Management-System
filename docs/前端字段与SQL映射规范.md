# 前端字段与 SQL 映射规范

本文档用于联调阶段统一前端（mock）与数据库（`schema.sql`）字段和状态枚举，避免页面直接依赖 SQL 命名。

## 1. 状态枚举映射

### 用户状态

- 前端：`active` / `disabled`
- SQL：`ACTIVE` / `LOCKED`

### 预约状态

- 前端：`pending` / `approved` / `rejected` / `cancelled` / `completed`
- SQL：`PENDING` / `APPROVED` / `REJECTED` / `CANCELLED` / `IN_USE` / `FINISHED`
- 约定：前端 `completed` 对应 SQL `FINISHED`，SQL `IN_USE` 回读映射为前端 `approved`（可按后端需要再细分）

### 报修状态

- 前端：`submitted` / `confirmed` / `in_progress` / `resolved`
- SQL：`SUBMITTED` / `CONFIRMED` / `IN_REPAIR` / `FINISHED` / `REJECTED`
- 约定：前端 `resolved` 对应 SQL `FINISHED`

## 2. 关键实体字段映射

## 用户 users

- `realName -> real_name`
- `role -> role_code`
- `status -> status`（值需映射）
- `createdAt -> created_at`
- `updatedAt -> updated_at`

## 预约 bookings

- `createdByUserId -> user_id`
- `resourceType(lab/device) -> booking_scope(LAB/DEVICE)`
- `resourceId -> lab_id/device_id`（按 `resourceType` 分配）
- `startAt/endAt -> start_time/end_time`
- `isEmergency -> emergency_flag(0/1)`
- `reviewerUserId -> review_user_id`
- `reviewNote -> reject_reason`（仅驳回时）
- `status -> status`（值需映射）
- `createdAt/updatedAt -> created_at/updated_at`

## 报修 repairs -> repair_requests

- `createdByUserId -> reporter_id`
- `handlerUserId -> confirmer_id`
- `resourceType/resourceId -> lab_id/device_id`
- `description -> fault_desc`
- `status -> status`（值需映射）
- `createdAt/updatedAt -> created_at/updated_at`

## 3. 适配函数位置

已在 `frontend/src/mock/db.js` 提供：

- 状态映射：`toSqlBookingStatus`、`fromSqlBookingStatus`、`toSqlRepairStatus`、`toSqlUserStatus` 等
- 实体映射：`toSqlUser`、`fromSqlUser`、`toSqlBooking`、`toSqlRepairRequest`

后续后端联调建议所有接口经此适配层转换，页面仅使用前端统一字段。

