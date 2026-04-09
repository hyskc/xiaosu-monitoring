package com.example.xiaosuserver.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    // 文件存储的基础路径，使用相对路径和File.separator确保跨平台兼容性
    private static final String BASE_UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "file";

    /**
     * 检查文件上传状态
     * @param fileName 文件名
     * @param fileSize 文件总大小
     * @param userId 用户ID
     * @return 已上传的字节数和状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> checkUploadStatus(
            @RequestParam("fileName") String fileName,
            @RequestParam("fileSize") long fileSize,
            @RequestParam("userId") int userId) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 获取文件日期目录路径
        String fileDateDir = getFileDateDir(userId, fileName);
        File file = new File(fileDateDir, fileName);
        
        if (file.exists()) {
            response.put("uploaded", file.length());
            response.put("status", "in_progress");
        } else {
            // 检查是否在其他日期目录中存在该文件
            File userVideoDir = new File(getUserVideoDir(userId));
            if (userVideoDir.exists() && userVideoDir.isDirectory()) {
                for (File dateDir : userVideoDir.listFiles()) {
                    if (dateDir.isDirectory()) {
                        File existingFile = new File(dateDir, fileName);
                        if (existingFile.exists()) {
                            response.put("uploaded", existingFile.length());
                            response.put("status", "in_progress");
                            return ResponseEntity.ok(response);
                        }
                    }
                }
            }
            
            response.put("uploaded", 0);
            response.put("status", "not_found");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 上传文件块
     * @param fileName 文件名
     * @param chunkIndex 块索引
     * @param chunkTotal 总块数
     * @param startByte 开始字节位置
     * @param filePart 文件块数据
     * @param userId 用户ID
     * @return 上传响应
     */
    @PostMapping("/chunk")
    public ResponseEntity<Map<String, Object>> uploadChunk(
            @RequestParam("fileName") String fileName,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("chunkTotal") int chunkTotal,
            @RequestParam("startByte") long startByte,
            @RequestParam("filePart") MultipartFile filePart,
            @RequestParam("userId") int userId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取文件日期目录
            String fileDateDir = getFileDateDir(userId, fileName);
            
            // 确保目录存在
            File uploadDir = new File(fileDateDir);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            File file = new File(fileDateDir, fileName);
            
            // 使用RandomAccessFile进行断点续传
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
                // 定位到指定位置
                randomAccessFile.seek(startByte);
                
                // 写入数据
                byte[] bytes = filePart.getBytes();
                randomAccessFile.write(bytes);
                
                response.put("success", true);
                response.put("message", "Chunk " + chunkIndex + " uploaded successfully");
                response.put("bytesUploaded", startByte + bytes.length);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error uploading chunk: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 完成文件上传
     * @param fileName 文件名
     * @param fileSize 文件总大小
     * @param userId 用户ID
     * @return 完成响应
     */
    @PostMapping("/complete")
    public ResponseEntity<Map<String, Object>> completeUpload(
            @RequestParam("fileName") String fileName,
            @RequestParam("fileSize") long fileSize,
            @RequestParam("userId") int userId) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 获取文件日期目录
        String fileDateDir = getFileDateDir(userId, fileName);
        File file = new File(fileDateDir, fileName);
        
        if (file.exists() && file.length() == fileSize) {
            response.put("success", true);
            response.put("message", "File upload completed successfully");
            response.put("filePath", file.getAbsolutePath());
        } else {
            response.put("success", false);
            response.put("message", "File upload incomplete or file size mismatch");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取用户视频目录路径
     * @param userId 用户ID
     * @return 用户视频目录路径
     */
    private String getUserVideoDir(int userId) {
        // 构建用户ID对应的视频目录路径: BASE_UPLOAD_DIR/userId/video
        String userDir = BASE_UPLOAD_DIR + File.separator + userId;
        String videoDir = userDir + File.separator + "video";
        
        // 确保目录存在
        File dir = new File(videoDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        return videoDir;
    }
    
    /**
     * 获取文件日期目录路径
     * 如果文件已存在，返回其所在的日期目录
     * 如果文件不存在，返回当前日期的目录
     * 
     * @param userId 用户ID
     * @param fileName 文件名
     * @return 文件日期目录路径
     */
    private String getFileDateDir(int userId, String fileName) {
        // 获取用户视频基础目录
        String userVideoDir = getUserVideoDir(userId);
        
        // 检查文件是否已存在于某个日期目录中
        File baseDir = new File(userVideoDir);
        if (baseDir.exists() && baseDir.isDirectory()) {
            for (File dateDir : baseDir.listFiles()) {
                if (dateDir.isDirectory()) {
                    File existingFile = new File(dateDir, fileName);
                    if (existingFile.exists()) {
                        return dateDir.getAbsolutePath();
                    }
                }
            }
        }
        
        // 文件不存在，创建当前日期目录
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(new Date());
        String dateDirPath = userVideoDir + File.separator + today;
        
        // 确保日期目录存在
        File dateDir = new File(dateDirPath);
        if (!dateDir.exists()) {
            dateDir.mkdirs();
        }
        
        return dateDirPath;
    }
    
    /**
     * 验证文件完整性
     * @param fileName 文件名
     * @param fileSize 文件总大小
     * @param userId 用户ID
     * @return 文件完整性验证结果
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyFileIntegrity(
            @RequestParam("fileName") String fileName,
            @RequestParam("fileSize") long fileSize,
            @RequestParam("userId") int userId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取文件日期目录
            String fileDateDir = getFileDateDir(userId, fileName);
            File file = new File(fileDateDir, fileName);
            
            boolean fileExists = file.exists();
            boolean sizeMatches = fileExists && file.length() == fileSize;
            boolean isReadable = fileExists && file.canRead();
            
            // 尝试打开文件进行读取测试，验证文件是否损坏
            boolean isIntact = false;
            if (fileExists && sizeMatches && isReadable) {
                try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                    // 尝试读取文件的开头、中间和结尾部分
                    long fileLength = file.length();
                    
                    // 读取文件开头
                    byte[] buffer = new byte[1024];
                    raf.seek(0);
                    int bytesRead = raf.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        // 读取文件中间
                        if (fileLength > 2048) {
                            raf.seek(fileLength / 2);
                            bytesRead = raf.read(buffer, 0, buffer.length);
                            if (bytesRead > 0) {
                                // 读取文件结尾
                                long endPos = Math.max(0, fileLength - 1024);
                                raf.seek(endPos);
                                bytesRead = raf.read(buffer, 0, (int)Math.min(1024, fileLength - endPos));
                                if (bytesRead > 0) {
                                    isIntact = true;
                                }
                            }
                        } else {
                            // 小文件，只需验证开头
                            isIntact = true;
                        }
                    }
                } catch (IOException e) {
                    // 文件读取失败，可能已损坏
                    isIntact = false;
                }
            }
            
            response.put("exists", fileExists);
            response.put("sizeMatches", sizeMatches);
            response.put("isReadable", isReadable);
            response.put("intact", isIntact);
            
            if (isIntact) {
                response.put("message", "File integrity verified successfully");
            } else {
                response.put("message", "File integrity check failed");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("intact", false);
            response.put("message", "Error verifying file integrity: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除损坏的文件
     * @param fileName 文件名
     * @param userId 用户ID
     * @return 删除结果
     */
    @PostMapping("/delete-corrupted")
    public ResponseEntity<Map<String, Object>> deleteCorruptedFile(
            @RequestParam("fileName") String fileName,
            @RequestParam("userId") int userId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取用户视频基础目录
            String userVideoDir = getUserVideoDir(userId);
            boolean deleted = false;
            
            // 检查所有日期目录中是否存在该文件
            File baseDir = new File(userVideoDir);
            if (baseDir.exists() && baseDir.isDirectory()) {
                for (File dateDir : baseDir.listFiles()) {
                    if (dateDir.isDirectory()) {
                        File fileToDelete = new File(dateDir, fileName);
                        if (fileToDelete.exists()) {
                            deleted = fileToDelete.delete();
                            if (deleted) {
                                break;
                            }
                        }
                    }
                }
            }
            
            if (deleted) {
                response.put("success", true);
                response.put("message", "Corrupted file deleted successfully");
            } else {
                response.put("success", false);
                response.put("message", "Failed to delete corrupted file or file not found");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting corrupted file: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}