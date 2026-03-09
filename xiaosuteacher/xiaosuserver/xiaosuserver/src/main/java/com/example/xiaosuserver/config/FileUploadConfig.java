package com.example.xiaosuserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import jakarta.servlet.MultipartConfigElement;
import java.io.File;
import org.springframework.util.unit.DataSize;

@Configuration
public class FileUploadConfig {

    /**
     * 配置文件上传解析器
     * 设置较大的文件上传限制，以支持视频文件
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        // 设置最大上传大小为1GB
        factory.setMaxFileSize(DataSize.ofBytes(1024 * 1024 * 1024L));
        factory.setMaxRequestSize(DataSize.ofBytes(1024 * 1024 * 1024L));
        // 设置临时文件目录
        String location = System.getProperty("user.dir") + "/temp";
        File tmpFile = new File(location);
        if (!tmpFile.exists()) {
            tmpFile.mkdirs();
        }
        factory.setLocation(location);
        return factory.createMultipartConfig();
    }
    
    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}