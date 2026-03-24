# 设备预约全链路联调说明

## 1) 前后端契约对齐

### 资源类型
- 前端：`lab` / `device`
- 后端：`LAB` / `DEVICE`
- 约定：仅允许双值映射，非法值直接报错，避免默认兜底导致误预约。

### 预约状态
- 后端状态：`PENDING`、`APPROVED`、`IN_USE`、`REJECTED`、`CANCELLED`、`FINISHED`
- 前端状态：`pending`、`approved`、`rejected`、`cancelled`、`completed`
- 映射规则：
  - `PENDING -> pending`
  - `APPROVED/IN_USE -> approved`
  - `REJECTED -> rejected`
  - `CANCELLED -> cancelled`
  - `FINISHED -> completed`

## 2) 设备预约生命周期

1. 学生提交预约（`POST /api/bookings`）后，状态为 `PENDING`。
2. 教师审批：
   - 通过：`PATCH /api/bookings/{id}/approve`，状态 `APPROVED`。
   - 驳回：`PATCH /api/bookings/{id}/reject`，状态 `REJECTED`。
3. 系统自动过期：
   - 超时未处理 `PENDING` -> `CANCELLED`。
   - 超时进行中 `APPROVED/IN_USE` -> `FINISHED`。
4. 设备状态联动：
   - 存在有效预约（`APPROVED/IN_USE` 且未结束）时设备状态为 `IN_USE`。
   - 无有效预约时设备状态回退为 `AVAILABLE`。

## 3) 权限与评论闭环

### 设备预约审批权限
- 教师仅可审批“自己已开放实验室”下设备的预约。
- 校验方式：通过预约设备的 `labId` 反查教师可管理实验室集合后做包含判断。

### 评价权限
- 仅预约本人可评价。
- 仅 `FINISHED` 预约允许评价。
- 同一 `bookingId` 仅一条评价，重复提交走更新（upsert）。

### 评价接口
- 写入：`POST /api/reviews/upsert`
- 查询资源评价：`GET /api/reviews/resource?resourceType=DEVICE&resourceId=...`
- 兼容保留：`POST /api/reviews` 与 `GET /api/reviews`

## 4) 联调顺序（建议）

1. 设备列表可见且仅可预约设备可被选中。
2. 冲突检测通过后可创建 `DEVICE` 预约。
3. 教师端可看到待审数据并执行通过/驳回。
4. 状态变更后设备 `status` 同步正确。
5. 已完成预约可提交/更新评价。
6. 学生、教师、管理员均可查到一致评价数据。

## 5) 验收清单
- 学生侧：创建、冲突检测、查询我的预约、取消预约流程可用。
- 教师侧：仅能审批其可管理范围内设备预约。
- 后端：自动过期与状态回写无异常。
- 评价侧：权限正确、重复提交为更新、跨角色查询一致。
- 数据契约：`resourceType` 与 `status` 映射在前后端一致且无隐式兜底。
