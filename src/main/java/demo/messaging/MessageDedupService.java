package demo.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class MessageDedupService {

    private static final String MESSAGE_DEDUP_PREFIX = "mq:processed:";
    private static final String PROCESSING = "PROCESSING";
    private static final String DONE = "DONE";

    private final StringRedisTemplate stringRedisTemplate;

    public boolean isProcessed(String eventId) {
        String value = stringRedisTemplate.opsForValue().get(MESSAGE_DEDUP_PREFIX + eventId);
        return DONE.equals(value);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean tryStartProcessing(String eventId) {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(
                MESSAGE_DEDUP_PREFIX + eventId,
                PROCESSING,
                Duration.ofHours(1)
        );
        return Boolean.TRUE.equals(success);
    }

    public void markProcessed(String eventId) {
        stringRedisTemplate.opsForValue().set(
                MESSAGE_DEDUP_PREFIX + eventId,
                DONE,
                Duration.ofHours(24)
        );
    }

    public void clearProcessing(String eventId) {
        stringRedisTemplate.delete(MESSAGE_DEDUP_PREFIX + eventId);
    }
}
