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
public class UserRegisteredEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String eventId;
    private Long userId;
    private String username;
    private String name;
    private String email;
    private String role;
    private LocalDateTime occurredAt;
}
