-- ums_address 收货地址表
-- 用户收货地址管理

CREATE TABLE IF NOT EXISTS `ums_address` (
  `address_id` VARCHAR(32) NOT NULL COMMENT '地址ID',
  `user_id` VARCHAR(32) NOT NULL COMMENT '用户ID',
  `receiver_name` VARCHAR(64) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(20) NOT NULL COMMENT '收货人电话',
  `province_code` VARCHAR(10) DEFAULT NULL COMMENT '省份编码',
  `province_name` VARCHAR(50) DEFAULT NULL COMMENT '省份名称',
  `city_code` VARCHAR(10) DEFAULT NULL COMMENT '城市编码',
  `city_name` VARCHAR(50) DEFAULT NULL COMMENT '城市名称',
  `district_code` VARCHAR(10) DEFAULT NULL COMMENT '区县编码',
  `district_name` VARCHAR(50) DEFAULT NULL COMMENT '区县名称',
  `detail_address` VARCHAR(256) NOT NULL COMMENT '详细地址',
  `full_address` VARCHAR(512) DEFAULT NULL COMMENT '完整地址（省+市+区+详细地址）',
  `tag` VARCHAR(20) DEFAULT NULL COMMENT '地址标签（家、公司等）',
  `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否默认地址（0-否, 1-是）',
  `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`address_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- 查询说明：
-- 1. 查询用户所有地址：SELECT * FROM ums_address WHERE user_id = 'xxx' ORDER BY is_default DESC, updated_time DESC;
-- 2. 查询用户默认地址：SELECT * FROM ums_address WHERE user_id = 'xxx' AND is_default = 1;
