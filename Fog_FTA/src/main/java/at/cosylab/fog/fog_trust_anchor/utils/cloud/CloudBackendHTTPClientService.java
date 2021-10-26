package at.cosylab.fog.fog_trust_anchor.utils.cloud;


import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fog.error_handling.global_exceptions.InternalServerErrorException;
import fog.globals.FogComponentsConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import payloads.tnta.certificate.FogNodeCSR;
import payloads.tnta.certificate.FogNodeCSRResponse;
import payloads.tnta.certificate.RegisterFogNodeResponse;
import utils.CloudConstants;

import java.io.IOException;
import java.net.ConnectException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CloudBackendHTTPClientService {

    @Value("${cosylab.fta.tnta.url}")
    private String tntaURL;

    public RegisterFogNodeResponse registerFTACredentials(String requestContent) throws ConnectException, InternalServerErrorException {
        RestTemplate restTemplate = new RestTemplate();
        String url = tntaURL + CloudConstants.TNTA_ROUTE_REGISTER_FTA;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<?> requestEntity = new HttpEntity<>(requestContent, headers);
        HttpEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        HttpEntity<RegisterFogNodeResponse> httpResp = new HttpEntity<RegisterFogNodeResponse>(handleHTTPResponse(response.getBody(), RegisterFogNodeResponse.class), response.getHeaders());

        return httpResp.getBody();
    }

    public FogNodeCSRResponse requestCertificateFromTNTA(FogNodeCSR req) throws ConnectException, InternalServerErrorException {
        RestTemplate restTemplate = new RestTemplate();
        String url = tntaURL + CloudConstants.TNTA_ROUTE_FTA_CSR;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<?> requestEntity = new HttpEntity<>(req);
        HttpEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        HttpEntity<FogNodeCSRResponse> httpResp = new HttpEntity<>(handleHTTPResponse(response.getBody(), FogNodeCSRResponse.class), response.getHeaders());

        return httpResp.getBody();
    }

    private <T> T handleHTTPResponse(String jsonBody, Class<T> responseClassType) throws InternalServerErrorException {
        ObjectMapper mapper = new ObjectMapper();
        if (jsonBody != null) {
            try {
                try {
                    Map<String, Object> respMap = mapper.readValue(jsonBody, LinkedHashMap.class);
                    if ((respMap.size() == FogComponentsConstants.ERR_RESP_MAP_SIZE) &&
                            ((respMap.get(FogComponentsConstants.ERR_RESP_MAP_ERR_CODE) != null) ||
                                    (respMap.get(FogComponentsConstants.ERR_RESP_MAP_ERR_MSG) != null))) {
                        throw new InternalServerErrorException();
                    }
                } catch (JsonMappingException e) {
                }

                // No error received process JSON as required response
                return mapper.readValue(jsonBody, responseClassType);
            } catch (IOException e) {
                throw new InternalServerErrorException();
            }
        } else {
            return null;
        }
    }
}
