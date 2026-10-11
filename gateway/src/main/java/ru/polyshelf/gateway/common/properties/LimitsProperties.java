package ru.polyshelf.gateway.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;


@ConfigurationProperties(prefix = "polyshelf.limits")
public record LimitsProperties(
        @DefaultValue("64KB") DataSize maxBodySize,
        @DefaultValue("30") int authRequestsPerMinute
) {
}
