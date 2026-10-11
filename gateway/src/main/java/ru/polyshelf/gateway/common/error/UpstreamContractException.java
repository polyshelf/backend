package ru.polyshelf.gateway.common.error;

import org.springframework.http.HttpStatus;

public class UpstreamContractException extends GatewayException {

    public UpstreamContractException(String service, String problem) {
        super(HttpStatus.BAD_GATEWAY, "UPSTREAM_CONTRACT_VIOLATION",
                "Unexpected response from " + service + ": " + problem);
    }
}
