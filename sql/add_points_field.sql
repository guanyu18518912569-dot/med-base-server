-- 为用户表添加积分字段
ALTER TABLE ums_user ADD COLUMN points BIGINT DEFAULT 0 COMMENT '用户积分（每消费1元=1积分）';
