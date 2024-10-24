package io.dongvelop.springbootsse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SSEContoller {

    private final SSEService service;

    /**
     * Server - Client 연결 API <br/>
     * produces 속성을 "text/event-stream"로 지정해야 Client 와 스트림으로 연결 가능.
     *
     * @param userId : 유저 ID
     * @return : SSE Emitter
     */
    @GetMapping(
            value = "/connect/{userId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter connect(@PathVariable(value = "userId") final Long userId,
                              @RequestParam(value = "lastEventId", required = false, defaultValue = "") final String lastEventId) {
        log.info("userId[{}], lastEventId[{}]", userId, lastEventId);
        return service.connect(userId, lastEventId);
    }

    /**
     * Server Send Events 발송 API
     *
     * @param userId : 유저 ID
     */
    @PostMapping(
            value = "/events/{userId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public void sendServerSentEvents(@PathVariable(value = "userId") final Long userId) {
        log.info("userId[{}]", userId);

        final var data = new SSEEventData("2dongyeop", "안녕하세요.");
        final String comment = "자기소개가 도착했습니다.";
        final String type = "INTRODUCE";

        service.send(userId, data, comment, type);
    }
}
