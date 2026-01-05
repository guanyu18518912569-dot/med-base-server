package med.base.server.util;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * 微信支付证书工具类
 * 用于获取商户证书序列号
 */
@Slf4j
public class WxPayCertUtil {

    /**
     * 从证书文件中获取商户序列号
     * @param certPath 证书文件路径
     * @return 商户序列号（十六进制字符串）
     */
    public static String getMerchantSerialNumber(String certPath) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            FileInputStream in = new FileInputStream(certPath);
            X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
            in.close();

            // 获取证书序列号
            String serialNumber = cert.getSerialNumber().toString(16).toUpperCase();
            log.info("商户证书序列号：{}", serialNumber);
            return serialNumber;
        } catch (Exception e) {
            log.error("获取商户序列号失败", e);
            throw new RuntimeException("获取商户序列号失败：" + e.getMessage());
        }
    }

    /**
     * 验证证书是否有效
     * @param certPath 证书文件路径
     * @return 是否有效
     */
    public static boolean validateCertificate(String certPath) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            FileInputStream in = new FileInputStream(certPath);
            X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
            in.close();

            // 检查证书是否过期
            cert.checkValidity();
            log.info("证书有效，主题：{}", cert.getSubjectDN());
            return true;
        } catch (Exception e) {
            log.error("证书验证失败", e);
            return false;
        }
    }

    // 测试方法
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("用法：java WxPayCertUtil <证书文件路径>");
            System.out.println("例如：java WxPayCertUtil /path/to/apiclient_cert.pem");
            return;
        }

        String certPath = args[0];
        System.out.println("证书文件路径：" + certPath);

        try {
            boolean valid = validateCertificate(certPath);
            if (valid) {
                String serialNumber = getMerchantSerialNumber(certPath);
                System.out.println("商户序列号：" + serialNumber);
                System.out.println("请将此序列号填入配置文件中的 merchant-serial-number 字段");
            } else {
                System.out.println("证书无效，请检查证书文件");
            }
        } catch (Exception e) {
            System.out.println("获取序列号失败：" + e.getMessage());
        }
    }
}
