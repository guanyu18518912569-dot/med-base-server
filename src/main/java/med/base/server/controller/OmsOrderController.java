package med.base.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import med.base.server.annotation.UserLoginToken;
import med.base.server.common.DefaultResponse;
import med.base.server.mapper.OmsOrderItemMapper;
import med.base.server.model.OmsOrder;
import med.base.server.model.OmsOrderItem;
import med.base.server.service.OmsOrderService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理后台订单接口
 */
@RestController
@RequestMapping("/admin/oms/order")
@RequiredArgsConstructor
public class OmsOrderController {

    private final OmsOrderService orderService;
    private final OmsOrderItemMapper orderItemMapper;

    /**
     * 分页查询订单列表
     */
    @GetMapping("/list")
    @UserLoginToken
    public String list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String receiverName,
            @RequestParam(required = false) String receiverPhone) {

        try {
            LambdaQueryWrapper<OmsOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OmsOrder::getDeleted, 0);

            if (StringUtils.hasText(orderNo)) {
                wrapper.like(OmsOrder::getOrderNo, orderNo);
            }
            if (status != null && status >= 0) {
                wrapper.eq(OmsOrder::getStatus, status);
            }
            if (StringUtils.hasText(receiverName)) {
                wrapper.like(OmsOrder::getReceiverName, receiverName);
            }
            if (StringUtils.hasText(receiverPhone)) {
                wrapper.like(OmsOrder::getReceiverPhone, receiverPhone);
            }

            wrapper.orderByDesc(OmsOrder::getCreatedTime);

            Page<OmsOrder> page = new Page<>(pageNum, pageSize);
            IPage<OmsOrder> result = orderService.page(page, wrapper);

            // 转换为 VO
            List<OrderListVO> voList = result.getRecords().stream().map(order -> {
                OrderListVO vo = new OrderListVO();
                vo.setOrderId(order.getOrderId());
                vo.setOrderNo(order.getOrderNo());
                vo.setUserId(order.getUserId());
                vo.setStatus(order.getStatus());
                vo.setStatusDesc(getStatusDesc(order.getStatus()));
                vo.setPayStatus(order.getPayStatus());
                vo.setPayStatusDesc(order.getPayStatus() == 1 ? "已支付" : "未支付");
                vo.setTotalAmount(order.getTotalAmount());
                vo.setPayAmount(order.getPayAmount());
                vo.setFreightAmount(order.getFreightAmount());
                vo.setGoodsCount(order.getGoodsCount());
                vo.setReceiverName(order.getReceiverName());
                vo.setReceiverPhone(order.getReceiverPhone());
                vo.setReceiverFullAddress(order.getReceiverFullAddress());
                vo.setRemark(order.getRemark());
                vo.setDeliveryCompany(order.getDeliveryCompany());
                vo.setDeliverySn(order.getDeliverySn());
                vo.setCreatedTime(order.getCreatedTime());
                vo.setPayTime(order.getPayTime());
                vo.setDeliveryTime(order.getDeliveryTime());
                vo.setReceiveTime(order.getReceiveTime());

                // 加载订单商品
                List<OmsOrderItem> items = orderItemMapper.selectByOrderId(order.getOrderId());
                List<OrderItemVO> itemVOs = items.stream().map(item -> {
                    OrderItemVO itemVO = new OrderItemVO();
                    itemVO.setOrderItemId(item.getOrderItemId());
                    itemVO.setSpuId(item.getSpuId());
                    itemVO.setSkuId(item.getSkuId());
                    itemVO.setGoodsName(item.getGoodsName());
                    itemVO.setGoodsImage(item.getGoodsImage());
                    itemVO.setSpecs(item.getSpecs());
                    itemVO.setPrice(item.getPrice());
                    itemVO.setQuantity(item.getQuantity());
                    itemVO.setTotalAmount(item.getTotalAmount());
                    return itemVO;
                }).collect(Collectors.toList());
                vo.setOrderItems(itemVOs);

                return vo;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("list", voList);
            data.put("total", result.getTotal());
            data.put("pageNum", result.getCurrent());
            data.put("pageSize", result.getSize());
            data.put("pages", result.getPages());

            return DefaultResponse.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("查询订单列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/detail/{orderId}")
    @UserLoginToken
    public String detail(@PathVariable String orderId) {
        try {
            OmsOrder order = orderService.getOrderDetail(orderId);
            if (order == null) {
                return DefaultResponse.error("订单不存在");
            }

            OrderListVO vo = convertToVO(order);
            return DefaultResponse.success(vo);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("查询订单详情失败：" + e.getMessage());
        }
    }

    /**
     * 发货
     */
    @PostMapping("/deliver")
    @UserLoginToken
    public String deliver(@RequestBody DeliverRequest request) {
        try {
            if (!StringUtils.hasText(request.getOrderId())) {
                return DefaultResponse.error("订单ID不能为空");
            }

            boolean success = orderService.deliverOrder(
                    request.getOrderId(),
                    request.getDeliveryCompany(),
                    request.getDeliverySn());

            if (success) {
                return DefaultResponse.success();
            } else {
                return DefaultResponse.error("发货失败，请检查订单状态");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("发货失败：" + e.getMessage());
        }
    }

    /**
     * 订单统计
     */
    @GetMapping("/statistics")
    @UserLoginToken
    public String statistics() {
        try {
            LambdaQueryWrapper<OmsOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OmsOrder::getDeleted, 0);

            long totalCount = orderService.count(wrapper);

            // 各状态订单数量
            Map<String, Long> statusCount = new HashMap<>();
            for (int i = 0; i <= 5; i++) {
                LambdaQueryWrapper<OmsOrder> statusWrapper = new LambdaQueryWrapper<>();
                statusWrapper.eq(OmsOrder::getDeleted, 0);
                statusWrapper.eq(OmsOrder::getStatus, i);
                statusCount.put(getStatusDesc(i), orderService.count(statusWrapper));
            }

            Map<String, Object> data = new HashMap<>();
            data.put("totalCount", totalCount);
            data.put("statusCount", statusCount);

            return DefaultResponse.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("查询统计失败：" + e.getMessage());
        }
    }


    private String getStatusDesc(Integer status) {
        return med.base.server.enums.OrderStatus.getDescByCode(status);
    }

    private OrderListVO convertToVO(OmsOrder order) {
        OrderListVO vo = new OrderListVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(getStatusDesc(order.getStatus()));
        vo.setPayStatus(order.getPayStatus());
        vo.setPayStatusDesc(order.getPayStatus() == 1 ? "已支付" : "未支付");
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setGoodsCount(order.getGoodsCount());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverFullAddress(order.getReceiverFullAddress());
        vo.setRemark(order.getRemark());
        vo.setDeliveryCompany(order.getDeliveryCompany());
        vo.setDeliverySn(order.getDeliverySn());
        vo.setCreatedTime(order.getCreatedTime());
        vo.setPayTime(order.getPayTime());
        vo.setDeliveryTime(order.getDeliveryTime());
        vo.setReceiveTime(order.getReceiveTime());

        if (order.getOrderItems() != null) {
            List<OrderItemVO> itemVOs = order.getOrderItems().stream().map(item -> {
                OrderItemVO itemVO = new OrderItemVO();
                itemVO.setOrderItemId(item.getOrderItemId());
                itemVO.setSpuId(item.getSpuId());
                itemVO.setSkuId(item.getSkuId());
                itemVO.setGoodsName(item.getGoodsName());
                itemVO.setGoodsImage(item.getGoodsImage());
                itemVO.setSpecs(item.getSpecs());
                itemVO.setPrice(item.getPrice());
                itemVO.setQuantity(item.getQuantity());
                itemVO.setTotalAmount(item.getTotalAmount());
                return itemVO;
            }).collect(Collectors.toList());
            vo.setOrderItems(itemVOs);
        }

        return vo;
    }
}

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
class OrderListVO {
    private String orderId;
    private String orderNo;
    private String userId;
    private Integer status;
    private String statusDesc;
    private Integer payStatus;
    private String payStatusDesc;
    private Long totalAmount;
    private Long payAmount;
    private Long freightAmount;
    private Integer goodsCount;
    private String receiverName;
    private String receiverPhone;
    private String receiverFullAddress;
    private String remark;
    private String deliveryCompany;
    private String deliverySn;
    private LocalDateTime createdTime;
    private LocalDateTime payTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime receiveTime;
    private List<OrderItemVO> orderItems;
}

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
class OrderItemVO {
    private String orderItemId;
    private String spuId;
    private String skuId;
    private String goodsName;
    private String goodsImage;
    private String specs;
    private Long price;
    private Integer quantity;
    private Long totalAmount;
}

@Data
class DeliverRequest {
    private String orderId;
    private String deliveryCompany;
    private String deliverySn;
}
