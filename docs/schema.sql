-- 设备与实验室管理系统建表脚本（MySQL 8.x）
-- 说明：每张表和每个字段均通过 COMMENT 标注字段含义

CREATE DATABASE IF NOT EXISTS equipment_management
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE equipment_management;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS operation_logs;
DROP TABLE IF EXISTS checkin_logs;
DROP TABLE IF EXISTS assets_inbound_records;
DROP TABLE IF EXISTS scrap_apply_forms;
DROP TABLE IF EXISTS device_apply_forms;
DROP TABLE IF EXISTS lab_apply_forms;
DROP TABLE IF EXISTS maintenance_plans;
DROP TABLE IF EXISTS repair_work_orders;
DROP TABLE IF EXISTS repair_requests;
DROP TABLE IF EXISTS notices;
DROP TABLE IF EXISTS lab_reviews;
DROP TABLE IF EXISTS device_reviews;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS devices;
DROP TABLE IF EXISTS labs;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  role_code VARCHAR(32) NOT NULL COMMENT '角色编码：STUDENT/TEACHER/ADMIN',
  email VARCHAR(128) NOT NULL UNIQUE COMMENT '邮箱账号',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值',
  real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
  avatar_url VARCHAR(255) COMMENT '头像URL',
  phone VARCHAR(32) COMMENT '联系方式',
  credit_score INT NOT NULL DEFAULT 100 COMMENT '信用积分',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态：ACTIVE/LOCKED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_users_role_code (role_code)
) ENGINE=InnoDB COMMENT='用户表';

CREATE TABLE labs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  lab_code VARCHAR(64) NOT NULL UNIQUE COMMENT '实验室编码',
  lab_name VARCHAR(128) NOT NULL COMMENT '实验室名称',
  lab_type VARCHAR(32) NOT NULL COMMENT '实验室类型：COMPUTER/BIOLOGY',
  building VARCHAR(64) COMMENT '楼宇',
  college VARCHAR(128) COMMENT '所属学院',
  capacity INT COMMENT '容纳人数',
  manager_teacher_id BIGINT COMMENT '负责人教师ID，关联users.id',
  status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE' COMMENT '实验室状态：AVAILABLE/MAINTENANCE/DISABLED',
  description TEXT COMMENT '实验室描述',
  image_urls TEXT COMMENT '实验室图片URL列表(JSON)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_labs_manager_teacher FOREIGN KEY (manager_teacher_id) REFERENCES users (id)
) ENGINE=InnoDB COMMENT='实验室表';

CREATE TABLE devices (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  lab_id BIGINT NOT NULL COMMENT '所属实验室ID，关联labs.id',
  device_code VARCHAR(64) NOT NULL UNIQUE COMMENT '设备编码',
  device_name VARCHAR(128) NOT NULL COMMENT '设备名称',
  category VARCHAR(64) COMMENT '设备分类',
  brand VARCHAR(64) COMMENT '品牌',
  model VARCHAR(64) COMMENT '型号',
  technical_params TEXT COMMENT '技术参数',
  usage_guide_url VARCHAR(255) COMMENT '使用文档URL',
  location VARCHAR(128) COMMENT '设备所在位置',
  status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE' COMMENT '设备状态：AVAILABLE/BOOKED/MAINTENANCE/SCRAPPED',
  purchase_date DATE COMMENT '采购日期',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_devices_lab FOREIGN KEY (lab_id) REFERENCES labs (id)
) ENGINE=InnoDB COMMENT='设备表';

CREATE TABLE bookings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  booking_no VARCHAR(64) NOT NULL UNIQUE COMMENT '预约单号',
  user_id BIGINT NOT NULL COMMENT '预约人ID，关联users.id',
  resource_type VARCHAR(32) NOT NULL COMMENT '资源类型：LAB/DEVICE',
  resource_id BIGINT NOT NULL COMMENT '资源ID（根据resource_type指向labs或devices）',
  booking_scope VARCHAR(32) NOT NULL COMMENT '预约范围：LAB_BOOKING/DEVICE_BOOKING/EMERGENCY',
  start_time DATETIME NOT NULL COMMENT '预约开始时间',
  end_time DATETIME NOT NULL COMMENT '预约结束时间',
  duration_minutes INT NOT NULL COMMENT '预约时长（分钟）',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '预约状态：PENDING/APPROVED/REJECTED/CANCELLED/IN_USE/FINISHED',
  approver_id BIGINT COMMENT '审核人ID，关联users.id',
  reject_reason VARCHAR(255) COMMENT '驳回原因',
  conflict_with_booking_id BIGINT COMMENT '冲突预约ID（冲突时记录，关联bookings.id）',
  conflict_reason VARCHAR(255) COMMENT '冲突原因',
  conflict_resolved TINYINT(1) NOT NULL DEFAULT 0 COMMENT '冲突是否已解决：0否1是',
  conflict_resolved_by BIGINT COMMENT '冲突处理人ID，关联users.id',
  conflict_resolved_at DATETIME NULL COMMENT '冲突处理时间',
  checkin_time DATETIME NULL COMMENT '签到时间',
  checkout_time DATETIME NULL COMMENT '签退时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_bookings_approver FOREIGN KEY (approver_id) REFERENCES users (id),
  CONSTRAINT fk_bookings_conflict_with_booking FOREIGN KEY (conflict_with_booking_id) REFERENCES bookings (id),
  CONSTRAINT fk_bookings_conflict_resolved_by FOREIGN KEY (conflict_resolved_by) REFERENCES users (id),
  INDEX idx_booking_resource_time (resource_type, resource_id, start_time, end_time, status),
  INDEX idx_booking_user (user_id, status, start_time)
) ENGINE=InnoDB COMMENT='预约表';

CREATE TABLE device_reviews (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  booking_id BIGINT NOT NULL COMMENT '预约ID，关联bookings.id',
  device_id BIGINT NOT NULL COMMENT '设备ID，关联devices.id',
  user_id BIGINT NOT NULL COMMENT '评价人ID，关联users.id',
  rating INT NOT NULL COMMENT '评分（1-5）',
  content TEXT COMMENT '评价内容',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_device_reviews_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
  CONSTRAINT fk_device_reviews_device FOREIGN KEY (device_id) REFERENCES devices (id),
  CONSTRAINT fk_device_reviews_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB COMMENT='设备评价表';

CREATE TABLE lab_reviews (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  booking_id BIGINT NOT NULL COMMENT '预约ID，关联bookings.id',
  lab_id BIGINT NOT NULL COMMENT '实验室ID，关联labs.id',
  user_id BIGINT NOT NULL COMMENT '评价人ID，关联users.id',
  rating INT NOT NULL COMMENT '评分（1-5）',
  content TEXT COMMENT '评价内容',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_lab_reviews_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
  CONSTRAINT fk_lab_reviews_lab FOREIGN KEY (lab_id) REFERENCES labs (id),
  CONSTRAINT fk_lab_reviews_user FOREIGN KEY (user_id) REFERENCES users (id),
  UNIQUE KEY uk_lab_reviews_booking (booking_id)
) ENGINE=InnoDB COMMENT='实验室评价表';

CREATE TABLE notices (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  notice_type VARCHAR(32) NOT NULL COMMENT '公告类型：LAB_NOTICE/DEVICE_NOTICE/SYSTEM_NOTICE',
  target_type VARCHAR(32) NOT NULL COMMENT '目标类型：ALL/LAB/DEVICE',
  target_id BIGINT NULL COMMENT '目标资源ID（LAB或DEVICE时使用）',
  title VARCHAR(128) NOT NULL COMMENT '公告标题',
  content TEXT NOT NULL COMMENT '公告内容',
  publisher_id BIGINT NOT NULL COMMENT '发布人ID，关联users.id',
  publish_time DATETIME NOT NULL COMMENT '发布时间',
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '公告状态：DRAFT/PUBLISHED/ARCHIVED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_notices_publisher FOREIGN KEY (publisher_id) REFERENCES users (id)
) ENGINE=InnoDB COMMENT='公告表';

CREATE TABLE repair_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  request_no VARCHAR(64) NOT NULL UNIQUE COMMENT '报修单号',
  reporter_id BIGINT NOT NULL COMMENT '报修人ID，关联users.id',
  resource_type VARCHAR(32) NOT NULL COMMENT '资源类型：LAB/DEVICE',
  resource_id BIGINT NOT NULL COMMENT '资源ID（根据resource_type指向labs或devices）',
  fault_desc TEXT NOT NULL COMMENT '故障描述',
  urgency_level VARCHAR(16) NOT NULL COMMENT '紧急程度：LOW/MEDIUM/HIGH',
  status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED' COMMENT '报修状态：SUBMITTED/CONFIRMED/IN_REPAIR/FINISHED/REJECTED',
  confirmer_id BIGINT COMMENT '确认人ID，关联users.id',
  confirmed_at DATETIME NULL COMMENT '确认时间',
  finished_at DATETIME NULL COMMENT '维修完成时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_repair_requests_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
  CONSTRAINT fk_repair_requests_confirmer FOREIGN KEY (confirmer_id) REFERENCES users (id)
) ENGINE=InnoDB COMMENT='报修申请表';

CREATE TABLE repair_work_orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  repair_request_id BIGINT NOT NULL COMMENT '报修申请ID，关联repair_requests.id',
  worker_name VARCHAR(64) COMMENT '维修人员姓名',
  action_desc TEXT COMMENT '维修处理过程说明',
  result_desc TEXT COMMENT '维修结果说明',
  material_cost DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '物料费用',
  labor_cost DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '人工费用',
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT '工单状态：CREATED/PROCESSING/COMPLETED',
  submitted_at DATETIME NULL COMMENT '工单提交时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_repair_work_orders_request FOREIGN KEY (repair_request_id) REFERENCES repair_requests (id)
) ENGINE=InnoDB COMMENT='维修工单表';

CREATE TABLE maintenance_plans (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  device_id BIGINT NOT NULL COMMENT '设备ID，关联devices.id',
  planner_id BIGINT NOT NULL COMMENT '制定人ID（教师），关联users.id',
  plan_name VARCHAR(128) NOT NULL COMMENT '保养计划名称',
  period_type VARCHAR(16) NOT NULL COMMENT '周期类型：WEEKLY/MONTHLY/QUARTERLY',
  next_execute_time DATETIME NOT NULL COMMENT '下次执行时间',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '计划状态：ACTIVE/PAUSED/FINISHED',
  attachment_url VARCHAR(255) COMMENT '计划附件URL',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_maintenance_plans_device FOREIGN KEY (device_id) REFERENCES devices (id),
  CONSTRAINT fk_maintenance_plans_planner FOREIGN KEY (planner_id) REFERENCES users (id)
) ENGINE=InnoDB COMMENT='设备保养计划表';

CREATE TABLE lab_apply_forms (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  applicant_id BIGINT NOT NULL COMMENT '申请人ID（教师），关联users.id',
  lab_name VARCHAR(128) NOT NULL COMMENT '申请实验室名称',
  lab_type VARCHAR(32) NOT NULL COMMENT '实验室类型：COMPUTER/BIOLOGY',
  reason TEXT NOT NULL COMMENT '申请原因',
  open_date_start DATE NULL COMMENT '开放开始日期（用于预约时段）',
  open_date_end DATE NULL COMMENT '开放结束日期（用于预约时段）',
  daily_start_time TIME NULL COMMENT '每日开放开始时间（用于预约时段）',
  daily_end_time TIME NULL COMMENT '每日开放结束时间（用于预约时段）',
  slot_preset VARCHAR(64) NULL COMMENT '时段模板（如上午第一段/自定义）',
  status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED' COMMENT '申请状态：SUBMITTED/APPROVED/REJECTED',
  reviewer_id BIGINT COMMENT '审核人ID，关联users.id',
  reviewed_at DATETIME NULL COMMENT '审核时间',
  review_comment VARCHAR(255) COMMENT '审核意见',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_lab_apply_forms_applicant FOREIGN KEY (applicant_id) REFERENCES users (id),
  CONSTRAINT fk_lab_apply_forms_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id)
) ENGINE=InnoDB COMMENT='实验室申请表';

CREATE TABLE device_apply_forms (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  applicant_id BIGINT NOT NULL COMMENT '申请人ID（教师），关联users.id',
  device_name VARCHAR(128) NOT NULL COMMENT '申请设备名称',
  category VARCHAR(64) COMMENT '设备分类',
  quantity INT NOT NULL COMMENT '申请数量',
  expected_budget DECIMAL(12,2) COMMENT '预估预算',
  reason TEXT NOT NULL COMMENT '申请原因',
  status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED' COMMENT '申请状态：SUBMITTED/APPROVED/REJECTED',
  reviewer_id BIGINT COMMENT '审核人ID，关联users.id',
  reviewed_at DATETIME NULL COMMENT '审核时间',
  review_comment VARCHAR(255) COMMENT '审核意见',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_device_apply_forms_applicant FOREIGN KEY (applicant_id) REFERENCES users (id),
  CONSTRAINT fk_device_apply_forms_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id)
) ENGINE=InnoDB COMMENT='设备申请表';

CREATE TABLE scrap_apply_forms (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  device_id BIGINT NOT NULL COMMENT '报废设备ID，关联devices.id',
  applicant_id BIGINT NOT NULL COMMENT '申请人ID，关联users.id',
  reason TEXT NOT NULL COMMENT '报废原因',
  status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED' COMMENT '申请状态：SUBMITTED/APPROVED/REJECTED',
  reviewer_id BIGINT COMMENT '审核人ID，关联users.id',
  reviewed_at DATETIME NULL COMMENT '审核时间',
  review_comment VARCHAR(255) COMMENT '审核意见',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_scrap_apply_forms_device FOREIGN KEY (device_id) REFERENCES devices (id),
  CONSTRAINT fk_scrap_apply_forms_applicant FOREIGN KEY (applicant_id) REFERENCES users (id),
  CONSTRAINT fk_scrap_apply_forms_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id)
) ENGINE=InnoDB COMMENT='设备报废申请表';

CREATE TABLE assets_inbound_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  device_id BIGINT NOT NULL COMMENT '设备ID，关联devices.id',
  operator_id BIGINT NOT NULL COMMENT '入库操作人ID，关联users.id',
  supplier_name VARCHAR(128) COMMENT '供应商名称',
  purchase_amount DECIMAL(12,2) COMMENT '采购金额',
  inbound_date DATE NOT NULL COMMENT '入库日期',
  invoice_no VARCHAR(64) COMMENT '发票编号',
  remark VARCHAR(255) COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_assets_inbound_records_device FOREIGN KEY (device_id) REFERENCES devices (id),
  CONSTRAINT fk_assets_inbound_records_operator FOREIGN KEY (operator_id) REFERENCES users (id)
) ENGINE=InnoDB COMMENT='资产入库记录表';

CREATE TABLE checkin_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  booking_id BIGINT NOT NULL COMMENT '预约ID，关联bookings.id',
  user_id BIGINT NOT NULL COMMENT '签到用户ID，关联users.id',
  checkin_time DATETIME NULL COMMENT '签到时间',
  checkout_time DATETIME NULL COMMENT '签退时间',
  is_abnormal TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否异常：0否1是',
  abnormal_reason VARCHAR(255) COMMENT '异常原因',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_checkin_logs_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
  CONSTRAINT fk_checkin_logs_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB COMMENT='签到签退日志表';

CREATE TABLE operation_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  operator_id BIGINT NOT NULL COMMENT '操作人ID，关联users.id',
  operation_module VARCHAR(64) NOT NULL COMMENT '操作模块，如BOOKING/REPAIR/NOTICE',
  operation_type VARCHAR(64) NOT NULL COMMENT '操作类型，如CREATE/UPDATE/DELETE/APPROVE',
  target_type VARCHAR(64) NOT NULL COMMENT '目标实体类型',
  target_id BIGINT NOT NULL COMMENT '目标实体ID',
  detail TEXT COMMENT '操作详情',
  operated_at DATETIME NOT NULL COMMENT '操作时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_operation_logs_operator FOREIGN KEY (operator_id) REFERENCES users (id)
) ENGINE=InnoDB COMMENT='操作审计日志表';

SET FOREIGN_KEY_CHECKS = 1;
