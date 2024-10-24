package io.dongvelop.springbootsse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SSEService {

    private final SSERepository repository;

    @Value("${sse.timeout.default:120000}")
    private Long defaultTimeout;

    /**
     * Server - Client 연결 메서드
     *
     * @param userId      : 유저 ID
     * @param lastEventId : 최근에 수신한 Key. 이 값이 존재하면, Client 가 추후에 재연결 시 해당 이벤트 이후의 정보만 수신.
     * @return : SSE Emitter
     */
    public SseEmitter connect(final Long userId, final String lastEventId) {
        log.debug("userId[{}], lastEventId[{}]", userId, lastEventId);

        // 1. 객체 생성 : 계속해서 연결을 갖고있지 않도록 객체 생성시 타임아웃 설정.
        final SseEmitter emitter = new SseEmitter(defaultTimeout);

        // 2. SSE Emitter 를 구분할 Key 생성.
        final String key = SSEUtils.generateSSEKey(userId);
        log.debug("key[{}]", key);

        // 3. 정상 동작 완료 후 & 타임아웃 시에 저장해놨던 객체 삭제되도록 동작 설정.
        emitter.onCompletion(() -> repository.deleteById(key));
        emitter.onTimeout(() -> repository.deleteById(key));
        repository.save(key, emitter);

        // 4. 첫 연결 시에, 메시지를 보내지 않으면 추후 503 에러 발생.
        sendServerSentEvents(key, "EventStream Created. [userId=" + userId + "]", "sse 접속 성공", "text");

        // 5. lastEventId 가 존재할 경우, 연결이 끊긴 후 재연결한 경우임.
        // 따라서, 남은 이벤트가 있다면 전송.
        if (StringUtils.hasText(lastEventId)) {
            final Map<String, SseEmitter> events = repository.findAllEmitterStartWithUserId(String.valueOf(userId));
            events.entrySet().stream()
                    .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
                    .forEach(event -> sendServerSentEvents(event.getKey(), event.getValue(), "", ""));
        }

        return emitter;
    }

    /**
     * Server Send Events 발송 메서드
     *
     * @param key     : 이벤트의 고유 ID.
     * @param data    : 전송할 data
     * @param comment : SSE 이벤트에 추가할 주석. 디버깅 및 로그 용도.
     * @param type    : 이벤트 이름. Event 구분 타입.
     * @param <T>     : 전송할 data 타입
     */
    public <T> void sendServerSentEvents(final String key, final T data, final String comment, final String type) {
        log.info("key[{}], data[{}], comment[{}], type[{}]", key, data, comment, type);

        final SseEmitter emitter = repository.get(key);
        if (emitter == null) {
            final String errorMessage = "emitter key[" + key + "] not found.";
            log.error("errorMessage[{}]", errorMessage);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage);
        }
        log.info("emitter[{}]", emitter);

        try {
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(key))
                    .name(type)
                    .data(data)
                    .comment(comment));
        } catch (IOException e) {
            // 전송에 실패하더라도, 우선 삭제 처리.
            repository.deleteById(key);
            emitter.completeWithError(e);

            log.error("send SSE Message error. emitter[{}] ", emitter);
        }
    }

    public <T> void send(final Long userId, final T data, final String comment, final String type) {
        log.info("userId[{}], data[{}], comment[{}], type[{}]", userId, data, comment, type);

        final Map<String, SseEmitter> events = repository.findAllEmitterStartWithUserId(String.valueOf(userId));

        if (events.isEmpty()) {
            final String errorMessage = "emitter userId[" + userId + "] not found.";
            log.error("errorMessage[{}]", errorMessage);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage);
        }

        events.forEach((key, emitter) -> {
            try {
                log.info("key[{}], emitter[{}]", key, emitter);

                emitter.send(SseEmitter.event()
                        .id(String.valueOf(key))
                        .name(type)
                        .data(data)
                        .comment(comment));
            } catch (IOException e) {
                // 전송에 실패하더라도, 우선 삭제 처리.
                repository.deleteById(key);
                emitter.completeWithError(e);

                log.error("send SSE Message error. emitter[{}] ", emitter);
            }
        });
    }
}
