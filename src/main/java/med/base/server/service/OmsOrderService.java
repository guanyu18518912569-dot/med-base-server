package med.base.server.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import med.base.server.mapper.OmsOrderItemMapper;
import med.base.server.mapper.OmsOrderMapper;
import med.base.server.mapper.PmsSkuMapper;
import med.base.server.mapper.UmsUserMapper;
import med.base.server.model.OmsOrder;
import med.base.server.model.OmsOrderItem;
import med.base.server.model.PmsSku;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OmsOrderService extends ServiceImpl<OmsOrderMapper, OmsOrder> {

    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper orderItemMapper;
    private final PmsSkuMapper skuMapper;
    private final UmsUserMapper userMapper;

    @Autowired
    private OmsOrderAllocationService allocationService;

    public OmsOrderService(OmsOrderMapper orderMapper, OmsOrderItemMapper orderItemMapper,
                           PmsSkuMapper skuMapper, UmsUserMapper userMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.skuMapper = skuMapper;
        this.userMapper = userMapper;
    }

    /**
     * 创建订单
     */
    @Transactional(rollbackFor = Exception.class)
    public OmsOrder createOrder(OmsOrder order, List<OmsOrderItem> orderItems) {
        // 生成订单ID和订单编号
        String orderId = generateOrderId();
        String orderNo = generateOrderNo();

        order.setOrderId(orderId);
        order.setOrderNo(orderNo);
        order.setStatus(0); // 待付款
        order.setPayStatus(0); // 未支付
        order.setDeleted(0);
        order.setCreatedTime(LocalDateTime.now());
        order.setUpdatedTime(LocalDateTime.now());

        // 计算订单金额
        long totalAmount = 0L;
        int goodsCount = 0;

        for (OmsOrderItem item : orderItems) {
            item.setOrderItemId(generateUUID());
            item.setOrderId(orderId);
            item.setOrderNo(orderNo);
            item.setCreatedTime(LocalDateTime.now());

            // 计算小计
            long itemTotal = item.getPrice() * item.getQuantity();
            item.setTotalAmount(itemTotal);
            item.setPayAmount(itemTotal); // 暂不考虑优惠
            item.setCouponAmount(0L);
            item.setPromotionAmount(0L);

            totalAmount += itemTotal;
            goodsCount += item.getQuantity();

            // 扣减库存
            if (item.getSkuId() != null) {
                PmsSku sku = skuMapper.selectById(item.getSkuId());
                if (sku != null && sku.getStock() >= item.getQuantity()) {
                    sku.setStock(sku.getStock() - item.getQuantity());
                    skuMapper.updateById(sku);
                }
            }

            // 保存订单商品
            orderItemMapper.insert(item);
        }

        order.setTotalAmount(totalAmount);
        order.setGoodsCount(goodsCount);
        order.setFreightAmount(0L); // 暂时免运费
        order.setCouponAmount(0L);
        order.setPromotionAmount(0L);
        order.setPayAmount(totalAmount); // 实付 = 商品总额 - 优惠

        // 保存订单
        orderMapper.insert(order);

        order.setOrderItems(orderItems);
        return order;
    }

    /**
     * 查询用户订单列表
     */
    public List<OmsOrder> getUserOrders(String userId, Integer status) {
        List<OmsOrder> orders;
        if (status == null || status == -1) {
            orders = orderMapper.selectByUserId(userId);
        } else {
            orders = orderMapper.selectByUserIdAndStatus(userId, status);
        }

        // 加载订单商品
        for (OmsOrder order : orders) {
            List<OmsOrderItem> items = orderItemMapper.selectByOrderId(order.getOrderId());
            order.setOrderItems(items);
        }

        return orders;
    }

    /**
     * 查询订单详情
     */
    public OmsOrder getOrderDetail(String orderId) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order != null) {
            List<OmsOrderItem> items = orderItemMapper.selectByOrderId(orderId);
            order.setOrderItems(items);
        }
        return order;
    }

    /**
     * 根据订单编号查询
     */
    public OmsOrder getByOrderNo(String orderNo) {
        OmsOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order != null) {
            List<OmsOrderItem> items = orderItemMapper.selectByOrderNo(orderNo);
            order.setOrderItems(items);
        }
        return order;
    }

    /**
     * 取消订单
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(String orderId) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 0) {
            return false;
        }

        // 恢复库存
        List<OmsOrderItem> items = orderItemMapper.selectByOrderId(orderId);
        for (OmsOrderItem item : items) {
            if (item.getSkuId() != null) {
                PmsSku sku = skuMapper.selectById(item.getSkuId());
                if (sku != null) {
                    sku.setStock(sku.getStock() + item.getQuantity());
                    skuMapper.updateById(sku);
                }
            }
        }

        return orderMapper.cancelOrder(orderId) > 0;
    }

    /**
     * 确认收货
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmReceive(String orderId, String userId) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 2) {
            return false;
        }

        int result = orderMapper.confirmReceive(orderId);

        if (result > 0) {
            // 订单完成后，更新用户业绩
            updateUserPerformance(order);

            // 订单完成后，创建分成记录
            try {
                allocationService.createAllocation(order);
            } catch (Exception e) {
                // 分成记录创建失败不影响订单状态
                e.printStackTrace();
            }
        }

        return result > 0;
    }

    /**
     * 模拟支付完成（实际应由支付回调触发）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean payOrder(String orderId) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 0) {
            return false;
        }

        return orderMapper.updatePayStatus(orderId, 1) > 0;
    }

    /**
     * 根据订单编号支付订单（微信支付回调使用）
     * @param orderNo 订单编号
     * @param transactionId 微信支付交易号
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean payOrderByOrderNo(String orderNo, String transactionId) {
        OmsOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            return false;
        }

        // 订单已支付，幂等处理
        if (order.getStatus() != 0) {
            return true;
        }

        // 更新订单状态
        int result = orderMapper.updatePayStatusByOrderNo(orderNo, transactionId);

        // 注意：业绩更新已改为确认收货时触发，支付时不再更新业绩

        return result > 0;
    }

    /**
     * 发货
     */
    public boolean deliverOrder(String orderId, String deliveryCompany, String deliverySn) {
        return orderMapper.deliverOrder(orderId, deliveryCompany, deliverySn) > 0;
    }

    /**
     * 更新用户业绩（确认收货后调用）
     * 从 oms_order_allocation 表读取数据计算业绩
     */
    private void updateUserPerformance(OmsOrder order) {
        try {
            String userId = order.getUserId();
            BigDecimal orderAmount = new BigDecimal(order.getPayAmount()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);

            // 更新个人消费（订单总金额）
            userMapper.addSelfConsumption(userId, orderAmount);

            // 增加积分（每消费1元=1积分，金额单位是分，所以除以100取整）
            Long points = order.getPayAmount() / 100;
            if (points > 0) {
                userMapper.addPoints(userId, points);
            }

            // 从 oms_order_allocation 表获取分成记录，计算实际业绩金额
            List<med.base.server.model.OmsOrderAllocation> allocations =
                    allocationService.getByOrderId(order.getOrderId());

            BigDecimal performanceAmount = BigDecimal.ZERO;

            for (med.base.server.model.OmsOrderAllocation allocation : allocations) {
                if (allocation.getInviteIncomeRatio() != null &&
                    allocation.getInviteIncomeRatio().compareTo(BigDecimal.ZERO) > 0) {
                    // 商品实付金额（元）
                    BigDecimal itemAmount = new BigDecimal(allocation.getPayAmount())
                            .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                    // 根据 inviteIncomeRatio 计算业绩金额
                    BigDecimal itemPerformance = itemAmount.multiply(allocation.getInviteIncomeRatio());
                    performanceAmount = performanceAmount.add(itemPerformance);
                }
            }

            // 如果计算出的业绩金额为0（商品未设置比例），则使用订单总金额
            if (performanceAmount.compareTo(BigDecimal.ZERO) == 0) {
                performanceAmount = orderAmount;
            }

            // 获取用户信息，更新上级业绩
            med.base.server.model.UmsUser user = userMapper.selectById(userId);
            if (user != null && user.getParentId() != null) {
                // 更新直接上级的直推业绩（使用计算后的业绩金额）
                userMapper.addDirectPerformance(user.getParentId(), performanceAmount);

                // 更新所有上级的团队业绩
                if (user.getParentPath() != null && !user.getParentPath().isEmpty()) {
                    String[] parentIds = user.getParentPath().split(",");
                    for (String parentId : parentIds) {
                        if (!parentId.isEmpty()) {
                            userMapper.addTeamPerformance(parentId, performanceAmount);
                        }
                    }
                }
                // 直接上级也要更新团队业绩
                userMapper.addTeamPerformance(user.getParentId(), performanceAmount);
            }
        } catch (Exception e) {
            // 业绩更新失败不影响订单状态
            e.printStackTrace();
        }
    }

    /**
     * 获取订单状态统计
     */
    public java.util.Map<Integer, Long> getOrderCountByStatus(String userId) {
        List<java.util.Map<String, Object>> list = orderMapper.countByStatus(userId);
        java.util.Map<Integer, Long> result = new java.util.HashMap<>();
        for (java.util.Map<String, Object> map : list) {
            Integer status = ((Number) map.get("status")).intValue();
            Long count = ((Number) map.get("count")).longValue();
            result.put(status, count);
        }
        return result;
    }

    /**
     * 生成订单ID
     */
    private String generateOrderId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成订单编号（年月日时分秒 + 4位随机数）
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return timestamp + random;
    }

    /**
     * 生成UUID
     */
    private String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
