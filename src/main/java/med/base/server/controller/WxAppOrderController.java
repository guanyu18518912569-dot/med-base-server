package med.base.server.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import med.base.server.common.DefaultResponse;
import med.base.server.mapper.OmsInvoiceMapper;
import med.base.server.model.OmsInvoice;
import med.base.server.model.OmsOrder;
import med.base.server.model.OmsOrderItem;
import med.base.server.service.OmsOrderService;
import med.base.server.util.SysUtil;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 微信小程序订单接口
 */
@RestController
@RequestMapping("/wxapp/order")
public class WxAppOrderController {

    private final OmsOrderService orderService;
    private final OmsInvoiceMapper invoiceMapper;

    public WxAppOrderController(OmsOrderService orderService, OmsInvoiceMapper invoiceMapper) {
        this.orderService = orderService;
        this.invoiceMapper = invoiceMapper;
    }

    /**
     * 提交订单
     */
    @PostMapping("/submit")
    public String submitOrder(@RequestBody WxOrderSubmitRequest request) {
        try {
            // 验证参数
            if (request.getUserId() == null || request.getUserId().isEmpty()) {
                return DefaultResponse.error("用户ID不能为空");
            }
            if (request.getGoodsList() == null || request.getGoodsList().isEmpty()) {
                return DefaultResponse.error("商品列表不能为空");
            }
            
            // 构建订单
            OmsOrder order = OmsOrder.builder()
                    .userId(request.getUserId())
                    .storeId(request.getStoreId())
                    .receiverName(request.getReceiverName())
                    .receiverPhone(request.getReceiverPhone())
                    .receiverProvince(request.getReceiverProvince())
                    .receiverCity(request.getReceiverCity())
                    .receiverDistrict(request.getReceiverDistrict())
                    .receiverAddress(request.getReceiverAddress())
                    .receiverFullAddress(buildFullAddress(request))
                    .remark(request.getRemark())
                    .payType(1) // 微信支付
                    .build();
            
            // 构建订单商品列表
            List<OmsOrderItem> orderItems = new ArrayList<>();
            for (WxOrderGoodsItem goods : request.getGoodsList()) {
                OmsOrderItem item = OmsOrderItem.builder()
                        .spuId(goods.getSpuId())
                        .skuId(goods.getSkuId())
                        .goodsName(goods.getGoodsName())
                        .goodsImage(goods.getGoodsImage())
                        .specs(goods.getSpecs())
                        .price(goods.getPrice())
                        .quantity(goods.getQuantity())
                        .build();
                orderItems.add(item);
            }
            
            // 创建订单
            OmsOrder createdOrder = orderService.createOrder(order, orderItems);
            
            // 创建发票记录（如果需要开票）
            if (request.getInvoice() != null && request.getInvoice().getInvoiceType() != null 
                && request.getInvoice().getInvoiceType() > 0) {
                WxInvoiceRequest invoiceReq = request.getInvoice();
                OmsInvoice invoice = OmsInvoice.builder()
                        .invoiceId(SysUtil.createOrderId("INV"))
                        .orderId(createdOrder.getOrderId())
                        .orderNo(createdOrder.getOrderNo())
                        .userId(request.getUserId())
                        .invoiceType(invoiceReq.getInvoiceType())
                        .titleType(invoiceReq.getTitleType())
                        .invoiceTitle(invoiceReq.getBuyerName())
                        .taxNo(invoiceReq.getBuyerTaxNo())
                        .contentType(invoiceReq.getContentType())
                        .email(invoiceReq.getEmail())
                        .phone(invoiceReq.getBuyerPhone())
                        .amount(createdOrder.getPayAmount())
                        .invoiceStatus(0) // 待开票
                        .deleted(0)
                        .build();
                invoiceMapper.insert(invoice);
            }
            
            // 返回订单信息
            WxOrderSubmitResponse response = new WxOrderSubmitResponse();
            response.setOrderId(createdOrder.getOrderId());
            response.setOrderNo(createdOrder.getOrderNo());
            response.setPayAmount(createdOrder.getPayAmount());
            response.setStatus(createdOrder.getStatus());
            
            return DefaultResponse.success(response);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("提交订单失败：" + e.getMessage());
        }
    }

    /**
     * 获取订单列表
     * @param userId 用户ID
     * @param status 订单状态：-1全部 0待付款 1待发货 2待收货 3已完成
     */
    @GetMapping("/list")
    public String getOrderList(@RequestParam String userId,
                                @RequestParam(defaultValue = "-1") Integer status) {
        try {
            List<OmsOrder> orders = orderService.getUserOrders(userId, status);
            
            List<WxOrderListItem> result = orders.stream().map(order -> {
                WxOrderListItem item = new WxOrderListItem();
                item.setOrderId(order.getOrderId());
                item.setOrderNo(order.getOrderNo());
                item.setStatus(order.getStatus());
                item.setStatusDesc(getStatusDesc(order.getStatus()));
                item.setPayAmount(order.getPayAmount());
                item.setGoodsCount(order.getGoodsCount());
                item.setCreatedTime(order.getCreatedTime().toString());
                
                // 商品列表
                List<WxOrderGoodsVO> goodsList = new ArrayList<>();
                if (order.getOrderItems() != null) {
                    for (OmsOrderItem oi : order.getOrderItems()) {
                        WxOrderGoodsVO vo = new WxOrderGoodsVO();
                        vo.setSpuId(oi.getSpuId());
                        vo.setSkuId(oi.getSkuId());
                        vo.setGoodsName(oi.getGoodsName());
                        vo.setGoodsImage(oi.getGoodsImage());
                        vo.setSpecs(oi.getSpecs());
                        vo.setPrice(oi.getPrice());
                        vo.setQuantity(oi.getQuantity());
                        goodsList.add(vo);
                    }
                }
                item.setGoodsList(goodsList);
                
                return item;
            }).collect(Collectors.toList());
            
            return DefaultResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取订单列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/detail/{orderId}")
    public String getOrderDetail(@PathVariable String orderId) {
        try {
            OmsOrder order = orderService.getOrderDetail(orderId);
            if (order == null) {
                return DefaultResponse.error("订单不存在");
            }
            
            return buildOrderDetailResponse(order);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取订单详情失败：" + e.getMessage());
        }
    }

    /**
     * 根据订单编号获取订单详情
     */
    @GetMapping("/detail-by-no")
    public String getOrderDetailByNo(@RequestParam String orderNo) {
        try {
            OmsOrder order = orderService.getByOrderNo(orderNo);
            if (order == null) {
                return DefaultResponse.error("订单不存在");
            }
            
            return buildOrderDetailResponse(order);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取订单详情失败：" + e.getMessage());
        }
    }

    /**
     * 构建订单详情响应
     */
    private String buildOrderDetailResponse(OmsOrder order) {
        WxOrderDetailVO detail = new WxOrderDetailVO();
        detail.setOrderId(order.getOrderId());
        detail.setOrderNo(order.getOrderNo());
        detail.setStatus(order.getStatus());
        detail.setStatusDesc(getStatusDesc(order.getStatus()));
        detail.setPayStatus(order.getPayStatus());
        
        // 金额信息
        detail.setTotalAmount(order.getTotalAmount());
        detail.setFreightAmount(order.getFreightAmount());
        detail.setCouponAmount(order.getCouponAmount());
        detail.setPromotionAmount(order.getPromotionAmount());
        detail.setPayAmount(order.getPayAmount());
        detail.setGoodsCount(order.getGoodsCount());
        
        // 收货信息
        detail.setReceiverName(order.getReceiverName());
        detail.setReceiverPhone(order.getReceiverPhone());
        detail.setReceiverFullAddress(order.getReceiverFullAddress());
        
        // 物流信息
        detail.setDeliveryCompany(order.getDeliveryCompany());
        detail.setDeliverySn(order.getDeliverySn());
        
        // 时间信息
        detail.setCreatedTime(order.getCreatedTime() != null ? order.getCreatedTime().toString() : null);
        detail.setPayTime(order.getPayTime() != null ? order.getPayTime().toString() : null);
        detail.setDeliveryTime(order.getDeliveryTime() != null ? order.getDeliveryTime().toString() : null);
        detail.setReceiveTime(order.getReceiveTime() != null ? order.getReceiveTime().toString() : null);
        
        detail.setRemark(order.getRemark());
        
        // 查询发票信息（从独立的发票表）
        OmsInvoice invoice = invoiceMapper.selectByOrderId(order.getOrderId());
        if (invoice != null && invoice.getInvoiceType() != null && invoice.getInvoiceType() > 0) {
            WxInvoiceVO invoiceVO = new WxInvoiceVO();
            invoiceVO.setInvoiceType(invoice.getInvoiceType());
            invoiceVO.setTitleType(invoice.getTitleType());
            invoiceVO.setBuyerName(invoice.getInvoiceTitle());
            invoiceVO.setBuyerTaxNo(invoice.getTaxNo());
            invoiceVO.setContentType(invoice.getContentType());
            invoiceVO.setEmail(invoice.getEmail());
            invoiceVO.setBuyerPhone(invoice.getPhone());
            invoiceVO.setMoney(invoice.getAmount());
            invoiceVO.setInvoiceStatus(invoice.getInvoiceStatus());
            invoiceVO.setInvoiceNo(invoice.getInvoiceNo());
            invoiceVO.setInvoiceUrl(invoice.getInvoiceUrl());
            detail.setInvoiceVO(invoiceVO);
        }
        
        // 商品列表
        List<WxOrderGoodsVO> goodsList = new ArrayList<>();
        if (order.getOrderItems() != null) {
            for (OmsOrderItem oi : order.getOrderItems()) {
                WxOrderGoodsVO vo = new WxOrderGoodsVO();
                vo.setSpuId(oi.getSpuId());
                vo.setSkuId(oi.getSkuId());
                vo.setGoodsName(oi.getGoodsName());
                vo.setGoodsImage(oi.getGoodsImage());
                vo.setSpecs(oi.getSpecs());
                vo.setPrice(oi.getPrice());
                vo.setQuantity(oi.getQuantity());
                vo.setTotalAmount(oi.getTotalAmount());
                goodsList.add(vo);
            }
        }
        detail.setGoodsList(goodsList);
        
        return DefaultResponse.success(detail);
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel/{orderId}")
    public String cancelOrder(@PathVariable String orderId) {
        try {
            boolean result = orderService.cancelOrder(orderId);
            if (result) {
                return DefaultResponse.success("订单已取消");
            } else {
                return DefaultResponse.error("取消订单失败，订单状态不允许取消");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("取消订单失败：" + e.getMessage());
        }
    }

    /**
     * 确认收货
     */
    @PostMapping("/confirm-receive/{orderId}")
    public String confirmReceive(@PathVariable String orderId, @RequestParam String userId) {
        try {
            boolean result = orderService.confirmReceive(orderId, userId);
            if (result) {
                return DefaultResponse.success("确认收货成功");
            } else {
                return DefaultResponse.error("确认收货失败，订单状态不正确");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("确认收货失败：" + e.getMessage());
        }
    }

    /**
     * 模拟支付（用于测试）
     */
    @PostMapping("/pay/{orderId}")
    public String payOrder(@PathVariable String orderId) {
        try {
            boolean result = orderService.payOrder(orderId);
            if (result) {
                return DefaultResponse.success("支付成功");
            } else {
                return DefaultResponse.error("支付失败，订单状态不正确");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("支付失败：" + e.getMessage());
        }
    }

    /**
     * 获取订单状态统计
     */
    @GetMapping("/count")
    public String getOrderCount(@RequestParam String userId) {
        try {
            Map<Integer, Long> countMap = orderService.getOrderCountByStatus(userId);
            
            WxOrderCountVO vo = new WxOrderCountVO();
            vo.setToPay(countMap.getOrDefault(0, 0L));
            vo.setToDeliver(countMap.getOrDefault(1, 0L));
            vo.setToReceive(countMap.getOrDefault(2, 0L));
            vo.setCompleted(countMap.getOrDefault(3, 0L));
            
            return DefaultResponse.success(vo);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取订单统计失败：" + e.getMessage());
        }
    }

    /**
     * 构建完整地址
     */
    private String buildFullAddress(WxOrderSubmitRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.getReceiverProvince() != null) {
            sb.append(request.getReceiverProvince());
        }
        if (request.getReceiverCity() != null) {
            sb.append(request.getReceiverCity());
        }
        if (request.getReceiverDistrict() != null) {
            sb.append(request.getReceiverDistrict());
        }
        if (request.getReceiverAddress() != null) {
            sb.append(request.getReceiverAddress());
        }
        return sb.toString();
    }

    /**
     * 获取订单状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待付款";
            case 1 -> "待发货";
            case 2 -> "待收货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            case 5 -> "售后中";
            default -> "未知";
        };
    }
}

// ==================== DTO 类 ====================

@Data
class WxOrderSubmitRequest {
    private String userId;
    private String storeId;
    
    // 收货信息
    private String receiverName;
    private String receiverPhone;
    private String receiverProvince;
    private String receiverCity;
    private String receiverDistrict;
    private String receiverAddress;
    
    // 订单备注
    private String remark;
    
    // 发票信息
    private WxInvoiceRequest invoice;
    
    // 商品列表
    private List<WxOrderGoodsItem> goodsList;
}

@Data
class WxInvoiceRequest {
    // 发票类型：0-不开发票 5-电子普通发票
    private Integer invoiceType;
    
    // 发票抬头类型：1-个人 2-公司
    private Integer titleType;
    
    // 发票抬头（个人姓名或公司名称）
    private String buyerName;
    
    // 纳税人识别号（公司开票必填）
    private String buyerTaxNo;
    
    // 发票内容类型：1-商品明细 2-商品类别
    private Integer contentType;
    
    // 发票接收邮箱
    private String email;
    
    // 发票接收手机号（个人开票）
    private String buyerPhone;
}

@Data
class WxOrderGoodsItem {
    private String spuId;
    private String skuId;
    private String goodsName;
    private String goodsImage;
    private String specs;    // 规格JSON
    private Long price;      // 单价（分）
    private Integer quantity;
}

@Data
class WxOrderSubmitResponse {
    private String orderId;
    private String orderNo;
    private Long payAmount;
    private Integer status;
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class WxOrderListItem {
    private String orderId;
    private String orderNo;
    private Integer status;
    private String statusDesc;
    private Long payAmount;
    private Integer goodsCount;
    private String createdTime;
    private List<WxOrderGoodsVO> goodsList;
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class WxOrderDetailVO {
    private String orderId;
    private String orderNo;
    private Integer status;
    private String statusDesc;
    private Integer payStatus;
    
    // 金额
    private Long totalAmount;
    private Long freightAmount;
    private Long couponAmount;
    private Long promotionAmount;
    private Long payAmount;
    private Integer goodsCount;
    
    // 收货信息
    private String receiverName;
    private String receiverPhone;
    private String receiverFullAddress;
    
    // 物流
    private String deliveryCompany;
    private String deliverySn;
    
    // 时间
    private String createdTime;
    private String payTime;
    private String deliveryTime;
    private String receiveTime;
    
    private String remark;
    
    // 发票信息
    private WxInvoiceVO invoiceVO;
    
    private List<WxOrderGoodsVO> goodsList;
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class WxInvoiceVO {
    private Integer invoiceType;      // 发票类型：0-不开发票 5-电子普通发票
    private Integer titleType;        // 抬头类型：1-个人 2-公司
    private String buyerName;         // 发票抬头（个人姓名或公司名称）
    private String buyerTaxNo;        // 纳税人识别号
    private Integer contentType;      // 内容类型：1-商品明细 2-商品类别
    private String email;             // 接收邮箱
    private String buyerPhone;        // 接收手机号
    private Long money;               // 发票金额（分）
    private Integer invoiceStatus;    // 开票状态：0-待开票 1-已开票 2-开票失败
    private String invoiceNo;         // 发票号码
    private String invoiceUrl;        // 电子发票下载链接
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class WxOrderGoodsVO {
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
class WxOrderCountVO {
    private Long toPay;       // 待付款
    private Long toDeliver;   // 待发货
    private Long toReceive;   // 待收货
    private Long completed;   // 已完成
}
