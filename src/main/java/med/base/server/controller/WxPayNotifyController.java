package med.base.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import med.base.server.annotation.PassToken;
import med.base.server.service.OmsOrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付回调接口
 * 路径：/api/wx/pay/notify
 */
@Slf4j
@RestController
@RequestMapping("/api/wx/pay")
public class WxPayNotifyController {

    private final OmsOrderService orderService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${wxpay.api-v3-key:}")
    private String apiV3Key;

    public WxPayNotifyController(OmsOrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 微信支付异步通知
     * 微信支付成功后会调用此接口
     */
    @PassToken
    @PostMapping("/notify")
    public Map<String, String> payNotify(@RequestBody String requestBody,
                                         @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
                                         @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
                                         @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
                                         @RequestHeader(value = "Wechatpay-Serial", required = false) String serial) {
        log.info("收到微信支付回调通知");
        log.info("回调内容: {}", requestBody);

        Map<String, String> response = new HashMap<>();

        try {
            // 解析回调数据
            JsonNode rootNode = objectMapper.readTree(requestBody);
            String eventType = rootNode.path("event_type").asText();

            if (!"TRANSACTION.SUCCESS".equals(eventType)) {
                log.info("非支付成功通知，事件类型: {}", eventType);
                response.put("code", "SUCCESS");
                response.put("message", "OK");
                return response;
            }

            // 解密通知数据
            JsonNode resource = rootNode.path("resource");
            String ciphertext = resource.path("ciphertext").asText();
            String associatedData = resource.path("associated_data").asText();
            String nonceStr = resource.path("nonce").asText();

            // 解密获取支付结果
            String decryptedData = decryptToString(ciphertext, associatedData, nonceStr);
            log.info("解密后的支付数据: {}", decryptedData);

            JsonNode paymentData = objectMapper.readTree(decryptedData);
            String outTradeNo = paymentData.path("out_trade_no").asText();
            String transactionId = paymentData.path("transaction_id").asText();
            String tradeState = paymentData.path("trade_state").asText();

            if (!"SUCCESS".equals(tradeState)) {
                log.info("支付未成功，状态: {}", tradeState);
                response.put("code", "SUCCESS");
                response.put("message", "OK");
                return response;
            }

            // 处理订单号，支持重新支付时生成的带后缀订单号
            // 格式可能是：原订单号 或 原订单号_时间戳后缀
            String orderNo = outTradeNo;
            if (outTradeNo.contains("_")) {
                orderNo = outTradeNo.split("_")[0];
            }

            log.info("支付成功，订单号: {}, 微信交易号: {}", orderNo, transactionId);

            // 更新订单状态为已支付
            boolean success = orderService.payOrderByOrderNo(orderNo, transactionId);
            if (success) {
                log.info("订单状态更新成功，订单号: {}", orderNo);
            } else {
                log.warn("订单状态更新失败或订单已处理，订单号: {}", orderNo);
            }

            // 返回成功响应
            response.put("code", "SUCCESS");
            response.put("message", "OK");
            return response;

        } catch (Exception e) {
            log.error("处理微信支付回调异常", e);
            response.put("code", "FAIL");
            response.put("message", "处理失败：" + e.getMessage());
            return response;
        }
    }

    /**
     * 解密微信支付通知数据
     */
    private String decryptToString(String ciphertext, String associatedData, String nonce) throws Exception {
        if (apiV3Key == null || apiV3Key.isEmpty()) {
            throw new RuntimeException("API V3 Key未配置");
        }

        log.info("ciphertext，订单号: {}", ciphertext);
        log.info("associatedData，订单号: {}", associatedData);
        log.info("nonce，订单号: {}", nonce);

        byte[] key = apiV3Key.getBytes(StandardCharsets.UTF_8);
        byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);
        byte[] associatedDataBytes = associatedData.getBytes(StandardCharsets.UTF_8);
        byte[] nonceBytes = nonce.getBytes(StandardCharsets.UTF_8);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, nonceBytes);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
        cipher.updateAAD(associatedDataBytes);

        byte[] decryptedBytes = cipher.doFinal(ciphertextBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
