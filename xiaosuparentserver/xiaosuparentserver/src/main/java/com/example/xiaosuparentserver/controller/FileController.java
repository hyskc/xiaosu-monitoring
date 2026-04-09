package com.example.xiaosuparentserver.controller;

import com.example.xiaosuparentserver.dto.ApiResponse;
import com.example.xiaosuparentserver.dto.VideoFileDTO;
import com.example.xiaosuparentserver.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileService fileService;

    /**
     * 获取指定学生的视频文件列表
     * @param studentId 学生ID
     * @return 视频文件列表
     */
    @GetMapping("/videos/{studentId}")
    public ApiResponse<List<VideoFileDTO>> getStudentVideos(@PathVariable Integer studentId) {
        return fileService.getStudentVideos(studentId);
    }
    
    /**
     * 获取指定文件夹的内容（文件和子文件夹）
     * @param studentId 学生ID
     * @param folderPath 文件夹路径（相对于学生根目录）
     * @return 文件和文件夹列表
     */
    @GetMapping("/folder/{studentId}/**")
    public ApiResponse<List<VideoFileDTO>> getFolderContents(@PathVariable Integer studentId, HttpServletRequest request) {
        // 获取完整请求路径
        String requestPath = request.getRequestURI();
        // 提取文件夹路径部分
        String basePath = "/api/files/folder/" + studentId + "/";
        String folderPath = "";
        
        if (requestPath.length() > basePath.length()) {
            folderPath = requestPath.substring(basePath.length());
        }
        
        return fileService.getFolderContents(studentId, folderPath);
    }

    /**
     * 下载指定学生的视频文件
     * @param studentId 学生ID
     * @param fileName 文件名
     * @return 文件资源
     */
    @GetMapping("/download/{studentId}/**")
    public ResponseEntity<Resource> downloadFile(@PathVariable Integer studentId, HttpServletRequest request) {
        // 获取完整请求路径
        String requestPath = request.getRequestURI();
        // 提取文件路径部分
        String basePath = "/api/files/download/" + studentId + "/";
        String filePath = "";
        
        if (requestPath.length() > basePath.length()) {
            filePath = requestPath.substring(basePath.length());
        }
        
        Resource resource = fileService.loadFileAsResource(studentId, filePath);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        // 尝试确定文件的内容类型
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // 忽略异常，内容类型将保持为null
        }

        // 如果无法确定内容类型，则默认为二进制流
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
    
    /**
     * 流式传输视频文件，支持Range请求头，实现视频播放和进度条跳转
     * @param studentId 学生ID
     * @param request HTTP请求，用于获取Range头
     * @return 视频流响应
     */
    @GetMapping("/stream/{studentId}/**")
    public ResponseEntity<byte[]> streamVideo(@PathVariable Integer studentId, HttpServletRequest request) {
        try {
            // 获取完整请求路径
            String requestPath = request.getRequestURI();
            // 提取文件路径部分
            String basePath = "/api/files/stream/" + studentId + "/";
            String filePath = "";
            
            if (requestPath.length() > basePath.length()) {
                filePath = requestPath.substring(basePath.length());
            }
            
            // 获取文件资源
            File videoFile = fileService.getVideoFile(studentId, filePath);
            if (videoFile == null || !videoFile.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            // 获取文件大小
            long fileSize = videoFile.length();
            
            // 解析Range请求头
            String rangeHeader = request.getHeader("Range");
            long start = 0;
            long end = fileSize - 1;
            
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.substring("bytes=".length()).split("-");
                start = Long.parseLong(ranges[0]);
                
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                }
                
                // 确保范围有效
                if (start >= fileSize) {
                    start = 0;
                }
                
                if (end >= fileSize) {
                    end = fileSize - 1;
                }
            }
            
            // 计算内容长度
            long contentLength = end - start + 1;
            
            // 设置缓冲区大小，最大1MB
            int bufferSize = (int) Math.min(contentLength, 1024 * 1024);
            byte[] buffer = new byte[bufferSize];
            
            // 读取文件指定范围的数据
            RandomAccessFile randomAccessFile = new RandomAccessFile(videoFile, "r");
            randomAccessFile.seek(start);
            
            int bytesRead = randomAccessFile.read(buffer, 0, (int) Math.min(buffer.length, contentLength));
            randomAccessFile.close();
            
            // 如果没有读取到数据，返回404
            if (bytesRead <= 0) {
                return ResponseEntity.notFound().build();
            }
            
            // 如果读取的数据小于缓冲区大小，调整缓冲区
            if (bytesRead < buffer.length) {
                byte[] smallerBuffer = new byte[bytesRead];
                System.arraycopy(buffer, 0, smallerBuffer, 0, bytesRead);
                buffer = smallerBuffer;
            }
            
            // 确定内容类型
            String contentType = request.getServletContext().getMimeType(videoFile.getAbsolutePath());
            if (contentType == null) {
                // 根据文件扩展名判断
                String fileName = videoFile.getName();
                String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
                
                if (extension.equals("mp4")) {
                    contentType = "video/mp4";
                } else if (extension.equals("avi")) {
                    contentType = "video/x-msvideo";
                } else if (extension.equals("mov")) {
                    contentType = "video/quicktime";
                } else {
                    contentType = "application/octet-stream";
                }
            }
            
            // 构建响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", contentType);
            headers.add("Accept-Ranges", "bytes");
            headers.add("Content-Length", String.valueOf(buffer.length));
            
            // 如果是范围请求，返回206 Partial Content
            if (rangeHeader != null) {
                headers.add("Content-Range", "bytes " + start + "-" + (start + buffer.length - 1) + "/" + fileSize);
                return new ResponseEntity<>(buffer, headers, HttpStatus.PARTIAL_CONTENT);
            } else {
                // 否则返回200 OK
                return new ResponseEntity<>(buffer, headers, HttpStatus.OK);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}