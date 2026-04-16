package demo.service;

import org.springframework.web.multipart.MultipartFile;

public interface IFileUploadService {
    String uploadFile(MultipartFile file, String subDir);

    void deleteFile(String filePath);
}
