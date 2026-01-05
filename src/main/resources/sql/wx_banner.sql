-- 小程序轮播图表
CREATE TABLE IF NOT EXISTS `wx_banner` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `image_url` VARCHAR(500) NOT NULL COMMENT '图片地址',
  `sort` INT(11) DEFAULT 0 COMMENT '排序（数字越小越靠前）',
  `status` TINYINT(1) DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序轮播图配置表';

