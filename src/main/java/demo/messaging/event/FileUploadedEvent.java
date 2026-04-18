package demo.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadedEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String eventId;
    private Long userId;
    private String filePath;
    private String subDir;
    private String originalFilename;
    private String contentType;
    private long size;
    private LocalDateTime occurredAt;
}
