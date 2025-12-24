-- 订单主表
CREATE TABLE IF NOT EXISTS `oms_order` (
    `order_id` VARCHAR(64) NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `store_id` VARCHAR(64) DEFAULT NULL COMMENT '门店ID',
    
    -- 金额信息（单位：分）
    `total_amount` BIGINT DEFAULT 0 COMMENT '商品总金额',
    `freight_amount` BIGINT DEFAULT 0 COMMENT '运费',
    `coupon_amount` BIGINT DEFAULT 0 COMMENT '优惠券抵扣',
    `promotion_amount` BIGINT DEFAULT 0 COMMENT '促销优惠',
    `pay_amount` BIGINT DEFAULT 0 COMMENT '实付金额',
    
    -- 收货信息
    `receiver_name` VARCHAR(64) DEFAULT NULL COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(20) DEFAULT NULL COMMENT '收货人电话',
    `receiver_province` VARCHAR(64) DEFAULT NULL COMMENT '省',
    `receiver_city` VARCHAR(64) DEFAULT NULL COMMENT '市',
    `receiver_district` VARCHAR(64) DEFAULT NULL COMMENT '区',
    `receiver_address` VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
    `receiver_full_address` VARCHAR(500) DEFAULT NULL COMMENT '完整地址',
    
    -- 订单状态
    `status` TINYINT DEFAULT 0 COMMENT '订单状态：0待付款 1待发货 2待收货 3已完成 4已取消 5售后中',
    `pay_status` TINYINT DEFAULT 0 COMMENT '支付状态：0未支付 1已支付',
    `pay_type` TINYINT DEFAULT 1 COMMENT '支付方式：1微信支付',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `delivery_time` DATETIME DEFAULT NULL COMMENT '发货时间',
    `receive_time` DATETIME DEFAULT NULL COMMENT '收货时间',
    `finish_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间',
    
    -- 物流信息
    `delivery_company` VARCHAR(64) DEFAULT NULL COMMENT '物流公司',
    `delivery_sn` VARCHAR(64) DEFAULT NULL COMMENT '物流单号',
    
    -- 其他
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '订单备注',
    `goods_count` INT DEFAULT 0 COMMENT '商品数量',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1已删除',
    
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (`order_id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 订单商品明细表
CREATE TABLE IF NOT EXISTS `oms_order_item` (
    `order_item_id` VARCHAR(64) NOT NULL COMMENT '订单商品ID',
    `order_id` VARCHAR(64) NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
    
    -- 商品信息
    `spu_id` VARCHAR(64) DEFAULT NULL COMMENT 'SPU ID',
    `sku_id` VARCHAR(64) DEFAULT NULL COMMENT 'SKU ID',
    `goods_name` VARCHAR(255) DEFAULT NULL COMMENT '商品名称',
    `goods_image` VARCHAR(500) DEFAULT NULL COMMENT '商品图片',
    `specs` VARCHAR(500) DEFAULT NULL COMMENT '商品规格JSON',
    
    -- 价格信息（单位：分）
    `price` BIGINT DEFAULT 0 COMMENT '商品单价',
    `quantity` INT DEFAULT 1 COMMENT '购买数量',
    `total_amount` BIGINT DEFAULT 0 COMMENT '小计金额',
    `pay_amount` BIGINT DEFAULT 0 COMMENT '实付金额',
    `coupon_amount` BIGINT DEFAULT 0 COMMENT '优惠券抵扣',
    `promotion_amount` BIGINT DEFAULT 0 COMMENT '促销优惠',
    
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    PRIMARY KEY (`order_item_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品明细表';
