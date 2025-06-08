package org.example.Server1.services;

import org.example.Common.DTO.ClientStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ClientStatusHttpService{

    @Value("${service2.url}")
    private String service2Url;

    public final RestTemplate restTemplate;
    public final JwsTokenService jwsTokenService;

    public ClientStatusHttpService(RestTemplate restTemplate, JwsTokenService jwsTokenService) {
        this.restTemplate = restTemplate;
        this.jwsTokenService = jwsTokenService;

    }

    public ClientStatusResponse checkClientStatus(Long clientId, Long accountId){
        String token = jwsTokenService.generateToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "clientId", clientId,
                "accountId", accountId
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<ClientStatusResponse> response = restTemplate.exchange(
                service2Url + "/client/statusChecked",
                HttpMethod.POST,
                request,
                ClientStatusResponse.class
        );

        return response.getBody();

    }
}
