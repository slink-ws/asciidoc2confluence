package ws.slink.atlassian;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ws.slink.tools.FluentJson;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
        String url = String.format("%s/rest/api/content?title=%s&spaceKey=%s&expand=history", baseUrl, title, space);
        AtomicReference<Optional<String>> result = new AtomicReference<>(Optional.empty());
        exchange(url, HttpMethod.GET, prepare(null), "looking for pageId for page #" + title + " in " + space)
            .ifPresent(response -> {
                try {
                    result.set(
                        Optional.of(
                            new FluentJson(response.getBody())
                            .get("results")
                            .get(0)
                            .getString("id")
                            .replaceAll("\"", "")
                        )
                    );
                } catch (IndexOutOfBoundsException | ParseException e) {
                    log.trace("page '{}' not found in '{}'", title, space);
                }
            }
        );
        return result.get();
    }
    public void deletePage(String pageId) {
        String url = String.format("%s/rest/api/content/%s", baseUrl, pageId);
        exchange(url, HttpMethod.DELETE, prepare(null), "removing page #" + pageId);
        exchange(url + "?status=trashed", HttpMethod.DELETE, prepare(null), "removing page #" + pageId);
    }
    public boolean publishPage(String space, String title, String parent, String content) {
        String url = String.format("%s/rest/api/content", baseUrl);
        FluentJson fj = new FluentJson()
            .set("type", "page")
            .set("title", title)
            .set("space", new FluentJson().set("key", space))
            .set("body", new FluentJson()
                         .set("storage", new FluentJson()
                                         .set("value", content)
                                         .set("representation", "storage")
                         )
            );
        if (StringUtils.isNotBlank(parent))
            getPageId(space, parent).ifPresent(parentId -> {
                List<JSONObject> list = new ArrayList<>();
                list.add((JSONObject) new FluentJson().set("id", Long.valueOf(parentId)).get());
                fj.set("ancestors", list);
            });
            log.trace("DATA: {}", fj.toString());
            return exchange(url, HttpMethod.POST, prepare(fj.toString()), "publishing page #" + title).isPresent();
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
    private Optional<ResponseEntity<String>> exchange(String url, HttpMethod method, HttpEntity httpEntity, String message) {
        try {
            return Optional.ofNullable(new RestTemplate().exchange(url, method, httpEntity, String.class));
        } catch (ResourceAccessException e) {
            log.warn("Confluence server access exception: {}", e.getMessage());
        } catch (HttpClientErrorException e) {
            switch (e.getStatusCode().value()) {
                case 403:
                case 404:
                    log.warn("Confluence server error {}: {} {}", message, e.getStatusCode(), e.getStatusText());
                    break;
                default:
                    log.error("Unexpected HTTP error {}: {} {}", message, e.getStatusCode(), e.getStatusText());
            }
        }
        return Optional.empty();
    }

    public boolean canPublish() {
        return
            StringUtils.isNotBlank(baseUrl)
         && StringUtils.isNotBlank(user)
         && StringUtils.isNotBlank(pass);
    }
}
