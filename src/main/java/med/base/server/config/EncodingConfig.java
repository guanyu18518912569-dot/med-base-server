package med.base.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 编码配置类 - 确保所有请求和响应使用UTF-8编码
 */
@Configuration
public class EncodingConfig implements WebMvcConfigurer {

    /**
     * 配置字符编码过滤器
     */
    @Bean
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true); // 强制使用UTF-8，覆盖客户端指定的编码
        filter.setForceRequestEncoding(true); // 强制请求使用UTF-8
        filter.setForceResponseEncoding(true); // 强制响应使用UTF-8
        return filter;
    }

    /**
     * 配置HTTP消息转换器使用UTF-8编码
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 确保StringHttpMessageConverter使用UTF-8
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        converters.add(0, stringConverter);
    }
}



