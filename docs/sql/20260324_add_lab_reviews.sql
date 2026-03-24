-- 为实验室评价新增表（与 device_reviews 对齐）
USE equipment_management;

CREATE TABLE IF NOT EXISTS lab_reviews (
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
  UNIQUE KEY uk_lab_reviews_booking (booking_id),
  INDEX idx_lab_reviews_lab (lab_id, created_at),
  INDEX idx_lab_reviews_user (user_id, created_at)
) ENGINE=InnoDB COMMENT='实验室评价表';
