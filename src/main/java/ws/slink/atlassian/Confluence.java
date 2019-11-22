package ws.slink.atlassian;

import ws.slink.tools.FluentJson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.json.simple.parser.ParseException;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.Optional;

@Slf4j
public class Confluence {

    private final String baseUrl;
    private final String user;
    private final String pass;

    public Confluence(String baseUrl, String user, String pass) {
        this.baseUrl = baseUrl;
        this.user = user;
        this.pass = pass;
    }

    public Optional<String> getPageId(String space, String title) {
        try {
            String url = String.format("%s/rest/api/content?title=%s&spaceKey=%s&expand=history", baseUrl, title, space);
            ResponseEntity<String> response = new RestTemplate().exchange(url, HttpMethod.GET, prepare(null), String.class);
            return Optional.ofNullable(new FluentJson(response.getBody()).get("results").get(0).getString("id").replaceAll("\"", ""));
        } catch (ParseException | IndexOutOfBoundsException e) {
            log.trace("page '{}' not found in '{}'", title, space);
        }
        return Optional.empty();
    }
    public void deletePage(String pageId) {
        String url = String.format("%s/rest/api/content/%s", baseUrl, pageId);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.exchange(url, HttpMethod.DELETE, prepare(null), String.class);
        restTemplate.exchange(url + "?status=trashed", HttpMethod.DELETE, prepare(null), String.class);
    }
    public void publishPage(String space, String title, String content) {
        String url = String.format("%s/rest/api/content", baseUrl);
        FluentJson fj = new FluentJson()
            .set("type", "page")
            .set("title", title)
            .set("space", new FluentJson().set("key", space))
            .set("body", new FluentJson().set("storage", new FluentJson().set("value", content).set("representation", "storage")));
        try {
            ResponseEntity<String> response = new RestTemplate().exchange(url, HttpMethod.POST, prepare(fj.toString()), String.class);
            if (response.getStatusCode().isError()) {
                log.trace("could not create page: {} ({}) {}", response.getStatusCode(), response.getStatusCodeValue(), response.getBody());
            } else {
                log.trace("created page #{}", title);
            }
        } catch (HttpClientErrorException e) {
            log.trace("error creating page: {}", e.getMessage());
        }
    }

    private HttpEntity<String> prepare(String data) {
        HttpHeaders headers = new HttpHeaders();
        String auth = user + ":" + pass;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.isBlank(data))
            return new HttpEntity<>(headers);
        else
            return new HttpEntity<>(data, headers);
    }

}
