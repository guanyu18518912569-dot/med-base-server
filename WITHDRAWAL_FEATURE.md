# 分享业绩与提现功能实现说明

## 功能概述
实现了基于订单分成记录的分享业绩统计和提现申请功能。

## 业绩计算逻辑

### 分享业绩定义
用户的分享业绩 = 直推下级每个订单确认收货后的分成金额总和

**计算公式**：
```
单个订单分成金额 = 订单实付金额(元) × 直推比例(inviteIncomeRatio) ÷ 100
分享业绩 = SUM(所有直推下级订单的分成金额)
```

### 数据来源
- 表：`oms_order_allocation`（订单分成记录表）
- 触发时机：订单确认收货时创建分成记录
- 关键字段：
  - `pay_amount`: 订单实付金额（分）
  - `invite_income_ratio`: 直推比例（%）
  - `settlement_status`: 结算状态（0-未结算 1-已结算）

### 收益状态
1. **未结算收益**：订单已确认收货，分成记录已创建，但尚未转入账户余额
2. **已结算收益**：已转入账户余额，可以提现
3. **可提现余额**：用户账户余额（`ums_user.account`字段）

## 数据库变更

### 新增表：ums_withdrawal（提现申请表）

```sql
CREATE TABLE `ums_withdrawal` (
  `withdrawal_id` varchar(64) PRIMARY KEY COMMENT '提现ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `nick_name` varchar(100) COMMENT '用户昵称',
  `amount` decimal(10,2) NOT NULL COMMENT '提现金额(元)',
  `before_balance` decimal(10,2) COMMENT '提现前账户余额(元)',
  `after_balance` decimal(10,2) COMMENT '提现后账户余额(元)',
  `status` int(11) DEFAULT 0 COMMENT '申请状态: 0-待审核 1-已通过 2-已拒绝 3-已完成',
  `remark` varchar(500) COMMENT '审核备注',
  `reviewer_id` varchar(64) COMMENT '审核人ID',
  `reviewer_name` varchar(100) COMMENT '审核人姓名',
  `review_time` datetime COMMENT '审核时间',
  `payment_time` datetime COMMENT '打款时间',
  `payment_method` int(11) COMMENT '收款方式: 1-微信 2-支付宝 3-银行卡',
  `payment_account` varchar(1000) COMMENT '收款账号信息(JSON)',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 修改表：oms_order_allocation

已有字段（无需修改）：
- `settlement_status` int(11) DEFAULT 0 COMMENT '结算状态: 0-未结算 1-已结算'
- `settlement_time` datetime COMMENT '结算时间'

## 后端实现

### 新增文件

#### 1. UmsWithdrawal.java
- 路径：`src/main/java/med/base/server/model/UmsWithdrawal.java`
- 功能：提现申请实体类

#### 2. UmsWithdrawalMapper.java
- 路径：`src/main/java/med/base/server/mapper/UmsWithdrawalMapper.java`
- 功能：提现申请数据访问层

#### 3. UmsWithdrawalService.java
- 路径：`src/main/java/med/base/server/service/UmsWithdrawalService.java`
- 功能：提现业务逻辑
  - `getUserIncomeStats()`: 获取用户收益统计
  - `applyWithdrawal()`: 申请提现
  - `reviewWithdrawal()`: 审核提现
  - `confirmPayment()`: 确认打款
  - `getUserWithdrawals()`: 获取提现记录

### 修改文件

#### 1. OmsOrderAllocationMapper.java
新增方法：
- `sumSettledIncome()`: 计算已结算收益
- `sumUnsettledIncome()`: 计算未结算收益

#### 2. WxAppController.java
新增接口：
- `GET /wxapp/user/income-stats`: 获取收益统计
- `POST /wxapp/user/apply-withdrawal`: 申请提现
- `GET /wxapp/user/withdrawals`: 获取提现记录

## 小程序端实现

### 新增页面：pages/user/withdraw（提现申请页）

文件：
- `index.wxml`: 页面结构
- `index.js`: 页面逻辑
- `index.wxss`: 页面样式
- `index.json`: 页面配置

功能：
- 显示可提现余额
- 输入提现金额（支持全部提现）
- 选择提现方式（微信/支付宝/银行卡）
- 输入收款账号
- 提交提现申请

### 修改页面：pages/user/my-team（我的团队）

#### index.wxml
- 新增收益卡片，显示：
  - 可提现金额
  - 已结算收益
  - 未结算收益
- 添加"申请提现"按钮
- 修改统计卡片，显示总收益

#### index.js
- 新增 `fetchIncomeStats()` 调用，获取收益统计
- 新增 `handleWithdraw()` 方法，跳转到提现页面

#### index.wxss
- 新增收益卡片样式
- 调整统计卡片样式

### 修改服务：services/user/auth.js

新增方法：
- `fetchIncomeStats()`: 获取收益统计
- `applyWithdrawal()`: 申请提现
- `fetchWithdrawals()`: 获取提现记录
- `fetchUserInfo()`: 获取用户信息
- `fetchDirectChildren()`: 获取直推列表
- `fetchTeamMembers()`: 获取团队列表

## 提现流程

### 1. 用户申请提现
1. 用户在小程序"我的团队"页面查看可提现余额
2. 点击"申请提现"按钮
3. 填写提现金额、选择提现方式、输入收款账号
4. 提交申请
5. 系统验证余额，创建提现记录
6. 账户余额被冻结（扣减相应金额）

### 2. 管理员审核
1. 管理员在后台查看待审核的提现申请
2. 审核通过或拒绝
3. 如果拒绝，冻结金额退回用户账户

### 3. 确认打款
1. 审核通过后，管理员手动打款
2. 打款完成后，在系统中确认打款
3. 提现状态更新为"已完成"

## 后台管理功能（待实现）

需要在管理后台添加：
1. 提现申请列表页面
2. 提现审核功能
3. 打款确认功能
4. 提现统计报表

建议路由：
- `/admin/withdrawal/list` - 提现申请列表
- `/admin/withdrawal/review` - 审核提现

## 状态流转

### 提现申请状态
- `0` - 待审核：用户刚提交申请
- `1` - 已通过：管理员审核通过，待打款
- `2` - 已拒绝：管理员拒绝，金额退回
- `3` - 已完成：已打款完成

### 结算状态
- `0` - 未结算：分成记录已创建，但未转入账户余额
- `1` - 已结算：已转入账户余额，可提现

## 注意事项

1. **金额精度**：所有金额使用 `BigDecimal` 类型，确保精度
2. **事务控制**：提现申请、审核、退款等操作需要事务保证数据一致性
3. **余额验证**：提现前必须验证账户余额是否充足
4. **冻结机制**：提现申请时立即扣减余额，防止重复提现
5. **安全性**：后台审核接口需要管理员权限验证

## 后续优化建议

1. **自动结算**：定期任务将未结算收益转为已结算
2. **提现限制**：设置最低提现金额、每日提现次数限制
3. **手续费**：可以设置提现手续费
4. **提现通知**：提现状态变更时发送微信通知
5. **提现记录**：在小程序端显示提现历史记录

## 实施日期
2026年1月4日
