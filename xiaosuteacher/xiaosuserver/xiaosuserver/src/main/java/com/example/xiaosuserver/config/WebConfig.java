package com.example.xiaosuserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将文件系统中的路径映射到URL路径，使用相对路径
        String fileLocation = "file:" + System.getProperty("user.dir") + File.separator + "file" + File.separator;
        registry.addResourceHandler("/**")
                .addResourceLocations(fileLocation);
    }
}