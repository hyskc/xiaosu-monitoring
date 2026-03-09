package com.example.xiaosuserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.File;

@SpringBootApplication
@EnableConfigurationProperties
public class XiaosuserverApplication {

	public static void main(String[] args) {
		// 确保文件上传目录存在，使用相对路径和File.separator确保跨平台兼容性
		String uploadDir = System.getProperty("user.dir") + File.separator + "file";
		File directory = new File(uploadDir);
		if (!directory.exists()) {
			boolean created = directory.mkdirs();
			if (created) {
				System.out.println("Created upload directory: " + uploadDir);
			} else {
				System.err.println("Failed to create upload directory: " + uploadDir);
			}
		}
		
		SpringApplication.run(XiaosuserverApplication.class, args);
	}

}
