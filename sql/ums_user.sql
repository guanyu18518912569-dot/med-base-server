-- ums_user 会员用户表
-- 支持多级推荐关系的直销电商用户表

CREATE TABLE IF NOT EXISTS `ums_user` (
  `ums_user_id` VARCHAR(32) NOT NULL COMMENT '用户ID',
  `nick_name` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  `openid` VARCHAR(64) DEFAULT NULL COMMENT '微信openid',
  `unionid` VARCHAR(64) DEFAULT NULL COMMENT '微信unionid',
  `photo_url` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
  `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别',
  `birthday` VARCHAR(20) DEFAULT NULL COMMENT '生日',
  `country` VARCHAR(50) DEFAULT NULL COMMENT '国家',
  `province` VARCHAR(50) DEFAULT NULL COMMENT '省份',
  `city` VARCHAR(50) DEFAULT NULL COMMENT '城市',
  `account` DECIMAL(12,2) DEFAULT 0.00 COMMENT '账户余额（可提现金额）',
  `session_key` VARCHAR(128) DEFAULT NULL COMMENT '微信session_key',
  
  -- 推荐关系
  `parent_id` VARCHAR(32) DEFAULT NULL COMMENT '直接推荐人ID（上级，一级）',
  `parent_path` VARCHAR(1024) DEFAULT NULL COMMENT '推荐路径（逗号分隔，如: U001,U002,U003）',
  `level` INT DEFAULT 0 COMMENT '推荐层级（顶级为0）',
  
  -- 业绩统计
  `direct_count` INT DEFAULT 0 COMMENT '直推人数（一级下线数量）',
  `team_count` INT DEFAULT 0 COMMENT '团队总人数（所有下级数量）',
  `self_consumption` DECIMAL(12,2) DEFAULT 0.00 COMMENT '个人消费总额',
  `direct_performance` DECIMAL(12,2) DEFAULT 0.00 COMMENT '直推业绩（一级下线消费总额）',
  `team_performance` DECIMAL(12,2) DEFAULT 0.00 COMMENT '团队业绩（所有下级消费总额）',
  `total_income` DECIMAL(12,2) DEFAULT 0.00 COMMENT '累计收益（历史总收益）',
  
  -- 会员等级
  `member_level` INT DEFAULT 0 COMMENT '会员等级（0-普通会员, 1-VIP, 2-高级VIP等）',
  `status` INT DEFAULT 0 COMMENT '会员状态（0-正常, 1-冻结, 2-注销）',
  `invite_code` VARCHAR(16) DEFAULT NULL COMMENT '邀请码（有规律的数字，从100000开始）',
  
  -- 时间戳
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`ums_user_id`),
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `uk_invite_code` (`invite_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_parent_path` (`parent_path`(255)),
  KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员用户表';

-- 查询说明：
-- 1. 查询某用户的直接下级：SELECT * FROM ums_user WHERE parent_id = 'U001';
-- 2. 查询某用户的所有下级（团队）：SELECT * FROM ums_user WHERE FIND_IN_SET('U001', parent_path) > 0;
-- 3. 查询某用户的邀请码：SELECT invite_code FROM ums_user WHERE ums_user_id = 'U001';
-- 4. 通过邀请码查找用户：SELECT * FROM ums_user WHERE invite_code = '100001';
