package com.example.paymentservice.common;

import java.util.UUID;
import lombok.experimental.UtilityClass;

/**
 * <h4> 멱등키 생성 유틸 클래스 </h4>
 */
@UtilityClass
public class IdempotencyCreator {

    public String create(Object data) {
        return UUID.nameUUIDFromBytes(data.toString().getBytes()).toString();
    }
}
