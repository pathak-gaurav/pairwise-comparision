package laurentian.pairwise.rest;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component("restServiceClient")
public class RestServiceClient {
    private static Logger logger = LoggerFactory.getLogger(RestServiceClient.class);

    private RestTemplate restTemplate;

    public RestServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public <B, T> T invokePaloAltoService(HttpHeaders headers, HttpMethod method, final B body, final Class<T>
            responseClass, String endPointUrl, HttpEntity<LinkedMultiValueMap<String, Object>> variables) {

        final ResponseEntity<T> responseEntity = restTemplate.exchange(endPointUrl, method, variables, responseClass);
        return responseEntity.getBody();

    }
}
