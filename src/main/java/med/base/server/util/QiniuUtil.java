package med.base.server.util;

import com.alibaba.fastjson.JSON;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 七牛云上传工具类
 *
 * 所有需要上传文件到七牛云的地方，都可以注入该工具类并调用 upload 方法。
 *
 * 配置项在 application-*.yml 中的 qiniu 节点：
 * qiniu:
 *   access-key: xxx
 *   secret-key: xxx
 *   bucket: swzx-samll-mall
 *   domain: static.siweizx.com
 */
@Slf4j
@Component
public class QiniuUtil {

    @Value("${qiniu.access-key}")
    private String accessKey;

    @Value("${qiniu.secret-key}")
    private String secretKey;

    /**
     * 空间名称：swzx-samll-mall
     */
    @Value("${qiniu.bucket:swzx-samll-mall}")
    private String bucket;

    /**
     * 访问域名前缀：static.siweizx.com
     */
    @Value("${qiniu.domain:static.siweizx.com}")
    private String domain;

    private UploadManager uploadManager() {
        Configuration cfg = new Configuration(Region.autoRegion());
        return new UploadManager(cfg);
    }

    private Auth auth() {
        return Auth.create(accessKey, secretKey);
    }

    /**
     * 上传 MultipartFile，自动生成文件名，返回完整可访问 URL。
     *
     * @param file      要上传的文件
     * @param dirPrefix 目录前缀（例如 "image/brand"），可以为 null 或空
     * @return 文件的完整访问 URL，例如：https://static.siweizx.com/path/xxx.jpg
     */
    public String upload(MultipartFile file, String dirPrefix) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(originalFilename);

        // 生成 key：目录/年月日/随机串.后缀
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String random = UUID.randomUUID().toString().replaceAll("-", "");
        StringBuilder keyBuilder = new StringBuilder();
        if (StringUtils.hasText(dirPrefix)) {
            keyBuilder.append(dirPrefix).append("/");
        }
        keyBuilder.append(datePath).append("/").append(random);
        if (StringUtils.hasText(ext)) {
            keyBuilder.append(".").append(ext);
        }
        String key = keyBuilder.toString();

        try {
            return upload(file.getBytes(), key);
        } catch (IOException e) {
            log.error("读取上传文件字节失败", e);
            throw new RuntimeException("上传失败：读取文件失败", e);
        }
    }

    /**
     * 直接上传字节数组，指定 key，返回完整 URL。
     *
     * @param data 文件字节
     * @param key  存储到七牛云的 key（路径+文件名）
     * @return 文件的完整访问 URL
     */
    public String upload(byte[] data, String key) {
        UploadManager manager = uploadManager();
        String upToken = auth().uploadToken(bucket);

        try {
            Response response = manager.put(data, key, upToken);
            if (!response.isOK()) {
                log.error("七牛上传失败，code={} body={}", response.statusCode, response.bodyString());
                throw new RuntimeException("上传失败：" + response.bodyString());
            }

            // 使用 fastjson 解析返回结果，避免依赖 Gson
            DefaultPutRet putRet = JSON.parseObject(response.bodyString(), DefaultPutRet.class);
            String finalKey = (putRet != null && StringUtils.hasText(putRet.key)) ? putRet.key : key;

            // 组装访问 URL
            String prefix = domain.startsWith("http") ? domain : "https://" + domain;
            if (!prefix.endsWith("/")) {
                prefix = prefix + "/";
            }
            return prefix + finalKey;
        } catch (QiniuException ex) {
            Response r = ex.response;
            log.error("七牛上传异常，code={} body={}", r.statusCode, r.toString(), ex);
            throw new RuntimeException("上传失败：" + r.toString(), ex);
        }
    }
}


