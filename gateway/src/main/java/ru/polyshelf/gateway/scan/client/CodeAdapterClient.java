package ru.polyshelf.gateway.scan.client;

import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.polyshelf.gateway.common.config.HttpClientsConfiguration;
import ru.polyshelf.gateway.common.error.UpstreamContractException;

@Component
public class CodeAdapterClient {

    private static final String SERVICE_NAME = "code-adapter";
    private static final String PROCESS_PATH = "/process";
    private static final String CZ_RESPONSE_PATH = "/cz-response";
    private static final ParameterizedTypeReference<Map<String, Object>> PRODUCT_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient client;

    public CodeAdapterClient(@Qualifier(HttpClientsConfiguration.CODE_ADAPTER_CLIENT) RestClient client) {
        this.client = client;
    }

    public CodeAdapterResult process(String code) {
        CodeAdapterResult result = client.post()
                .uri(PROCESS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ProcessCodePayload(code))
                .retrieve()
                .body(CodeAdapterResult.class);
        if (result == null) {
            throw new UpstreamContractException(SERVICE_NAME, "empty processing result");
        }
        return result;
    }

    public Map<String, Object> prepareProductForApp(Map<String, Object> czPayload) {
        Map<String, Object> product = client.post()
                .uri(CZ_RESPONSE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CzResponsePayload(czPayload))
                .retrieve()
                .body(PRODUCT_TYPE);
        if (product == null) {
            throw new UpstreamContractException(SERVICE_NAME, "empty product information");
        }
        return product;
    }
}
