package com.example.paymentservice.payment.adapter.out.persistent.stream.util;

import static java.lang.Math.abs;

import org.springframework.stereotype.Component;

@Component
public class PartitionKeyUtil {

    // 이 값이 6인 이유는 카프카에 생성된 토픽의 파티션 값이 6이기 때문이다.
    private static final int PARTITION_KEY_COUNT = 6;

    public int createPartitionKey(int number) {
        return abs(number) % PARTITION_KEY_COUNT;
    }
}
