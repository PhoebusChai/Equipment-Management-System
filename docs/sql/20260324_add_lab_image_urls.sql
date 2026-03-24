ALTER TABLE labs
  ADD COLUMN image_urls TEXT COMMENT '实验室图片URL列表(JSON)' AFTER description;

