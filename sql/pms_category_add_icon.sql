-- 为 pms_category 表添加 icon 字段
-- 用于存储分类图标/图片的URL

ALTER TABLE pms_category 
ADD COLUMN `icon` VARCHAR(500) DEFAULT NULL COMMENT '分类图标/图片URL' 
AFTER `sort`;

-- 如果需要回滚，执行以下语句：
-- ALTER TABLE pms_category DROP COLUMN `icon`;
