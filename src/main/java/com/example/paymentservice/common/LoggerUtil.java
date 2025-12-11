package com.example.paymentservice.common;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class LoggerUtil {

    public void info(String context, String message, Object data) {

        Map<String, Object> logMap = new HashMap<>();
        logMap.put("context", context);
        logMap.put("message", message);
        logMap.put("data", data);

        try {
            log.info(ObjectMapperUtil.getObjectMapper().writeValueAsString(logMap));
        } catch (Exception e) {
            log.error("Failed to serialize log message", e);
        }
    }

    public void error(String context, String message, Throwable throwable) {
        
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("context", context);
        logMap.put("message", message);
        logMap.put("exception", throwable);
        logMap.put("stacktrace", throwable != null ? throwable.getStackTrace() : null);
        
        try {
            log.error(ObjectMapperUtil.getObjectMapper().writeValueAsString(logMap));
        } catch (Exception e) {
            log.error("Failed to serialize log message", e);
        }
    }
}
