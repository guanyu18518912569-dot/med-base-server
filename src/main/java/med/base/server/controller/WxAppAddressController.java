package med.base.server.controller;

import lombok.Data;
import med.base.server.common.DefaultResponse;
import med.base.server.model.UmsAddress;
import med.base.server.service.UmsAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 微信小程序 - 收货地址管理接口
 */
@RestController
@RequestMapping("/wxapp/address")
public class WxAppAddressController {

    @Autowired
    private UmsAddressService addressService;

    /**
     * 获取用户地址列表
     */
    @GetMapping("/list")
    public String getAddressList(@RequestParam String userId) {
        List<UmsAddress> addresses = addressService.getUserAddressList(userId);
        List<AddressVO> voList = addresses.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return DefaultResponse.success(voList);
    }

    /**
     * 获取默认地址
     */
    @GetMapping("/default")
    public String getDefaultAddress(@RequestParam String userId) {
        UmsAddress address = addressService.getDefaultAddress(userId);
        if (address == null) {
            return DefaultResponse.success(null);

        }
        return DefaultResponse.success(toVO(address));
    }

    /**
     * 获取地址详情
     */
    @GetMapping("/detail/{addressId}")
    public String getAddressDetail(@PathVariable String addressId) {
        UmsAddress address = addressService.getById(addressId);
        if (address == null) {
            return DefaultResponse.error("地址不存在");
        }
        return DefaultResponse.success(toVO(address));
    }

    /**
     * 添加收货地址
     */
    @PostMapping("/add")
    public String addAddress(@RequestBody AddressRequest request) {
        UmsAddress address = toEntity(request);
        address = addressService.addAddress(address);
        return DefaultResponse.success(toVO(address));
    }

    /**
     * 更新收货地址
     */
    @PostMapping("/update")
    public String updateAddress(@RequestBody AddressRequest request) {
        if (request.getAddressId() == null || request.getAddressId().isEmpty()) {
            return DefaultResponse.error("地址ID不能为空");
        }
        UmsAddress address = toEntity(request);
        address.setAddressId(request.getAddressId());
        address = addressService.updateAddress(address);
        return DefaultResponse.success(toVO(address));
    }

    /**
     * 删除收货地址
     */
    @PostMapping("/delete/{addressId}")
    public String deleteAddress(
            @PathVariable String addressId,
            @RequestParam String userId) {
        boolean result = addressService.deleteAddress(addressId, userId);
        if (result) {
            return DefaultResponse.success(true);
        }
        return DefaultResponse.error("删除失败");
    }

    /**
     * 设置默认地址
     */
    @PostMapping("/setDefault/{addressId}")
    public String setDefaultAddress(
            @PathVariable String addressId,
            @RequestParam String userId) {
        boolean result = addressService.setDefaultAddress(addressId, userId);
        if (result) {
            return DefaultResponse.success(true);
        }
        return DefaultResponse.error("设置失败");
    }

    /**
     * 转换为VO
     */
    private AddressVO toVO(UmsAddress address) {
        AddressVO vo = new AddressVO();
        vo.setAddressId(address.getAddressId());
        vo.setId(address.getAddressId()); // 兼容前端
        vo.setUserId(address.getUserId());
        vo.setName(address.getReceiverName());
        vo.setPhone(address.getReceiverPhone());
        vo.setPhoneNumber(address.getReceiverPhone()); // 兼容前端
        vo.setProvinceCode(address.getProvinceCode());
        vo.setProvinceName(address.getProvinceName());
        vo.setCityCode(address.getCityCode());
        vo.setCityName(address.getCityName());
        vo.setDistrictCode(address.getDistrictCode());
        vo.setDistrictName(address.getDistrictName());
        vo.setDetailAddress(address.getDetailAddress());
        vo.setAddress(address.getFullAddress()); // 兼容前端
        vo.setFullAddress(address.getFullAddress());
        vo.setTag(address.getTag());
        vo.setAddressTag(address.getTag()); // 兼容前端
        vo.setIsDefault(Boolean.TRUE.equals(address.getIsDefault()) ? 1 : 0);
        vo.setLatitude(address.getLatitude());
        vo.setLongitude(address.getLongitude());
        return vo;
    }

    /**
     * 转换为实体
     */
    private UmsAddress toEntity(AddressRequest request) {
        return UmsAddress.builder()
                .userId(request.getUserId())
                .receiverName(request.getName())
                .receiverPhone(request.getPhone() != null ? request.getPhone() : request.getPhoneNumber())
                .provinceCode(request.getProvinceCode())
                .provinceName(request.getProvinceName())
                .cityCode(request.getCityCode())
                .cityName(request.getCityName())
                .districtCode(request.getDistrictCode())
                .districtName(request.getDistrictName())
                .detailAddress(request.getDetailAddress())
                .tag(request.getTag() != null ? request.getTag() : request.getAddressTag())
                .isDefault(request.getIsDefault() != null && request.getIsDefault() == 1)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
    }

    /**
     * 地址请求对象
     */
    @Data
    public static class AddressRequest {
        private String addressId;
        private String userId;
        private String name;
        private String phone;
        private String phoneNumber; // 兼容前端
        private String provinceCode;
        private String provinceName;
        private String cityCode;
        private String cityName;
        private String districtCode;
        private String districtName;
        private String detailAddress;
        private String tag;
        private String addressTag; // 兼容前端
        private Integer isDefault;
        private BigDecimal latitude;
        private BigDecimal longitude;
    }

    /**
     * 地址返回对象
     */
    @Data
    public static class AddressVO {
        private String addressId;
        private String id; // 兼容前端
        private String userId;
        private String name;
        private String phone;
        private String phoneNumber; // 兼容前端
        private String provinceCode;
        private String provinceName;
        private String cityCode;
        private String cityName;
        private String districtCode;
        private String districtName;
        private String detailAddress;
        private String address; // 兼容前端（完整地址）
        private String fullAddress;
        private String tag;
        private String addressTag; // 兼容前端
        private Integer isDefault;
        private BigDecimal latitude;
        private BigDecimal longitude;
    }
}
