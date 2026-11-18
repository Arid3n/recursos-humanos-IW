package com.iw.rrhh.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj inyectable para que los instantes de negocio sean deterministas en tests (Art. 22.5).
 */
@Configuration
public class ClockConfig {

    /** Zona horaria de negocio (Art. 14.2): el servidor opera en UTC y convierte en los bordes. */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Madrid");

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
