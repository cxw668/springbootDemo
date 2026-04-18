package demo.messaging;

import demo.model.User;
import org.springframework.web.multipart.MultipartFile;

public interface DomainEventPublisher {

    void publishUserRegistered(User user);

    void publishFileUploaded(Long userId, String subDir, String filePath, MultipartFile file);
}
