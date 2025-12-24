package med.base.server.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import med.base.server.mapper.OmsCartMapper;
import med.base.server.model.OmsCart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 购物车 Service
 */
@Service
@RequiredArgsConstructor
public class OmsCartService extends ServiceImpl<OmsCartMapper, OmsCart> {

    private final OmsCartMapper cartMapper;

    /**
     * 添加商品到购物车
     * 如果已存在相同 SKU，则累加数量
     */
    @Transactional(rollbackFor = Exception.class)
    public OmsCart addToCart(OmsCart cart) {
        // 检查是否已存在相同 SKU
        OmsCart existing = cartMapper.selectByUserIdAndSkuId(cart.getUserId(), cart.getSkuId());
        
        if (existing != null) {
            // 累加数量
            int newQuantity = existing.getQuantity() + cart.getQuantity();
            cartMapper.updateQuantity(existing.getCartId(), newQuantity);
            existing.setQuantity(newQuantity);
            return existing;
        } else {
            // 新增记录
            cart.setIsSelected(1); // 默认选中
            cart.setDeleted(0);
            cart.setCreatedTime(LocalDateTime.now());
            cart.setUpdatedTime(LocalDateTime.now());
            cartMapper.insert(cart);
            return cart;
        }
    }

    /**
     * 获取用户购物车列表
     */
    public List<OmsCart> getCartList(String userId) {
        return cartMapper.selectByUserId(userId);
    }

    /**
     * 更新购物车商品数量
     */
    public boolean updateQuantity(Long cartId, Integer quantity) {
        if (quantity <= 0) {
            return cartMapper.softDelete(cartId) > 0;
        }
        return cartMapper.updateQuantity(cartId, quantity) > 0;
    }

    /**
     * 更新选中状态
     */
    public boolean updateSelected(Long cartId, Integer isSelected) {
        return cartMapper.updateSelected(cartId, isSelected) > 0;
    }

    /**
     * 全选/取消全选
     */
    public boolean updateAllSelected(String userId, Integer isSelected) {
        return cartMapper.updateAllSelected(userId, isSelected) > 0;
    }

    /**
     * 删除购物车商品
     */
    public boolean deleteCartItem(Long cartId) {
        return cartMapper.softDelete(cartId) > 0;
    }

    /**
     * 批量删除购物车商品
     */
    public boolean deleteCartItems(List<Long> cartIds) {
        if (cartIds == null || cartIds.isEmpty()) {
            return false;
        }
        return cartMapper.softDeleteBatch(cartIds) > 0;
    }

    /**
     * 清空购物车
     */
    public boolean clearCart(String userId) {
        return cartMapper.clearByUserId(userId) > 0;
    }

    /**
     * 统计购物车商品数量
     */
    public int countCartItems(String userId) {
        return cartMapper.countByUserId(userId);
    }

    /**
     * 获取已选中的购物车商品
     */
    public List<OmsCart> getSelectedCartItems(String userId) {
        return cartMapper.selectSelectedByUserId(userId);
    }
}
