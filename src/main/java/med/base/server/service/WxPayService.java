package med.base.server.service;

import com.alibaba.fastjson.JSON;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.util.PemUtil;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 微信支付服务
 */
@Slf4j
@Service
public class WxPayService {

    @Value("${wxpay.appid}")
    private String appId;

    @Value("${wxpay.mch-id}")
    private String mchId;

    @Value("${wxpay.private-key-path}")
    private String privateKeyPath;

    @Value("${wxpay.merchant-serial-number}")
    private String merchantSerialNumber;

    @Value("${wxpay.notify-url}")
    private String notifyUrl;

    @Value("${wxpay.api-v3-key:}")
    private String apiV3Key;

    @Value("${wxpay.wechat-pay-public-key-path:}")
    private String wechatPayPublicKeyPath;

    @Value("${wxpay.wechat-pay-public-key-id:}")
    private String wechatPayPublicKeyId;

    private JsapiService jsapiService;
    private Config config;
    private PrivateKey privateKey;

    /**
     * 初始化微信支付配置
     */
    private void initConfig() {
        if (config == null) {
            try {
                // 使用微信支付公钥模式（2024年新要求）
                config = new RSAPublicKeyConfig.Builder()
                        .merchantId(mchId)
                        .privateKeyFromPath(privateKeyPath)
                        .merchantSerialNumber(merchantSerialNumber)
                        .publicKeyFromPath(wechatPayPublicKeyPath)
                        .publicKeyId(wechatPayPublicKeyId)
                        .build();
                jsapiService = new JsapiService.Builder().config(config).build();

                // 加载私钥用于签名
                privateKey = PemUtil.loadPrivateKeyFromPath(privateKeyPath);

                log.info("微信支付配置初始化成功");
            } catch (Exception e) {
                log.error("微信支付配置初始化失败", e);
                throw new RuntimeException("微信支付配置初始化失败：" + e.getMessage());
            }
        }
    }

    /**
     * JSAPI统一下单
     */
    public Map<String, String> createJsapiOrder(String orderNo, BigDecimal amount, String openId, String description) {
        try {
            initConfig();
            log.info("微信支付预下单, orderNo: {}, amount: {}, user: {}", orderNo, amount, openId);

            // 构建统一下单请求
            PrepayRequest request = new PrepayRequest();
            request.setAppid(appId);
            request.setMchid(mchId);
            request.setDescription(description);
            request.setOutTradeNo(orderNo);
            request.setNotifyUrl(notifyUrl);

            // 设置金额（单位：分）
            Amount amt = new Amount();
            amt.setTotal(amount.multiply(BigDecimal.valueOf(100)).intValue());
            amt.setCurrency("CNY");
            request.setAmount(amt);

            // 设置支付者
            Payer payer = new Payer();
            payer.setOpenid(openId);
            request.setPayer(payer);

            // 调用统一下单API
            PrepayResponse response = jsapiService.prepay(request);
            log.info("微信支付预下单成功, prepayId: {}", response.getPrepayId());
            log.info("主要看通知url, request: {}", JSON.toJSONString(request));

            // 构建前端调用支付所需参数
            String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
            String nonceStr = generateNonceStr();
            String packageStr = "prepay_id=" + response.getPrepayId();

            Map<String, String> payParams = new HashMap<>();
            payParams.put("appId", appId);
            payParams.put("timeStamp", timeStamp);
            payParams.put("nonceStr", nonceStr);
            payParams.put("package", packageStr);
            payParams.put("signType", "RSA");

            // 生成支付签名
            String paySign = generatePaySign(appId, timeStamp, nonceStr, packageStr);
            payParams.put("paySign", paySign);

            return payParams;
        } catch (Exception e) {
            log.error("微信支付预下单失败", e);
            throw new RuntimeException("创建微信支付订单失败：" + e.getMessage());
        }
    }

    /**
     * 生成支付签名
     */
    private String generatePaySign(String appId, String timeStamp, String nonceStr, String packageStr) {
        try {
            // 构造签名串
            String signMessage = appId + "\n" + timeStamp + "\n" + nonceStr + "\n" + packageStr + "\n";

            // 使用SHA256withRSA签名
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(signMessage.getBytes(StandardCharsets.UTF_8));
            byte[] signBytes = signature.sign();

            return Base64.getEncoder().encodeToString(signBytes);
        } catch (Exception e) {
            log.error("生成支付签名失败", e);
            throw new RuntimeException("生成支付签名失败：" + e.getMessage());
        }
    }

    /**
     * 生成随机字符串
     */
    private String generateNonceStr() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 获取AppId
     */
    public String getAppId() {
        return appId;
    }

    /**
     * 获取商户号
     */
    public String getMchId() {
        return mchId;
    }
}
