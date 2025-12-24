-- 购物车表
CREATE TABLE IF NOT EXISTS `oms_cart` (
    `cart_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `store_id` VARCHAR(64) DEFAULT '1' COMMENT '门店ID',
    `spu_id` VARCHAR(64) DEFAULT NULL COMMENT 'SPU ID',
    `sku_id` VARCHAR(64) DEFAULT NULL COMMENT 'SKU ID',
    `goods_name` VARCHAR(255) DEFAULT NULL COMMENT '商品名称',
    `goods_image` VARCHAR(500) DEFAULT NULL COMMENT '商品图片',
    `specs` VARCHAR(500) DEFAULT NULL COMMENT '规格信息（JSON格式）',
    `price` BIGINT DEFAULT 0 COMMENT '单价（分）',
    `quantity` INT DEFAULT 1 COMMENT '数量',
    `is_selected` TINYINT DEFAULT 1 COMMENT '是否选中：0-未选中 1-已选中',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`cart_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_user_sku` (`user_id`, `sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';
