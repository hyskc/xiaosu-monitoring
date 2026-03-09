package com.example.xiaosuparentserver.service;

import com.example.xiaosuparentserver.dto.ApiResponse;
import com.example.xiaosuparentserver.dto.VideoFileDTO;
import com.example.xiaosuparentserver.entity.Student;
import com.example.xiaosuparentserver.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class FileService {

    @Autowired
    private StudentRepository studentRepository;

    private static final String BASE_VIDEO_PATH = "./file";

    /**
     * 获取指定学生的视频文件列表
     * @param studentId 学生ID
     * @return 视频文件列表
     */
    public ApiResponse<List<VideoFileDTO>> getStudentVideos(Integer studentId) {
        // 验证学生是否存在
        Student student = studentRepository.findById(studentId);
        if (student == null) {
            return ApiResponse.fail("学生不存在");
        }

        // 构建学生文件夹路径
        String studentFolderPath = BASE_VIDEO_PATH + File.separator + studentId;
        File studentFolder = new File(studentFolderPath);

        // 检查文件夹是否存在
        if (!studentFolder.exists() || !studentFolder.isDirectory()) {
            return ApiResponse.success(new ArrayList<>()); // 返回空列表
        }

        // 获取学生文件夹中的所有文件和文件夹
        return ApiResponse.success(getFilesAndFolders(studentFolderPath, studentId, ""));
    }
    
    /**
     * 获取指定文件夹内的文件和子文件夹
     * @param studentId 学生ID
     * @param folderPath 文件夹路径（相对于学生根目录）
     * @return 文件和文件夹列表
     */
    public ApiResponse<List<VideoFileDTO>> getFolderContents(Integer studentId, String folderPath) {
        // 验证学生是否存在
        Student student = studentRepository.findById(studentId);
        if (student == null) {
            return ApiResponse.fail("学生不存在");
        }
        
        // 构建完整的文件夹路径
        String fullFolderPath = BASE_VIDEO_PATH + File.separator + studentId;
        if (folderPath != null && !folderPath.isEmpty()) {
            fullFolderPath = fullFolderPath + File.separator + folderPath;
        }
        
        File folder = new File(fullFolderPath);
        
        // 检查文件夹是否存在
        if (!folder.exists() || !folder.isDirectory()) {
            return ApiResponse.fail("文件夹不存在");
        }
        
        // 获取文件夹中的所有文件和文件夹
        return ApiResponse.success(getFilesAndFolders(fullFolderPath, studentId, folderPath));
    }
    
    /**
     * 获取指定路径下的所有文件和文件夹
     * @param folderPath 文件夹完整路径
     * @param studentId 学生ID
     * @param relativePath 相对路径（用于构建API路径）
     * @return 文件和文件夹列表
     */
    private List<VideoFileDTO> getFilesAndFolders(String folderPath, Integer studentId, String relativePath) {
        List<VideoFileDTO> fileList = new ArrayList<>();
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        
        if (files != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (File file : files) {
                try {
                    String fileName = file.getName();
                    String lastModified = sdf.format(new Date(file.lastModified()));
                    String filePathPrefix = relativePath.isEmpty() ? fileName : relativePath + File.separator + fileName;
                    
                    if (file.isDirectory()) {
                        // 处理文件夹
                        String apiPath = "/api/files/folder/" + studentId + "/" + filePathPrefix;
                        VideoFileDTO folderDTO = new VideoFileDTO(
                                fileName,
                                apiPath,
                                0L, // 文件夹大小设为0
                                "folder",
                                lastModified,
                                true
                        );
                        fileList.add(folderDTO);
                    } else {
                        // 处理文件
                        Path path = Paths.get(file.getAbsolutePath());
                        String filePath = "/api/files/download/" + studentId + "/" + filePathPrefix;
                        Long fileSize = file.length();
                        String fileType = Files.probeContentType(path);
                        if (fileType == null) {
                            // 如果无法确定文件类型，根据扩展名判断
                            String extension = getFileExtension(fileName);
                            if (extension.equalsIgnoreCase("mp4") || 
                                extension.equalsIgnoreCase("avi") || 
                                extension.equalsIgnoreCase("mov")) {
                                fileType = "video/" + extension.toLowerCase();
                            } else {
                                fileType = "application/octet-stream";
                            }
                        }
                        
                        // 为视频文件添加流式传输URL
                        String streamPath = null;
                        if (fileType.startsWith("video/") || isVideoFile(fileName)) {
                            streamPath = "/api/files/stream/" + studentId + "/" + filePathPrefix;
                        }
                        
                        VideoFileDTO fileDTO = new VideoFileDTO(
                                fileName,
                                filePath,
                                fileSize,
                                fileType,
                                lastModified,
                                false
                        );
                        
                        // 如果是视频文件，设置流式传输URL
                        if (streamPath != null) {
                            fileDTO.setStreamUrl(streamPath);
                        }
                        
                        fileList.add(fileDTO);
                    }
                } catch (Exception e) {
                    // 忽略无法处理的文件
                }
            }
        }
        
        return fileList;
    }
    
    /**
     * 判断文件是否为视频文件（根据扩展名）
     * @param fileName 文件名
     * @return 是否为视频文件
     */
    private boolean isVideoFile(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        return extension.equals("mp4") || extension.equals("avi") || extension.equals("mov") || 
               extension.equals("mkv") || extension.equals("wmv") || extension.equals("flv");
    }
    
    /**
     * 获取文件扩展名
     * @param fileName 文件名
     * @return 扩展名
     */
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }
    
    /**
     * 加载文件作为资源
     * @param studentId 学生ID
     * @param filePath 文件路径（相对于学生根目录）
     * @return 文件资源
     */
    public Resource loadFileAsResource(Integer studentId, String filePath) {
        try {
            // 验证学生是否存在
            Student student = studentRepository.findById(studentId);
            if (student == null) {
                return null;
            }
            
            // 构建文件路径
            String fullFilePath = BASE_VIDEO_PATH + File.separator + studentId + File.separator + filePath;
            Path path = Paths.get(fullFilePath);
            Resource resource = new UrlResource(path.toUri());
            
            if (resource.exists()) {
                return resource;
            } else {
                return null;
            }
        } catch (MalformedURLException e) {
            return null;
        }
    }
    
    /**
     * 获取视频文件对象，用于流式传输
     * @param studentId 学生ID
     * @param filePath 文件路径（相对于学生根目录）
     * @return 视频文件对象
     */
    public File getVideoFile(Integer studentId, String filePath) {
        try {
            // 验证学生是否存在
            Student student = studentRepository.findById(studentId);
            if (student == null) {
                return null;
            }
            
            // 构建文件路径
            String fullFilePath = BASE_VIDEO_PATH + File.separator + studentId + File.separator + filePath;
            File videoFile = new File(fullFilePath);
            
            if (videoFile.exists() && videoFile.isFile()) {
                // 检查是否为视频文件
                String extension = getFileExtension(videoFile.getName()).toLowerCase();
                if (extension.equals("mp4") || extension.equals("avi") || extension.equals("mov") || 
                    extension.equals("mkv") || extension.equals("wmv") || extension.equals("flv")) {
                    return videoFile;
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}