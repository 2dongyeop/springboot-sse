package io.dongvelop.springbootsse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SSERepository {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * SSE Emitter 저장
     *
     * @param key     : 유저 ID + "-" + SystemCurrentTimeMillis 꼴
     * @param emitter : SSE Emitter
     */
    public void save(String key, SseEmitter emitter) {
        emitters.put(key, emitter);
    }

    /**
     * SSE Emitter 삭제
     *
     * @param key : 유저 ID + "-" + SystemCurrentTimeMillis 꼴
     */
    public void deleteById(String key) {
        emitters.remove(key);
    }

    /**
     * SSE Emitter 조회
     *
     * @param key : 유저 ID
     * @return : SSE Emitter
     */
    public SseEmitter get(String key) {
        return emitters.get(key);
    }

    /**
     * SSE Emitter 조회
     *
     * @param userId : 유저 ID
     * @return : SSE Emitter
     */
    public Map<String, SseEmitter> findAllEmitterStartWithUserId(String userId) {
        return emitters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(userId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
