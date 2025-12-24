package med.base.server.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import med.base.server.mapper.UmsAddressMapper;
import med.base.server.model.UmsAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 收货地址服务
 */
@Service
public class UmsAddressService extends ServiceImpl<UmsAddressMapper, UmsAddress> {

    /**
     * 获取用户地址列表
     */
    public List<UmsAddress> getUserAddressList(String userId) {
        return baseMapper.selectByUserId(userId);
    }

    /**
     * 获取用户默认地址
     */
    public UmsAddress getDefaultAddress(String userId) {
        return baseMapper.selectDefaultByUserId(userId);
    }

    /**
     * 添加收货地址
     */
    @Transactional
    public UmsAddress addAddress(UmsAddress address) {
        // 生成地址ID
        address.setAddressId(UUID.randomUUID().toString().replace("-", ""));
        
        // 生成完整地址
        address.setFullAddress(buildFullAddress(address));
        
        // 设置时间
        LocalDateTime now = LocalDateTime.now();
        address.setCreatedTime(now);
        address.setUpdatedTime(now);
        
        // 如果是默认地址，先清除其他默认地址
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            baseMapper.clearDefaultByUserId(address.getUserId());
        }
        
        // 如果是用户第一个地址，自动设为默认
        List<UmsAddress> existingAddresses = baseMapper.selectByUserId(address.getUserId());
        if (existingAddresses == null || existingAddresses.isEmpty()) {
            address.setIsDefault(true);
        }
        
        baseMapper.insert(address);
        return address;
    }

    /**
     * 更新收货地址
     */
    @Transactional
    public UmsAddress updateAddress(UmsAddress address) {
        // 生成完整地址
        address.setFullAddress(buildFullAddress(address));
        
        // 设置更新时间
        address.setUpdatedTime(LocalDateTime.now());
        
        // 如果设为默认地址，先清除其他默认地址
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            baseMapper.clearDefaultByUserId(address.getUserId());
        }
        
        baseMapper.updateById(address);
        return address;
    }

    /**
     * 删除收货地址
     */
    @Transactional
    public boolean deleteAddress(String addressId, String userId) {
        UmsAddress address = baseMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            return false;
        }
        
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        baseMapper.deleteById(addressId);
        
        // 如果删除的是默认地址，将第一个地址设为默认
        if (wasDefault) {
            List<UmsAddress> remaining = baseMapper.selectByUserId(userId);
            if (remaining != null && !remaining.isEmpty()) {
                baseMapper.setDefault(remaining.get(0).getAddressId());
            }
        }
        
        return true;
    }

    /**
     * 设置默认地址
     */
    @Transactional
    public boolean setDefaultAddress(String addressId, String userId) {
        UmsAddress address = baseMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            return false;
        }
        
        baseMapper.clearDefaultByUserId(userId);
        baseMapper.setDefault(addressId);
        return true;
    }

    /**
     * 构建完整地址
     */
    private String buildFullAddress(UmsAddress address) {
        StringBuilder sb = new StringBuilder();
        if (address.getProvinceName() != null) {
            sb.append(address.getProvinceName());
        }
        if (address.getCityName() != null) {
            sb.append(address.getCityName());
        }
        if (address.getDistrictName() != null) {
            sb.append(address.getDistrictName());
        }
        if (address.getDetailAddress() != null) {
            sb.append(address.getDetailAddress());
        }
        return sb.toString();
    }
}
