package med.base.server.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import med.base.server.common.DefaultResponse;
import med.base.server.model.OmsCart;
import med.base.server.service.OmsCartService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 微信小程序购物车接口
 * 添加购物车
 */
@RestController
@RequestMapping("/wxapp/cart")
@RequiredArgsConstructor
public class WxAppCartController {

    private final OmsCartService cartService;

    /**
     * 添加商品到购物车
     */
    @PostMapping("/add")
    public String addToCart(@RequestBody CartAddRequest request) {
        try {
            // 参数验证
            if (!StringUtils.hasText(request.getUserId())) {
                return DefaultResponse.error("用户ID不能为空");
            }
            if (!StringUtils.hasText(request.getSkuId())) {
                return DefaultResponse.error("SKU ID不能为空");
            }
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                request.setQuantity(1);
            }
            if (request.getPrice() == null) {
                request.setPrice(0L);
            }

            OmsCart cart = OmsCart.builder()
                    .userId(request.getUserId())
                    .storeId(request.getStoreId() != null ? request.getStoreId() : "1")
                    .spuId(request.getSpuId())
                    .skuId(request.getSkuId())
                    .goodsName(request.getGoodsName())
                    .goodsImage(request.getGoodsImage())
                    .specs(request.getSpecs())
                    .price(request.getPrice())
                    .quantity(request.getQuantity())
                    .build();

            OmsCart result = cartService.addToCart(cart);
            
            // 返回购物车商品数量
            int cartCount = cartService.countCartItems(request.getUserId());
            
            Map<String, Object> data = new HashMap<>();
            data.put("cartId", result.getCartId());
            data.put("cartCount", cartCount);
            
            return DefaultResponse.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("添加购物车失败：" + e.getMessage());
        }
    }

    /**
     * 获取购物车列表
     */
    @GetMapping("/list")
    public String getCartList(@RequestParam String userId) {
        try {
            List<OmsCart> cartList = cartService.getCartList(userId);
            
            // 按门店分组（这里简化处理，所有商品归为一个门店）
            List<CartItemVO> items = cartList.stream().map(cart -> {
                CartItemVO vo = new CartItemVO();
                vo.setCartId(cart.getCartId());
                vo.setSpuId(cart.getSpuId());
                vo.setSkuId(cart.getSkuId());
                vo.setGoodsName(cart.getGoodsName());
                vo.setGoodsImage(cart.getGoodsImage());
                vo.setSpecs(cart.getSpecs());
                vo.setPrice(cart.getPrice());
                vo.setQuantity(cart.getQuantity());
                vo.setIsSelected(cart.getIsSelected());
                return vo;
            }).collect(Collectors.toList());

            // 计算汇总信息
            long totalPrice = 0;
            int totalQuantity = 0;
            int selectedCount = 0;
            for (OmsCart cart : cartList) {
                totalQuantity += cart.getQuantity();
                if (cart.getIsSelected() == 1) {
                    totalPrice += cart.getPrice() * cart.getQuantity();
                    selectedCount += cart.getQuantity();
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("isNotEmpty", !cartList.isEmpty());
            data.put("list", items);
            data.put("totalPrice", totalPrice);
            data.put("totalQuantity", totalQuantity);
            data.put("selectedCount", selectedCount);
            data.put("isAllSelected", !cartList.isEmpty() && cartList.stream().allMatch(c -> c.getIsSelected() == 1));

            return DefaultResponse.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取购物车失败：" + e.getMessage());
        }
    }

    /**
     * 更新购物车商品数量
     */
    @PostMapping("/update-quantity")
    public String updateQuantity(@RequestBody CartUpdateQuantityRequest request) {
        try {
            if (request.getCartId() == null) {
                return DefaultResponse.error("购物车ID不能为空");
            }
            
            boolean success = cartService.updateQuantity(request.getCartId(), request.getQuantity());
            if (success) {
                return DefaultResponse.success();
            } else {
                return DefaultResponse.error("更新数量失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("更新数量失败：" + e.getMessage());
        }
    }

    /**
     * 更新选中状态
     */
    @PostMapping("/update-selected")
    public String updateSelected(@RequestBody CartUpdateSelectedRequest request) {
        try {
            if (request.getCartId() == null) {
                return DefaultResponse.error("购物车ID不能为空");
            }
            
            boolean success = cartService.updateSelected(request.getCartId(), request.getIsSelected());
            if (success) {
                return DefaultResponse.success();
            } else {
                return DefaultResponse.error("更新选中状态失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("更新选中状态失败：" + e.getMessage());
        }
    }

    /**
     * 全选/取消全选
     */
    @PostMapping("/select-all")
    public String selectAll(@RequestBody CartSelectAllRequest request) {
        try {
            if (!StringUtils.hasText(request.getUserId())) {
                return DefaultResponse.error("用户ID不能为空");
            }
            
            boolean success = cartService.updateAllSelected(request.getUserId(), request.getIsSelected());
            if (success) {
                return DefaultResponse.success();
            } else {
                return DefaultResponse.error("操作失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("操作失败：" + e.getMessage());
        }
    }

    /**
     * 删除购物车商品
     */
    @PostMapping("/delete")
    public String deleteCartItem(@RequestBody CartDeleteRequest request) {
        try {
            boolean success;
            if (request.getCartIds() != null && !request.getCartIds().isEmpty()) {
                success = cartService.deleteCartItems(request.getCartIds());
            } else if (request.getCartId() != null) {
                success = cartService.deleteCartItem(request.getCartId());
            } else {
                return DefaultResponse.error("请指定要删除的商品");
            }
            
            if (success) {
                return DefaultResponse.success();
            } else {
                return DefaultResponse.error("删除失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 清空购物车
     */
    @PostMapping("/clear")
    public String clearCart(@RequestBody CartClearRequest request) {
        try {
            if (!StringUtils.hasText(request.getUserId())) {
                return DefaultResponse.error("用户ID不能为空");
            }
            
            boolean success = cartService.clearCart(request.getUserId());
            if (success) {
                return DefaultResponse.success();
            } else {
                return DefaultResponse.error("清空购物车失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("清空购物车失败：" + e.getMessage());
        }
    }

    /**
     * 获取购物车商品数量
     */
    @GetMapping("/count")
    public String getCartCount(@RequestParam String userId) {
        try {
            int count = cartService.countCartItems(userId);
            return DefaultResponse.success(count);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取购物车数量失败：" + e.getMessage());
        }
    }
}

// ========== Request/Response DTOs ==========

@Data
class CartAddRequest {
    private String userId;
    private String storeId;
    private String spuId;
    private String skuId;
    private String goodsName;
    private String goodsImage;
    private String specs; // JSON 格式的规格信息
    private Long price;   // 单价（分）
    private Integer quantity;
}

@Data
class CartUpdateQuantityRequest {
    private Long cartId;
    private Integer quantity;
}

@Data
class CartUpdateSelectedRequest {
    private Long cartId;
    private Integer isSelected;
}

@Data
class CartSelectAllRequest {
    private String userId;
    private Integer isSelected;
}

@Data
class CartDeleteRequest {
    private Long cartId;
    private List<Long> cartIds;
}

@Data
class CartClearRequest {
    private String userId;
}

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
class CartItemVO {
    private Long cartId;
    private String spuId;
    private String skuId;
    private String goodsName;
    private String goodsImage;
    private String specs;
    private Long price;
    private Integer quantity;
    private Integer isSelected;
}
