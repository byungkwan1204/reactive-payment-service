package com.example.paymentservice.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Component;

/**
 * <h4> 애플리케이션 외부 세상인 메세지 큐와 연결된다는 의미의 어노테이션 </h4>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface StreamAdapter {}
