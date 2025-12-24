package med.base.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import med.base.server.annotation.PassToken;
import med.base.server.annotation.UserLoginToken;
import med.base.server.util.QiniuUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用文件上传接口
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class UploadController {

    private final QiniuUtil qiniuUtil;

    /**
     * 通用文件上传接口
     * 
     * @param file 上传的文件
     * @param dir 目录前缀（可选），例如 "image/category"
     * @return 上传结果，包含文件访问URL
     */
    @PostMapping("/upload")
    @UserLoginToken
    public Map<String, Object> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dir", required = false, defaultValue = "image/common") String dir) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (file == null || file.isEmpty()) {
                result.put("status", false);
                result.put("statusText", "上传文件不能为空");
                return result;
            }

            

            String url = qiniuUtil.upload(file, dir);
            log.info("文件上传成功: {}", url);

            result.put("status", true);
            result.put("data", url);
            result.put("statusText", "上传成功");
        } catch (Exception e) {
            log.error("文件上传失败", e);
            result.put("status", false);
            result.put("statusText", "上传失败: " + e.getMessage());
        }
        return result;
    }
}
