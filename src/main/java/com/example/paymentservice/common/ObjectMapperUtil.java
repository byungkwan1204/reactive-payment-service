package com.example.paymentservice.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ObjectMapperUtil {

    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModules(new Jdk8Module(), new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
}
