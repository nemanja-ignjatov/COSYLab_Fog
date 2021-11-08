package at.cosylab.fog.faca.commons.cloud;

import at.cosylab.fog.faca.commons.exceptions.InternalServerErrorException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fog.faca.utils.FACAProjectConstants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import payloads.acam.deviceTypes.ListDeviceTypesChangeUpdatesRequest;
import payloads.acam.deviceTypes.ListDeviceTypesChangeUpdatesResponse;
import payloads.acam.deviceTypes.ListLatestDeviceTypeResponse;
import payloads.tnta.ticket.TicketSignature;
import utils.CloudConstants;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CloudBackendHTTPClientService {

    @Value("${app.custom_config.cloudbackend.url}")
    @Getter
    @Setter
    private String CLOUDBACKEND_URL;

    @Value("${cosylab.faca.tnta.url}")
    private String tntaURL;

    public ListLatestDeviceTypeResponse getLatestDeviceTypesVersionFromCloud(String ticketToken, String ticketIssuer, String subjectIdentity) throws InternalServerErrorException {
        RestTemplate restTemplate = new RestTemplate();
        String url = CLOUDBACKEND_URL + CloudConstants.ACAM_GET_DEVICE_TYPES_VERSION;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set(CloudConstants.HEADER_TICKET_TOKEN, ticketToken);
        headers.set(CloudConstants.HEADER_TICKET_ISSUER, ticketIssuer);
        headers.set(CloudConstants.HEADER_REQUEST_SENDER, subjectIdentity);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        HttpEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        HttpEntity<ListLatestDeviceTypeResponse> httpResp = new HttpEntity<ListLatestDeviceTypeResponse>(handleHTTPResponse(response.getBody(), ListLatestDeviceTypeResponse.class), response.getHeaders());

        return httpResp.getBody();
    }

    public ListDeviceTypesChangeUpdatesResponse getLatestDeviceTypesVersionChangesFromCloud(ListDeviceTypesChangeUpdatesRequest req, String ticketToken, String ticketIssuer, String subjectIdentity) throws InternalServerErrorException {
        RestTemplate restTemplate = new RestTemplate();
        String url = CLOUDBACKEND_URL + CloudConstants.ACAM_GET_DEVICE_TYPES_VERSION;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set(CloudConstants.HEADER_TICKET_TOKEN, ticketToken);
        headers.set(CloudConstants.HEADER_TICKET_ISSUER, ticketIssuer);
        headers.set(CloudConstants.HEADER_REQUEST_SENDER, subjectIdentity);
        HttpEntity<?> requestEntity = new HttpEntity<>(req);
        HttpEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        HttpEntity<ListDeviceTypesChangeUpdatesResponse> httpResp = new HttpEntity<>(handleHTTPResponse(response.getBody(), ListDeviceTypesChangeUpdatesResponse.class), response.getHeaders());

        return httpResp.getBody();
    }

    public void validateFTATicket(TicketSignature ticket) throws InternalServerErrorException {
        RestTemplate restTemplate = new RestTemplate();
        String url = tntaURL + CloudConstants.TNTA_ROUTE_VALIDATE_TICKET;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<?> requestEntity = new HttpEntity<>(ticket);
        HttpEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        handleHTTPResponse(response.getBody(), String.class);
    }

    private <T> T handleHTTPResponse(String jsonBody, Class<T> responseClassType) throws InternalServerErrorException {
        ObjectMapper mapper = new ObjectMapper();
        if (jsonBody != null) {
            try {
                try {
                    Map<String, Object> respMap = mapper.readValue(jsonBody, LinkedHashMap.class);
                    if ((respMap.size() == FACAProjectConstants.ERR_RESP_MAP_SIZE) &&
                            ((respMap.get(FACAProjectConstants.ERR_RESP_MAP_ERR_CODE) != null) ||
                                    (respMap.get(FACAProjectConstants.ERR_RESP_MAP_ERR_MSG) != null))) {
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
