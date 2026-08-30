package com.biodatamaker.config;

import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The SPA form submits every field as a string, so unselected enum / number / date
 * fields arrive as {@code ""}. Treat an empty string as {@code null} for non-string
 * targets instead of failing deserialization.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer emptyStringAsNull() {
        return builder -> builder.postConfigurer(mapper ->
                mapper.coercionConfigDefaults()
                        .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull));
    }
}
