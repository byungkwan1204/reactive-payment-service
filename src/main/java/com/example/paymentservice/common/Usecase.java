package com.example.paymentservice.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Component;

/**
 * - application 이 제공하는 핵심 기능들의 작업 흐름을 의미한다.
 * - usecase 에서 domain 패키지 들의 클래스들이 상호작용하면서 비즈니스 로직들이 처리된다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface Usecase {

}
