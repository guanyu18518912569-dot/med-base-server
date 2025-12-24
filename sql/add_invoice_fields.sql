-- 创建发票表（与订单解耦）
CREATE TABLE IF NOT EXISTS `oms_invoice` (
  `invoice_id` VARCHAR(64) NOT NULL COMMENT '发票ID',
  `order_id` VARCHAR(64) NOT NULL COMMENT '关联订单ID',
  `order_no` VARCHAR(64) COMMENT '订单编号',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  
  -- 发票基本信息
  `invoice_type` INT DEFAULT 0 COMMENT '发票类型：0-不开发票 5-电子普通发票',
  `title_type` INT COMMENT '抬头类型：1-个人 2-公司',
  `invoice_title` VARCHAR(200) COMMENT '发票抬头（个人姓名或公司名称）',
  `tax_no` VARCHAR(50) COMMENT '纳税人识别号（公司开票必填）',
  `content_type` INT COMMENT '发票内容类型：1-商品明细 2-商品类别',
  
  -- 收票人信息
  `email` VARCHAR(100) COMMENT '发票接收邮箱',
  `phone` VARCHAR(20) COMMENT '发票接收手机号',
  
  -- 发票金额
  `amount` BIGINT COMMENT '发票金额（分）',
  
  -- 开票状态
  `invoice_status` INT DEFAULT 0 COMMENT '开票状态：0-待开票 1-已开票 2-开票失败',
  `invoice_no` VARCHAR(100) COMMENT '发票号码',
  `invoice_url` VARCHAR(500) COMMENT '电子发票下载链接',
  `invoice_time` DATETIME COMMENT '开票时间',
  
  -- 其他
  `remark` VARCHAR(500) COMMENT '备注',
  `deleted` INT DEFAULT 0 COMMENT '删除标记：0-正常 1-已删除',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`invoice_id`),
  INDEX `idx_order_id` (`order_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票表';
