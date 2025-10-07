package com.example.paymentservice.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Component;

/**
 * <h4> 외부의 웹 요청을 받아서 어플리케이션으로 전달하는 역할을 담당하는 것을 명확하게 나타내기 위한 어노테이션 </h4>
 */
@Target(ElementType.TYPE)   // 클래스 레벨에 선언
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface WebAdapter {}
