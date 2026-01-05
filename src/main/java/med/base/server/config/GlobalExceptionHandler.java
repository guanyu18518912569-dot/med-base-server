package med.base.server.config;

import lombok.extern.slf4j.Slf4j;
import med.base.server.common.DefaultResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

/**
 * \u5168\u5c40\u5f02\u5e38\u5904\u7406\u5668
 * \u7edf\u4e00\u5904\u7406\u5404\u79cd\u5f02\u5e38\uff0c\u8fd4\u56de\u53cb\u597d\u7684\u9519\u8bef\u4fe1\u606f\u7ed9\u524d\u7aef
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * \u5904\u7406\u6570\u636e\u5e93\u76f8\u5173\u5f02\u5e38
     */
    @ExceptionHandler({SQLException.class, DataAccessException.class})
    public String handleDatabaseException(Exception e) {
        log.error("\u6570\u636e\u5e93\u64cd\u4f5c\u5f02\u5e38", e);

        // \u5224\u65ad\u662f\u5426\u662f\u5b57\u6bb5\u4e0d\u5b58\u5728\u9519\u8bef
        if (e.getMessage() != null && e.getMessage().contains("Unknown column")) {
            return DefaultResponse.error("\u6570\u636e\u5e93\u7ed3\u6784\u672a\u66f4\u65b0\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458\u6267\u884c\u6570\u636e\u5e93\u8fc1\u79fb\u811a\u672c");
        }

        return DefaultResponse.error("\u6570\u636e\u5e93\u64cd\u4f5c\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5");
    }

    /**
     * \u5904\u7406\u7a7a\u6307\u9488\u5f02\u5e38
     */
    @ExceptionHandler(NullPointerException.class)
    public String handleNullPointerException(NullPointerException e) {
        log.error("\u7a7a\u6307\u9488\u5f02\u5e38", e);
        return DefaultResponse.error("\u7cfb\u7edf\u5f02\u5e38\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458");
    }

    /**
     * \u5904\u7406\u975e\u6cd5\u53c2\u6570\u5f02\u5e38
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("\u53c2\u6570\u9519\u8bef", e);
        return DefaultResponse.error(e.getMessage() != null ? e.getMessage() : "\u8bf7\u6c42\u53c2\u6570\u9519\u8bef");
    }

    /**
     * \u5904\u7406\u6240\u6709\u5176\u4ed6\u5f02\u5e38
     */
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e) {
        log.error("\u7cfb\u7edf\u5f02\u5e38", e);

        // \u5f00\u53d1\u73af\u5883\u8fd4\u56de\u8be6\u7ec6\u9519\u8bef\u4fe1\u606f\uff0c\u751f\u4ea7\u73af\u5883\u53ea\u8fd4\u56de\u901a\u7528\u9519\u8bef
        String message = e.getMessage();
        if (message != null && message.length() > 200) {
            message = message.substring(0, 200) + "...";
        }

        return DefaultResponse.error("\u7cfb\u7edf\u5f02\u5e38\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5");
    }
}
