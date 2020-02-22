package ws.slink.atlassian;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ws.slink.config.AppConfig;
import ws.slink.model.Page;
import ws.slink.tools.FluentJson;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


@Slf4j
@Component
@RequiredArgsConstructor (onConstructor = @__(@Autowired))
public class Confluence {

    @Value("${confluence.protected.label:}")
    private List<String> protectedLabels;

    @Value("${confluence.urls.versions:}")
    private String urlsVersions;

    private final AppConfig appConfig;

    public Optional<String> getPageId(String space, String title) {
        String url = String.format("%s/rest/api/content?title=%s&spaceKey=%s&expand=history", baseUrl(), title, space);
        AtomicReference<Optional<String>> result = new AtomicReference<>(Optional.empty());
        exchange(
            url,
            HttpMethod.GET,
            prepare(null),
            new StringBuilder()
                .append("looking for pageId for page '")
                .append(title)
                .append("' in '")
                .append(space)
                .append("'")
                .toString()
            )
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
                } catch (IndexOutOfBoundsException e) {
                    log.trace("page '{}' not found in '{}'", title, space);
                }
            }
        );
        return result.get();
    }

    public Optional<Page> getPage(String space, String title) {
        Optional<String> pageId = getPageId(space, title);
        if (pageId.isPresent()) {
            log.trace("trying to get page '{}' from '{}'", title, space);
            return getPage(pageId.get());
        } else {
            log.trace("no page '{}' exists in '{}'", title, space);
            return Optional.empty();
        }
    }
    public Optional<Page> getPage(String pageId) {
        log.trace("trying to get page #{}", pageId);
        String url = String.format("%s/rest/api/content/%s?expand=metadata.labels,version", baseUrl(), pageId);
        AtomicReference<Optional<Page>> result = new AtomicReference<>(Optional.empty());
        exchange(
            url
            ,HttpMethod.GET
            ,prepare(null)
            ,new StringBuilder().append("requesting page #").append(pageId).toString()
        ).ifPresent(response -> {
//            log.trace("response: {}", response.getBody());
            try {
                result.set(parsePageJson(response.getBody()));
            } catch (Exception e) {
                log.warn("could not convert response json");
            }
        });
        return result.get();
    }

    public int deletePage(String pageId, String title) {
        String url = String.format("%s/rest/api/content/%s", baseUrl(), pageId);
        String errorMessage = new StringBuilder()
            .append("removing page #")
            .append(pageId)
            .append(" (")
            .append(title)
            .append(")")
            .toString();
        AtomicInteger r1 = new AtomicInteger(0);
        exchange(url, HttpMethod.DELETE, prepare(null), errorMessage).ifPresent(re -> {
            if (!re.getStatusCode().isError())
                r1.set(1);
        });
        exchange(url.concat("?status=trashed"), HttpMethod.DELETE, prepare(null), errorMessage);
        return r1.get();
    }
    public boolean publishPage(String space, String title, String parent, String status, String content) {
        String url = String.format("%s/rest/api/content", baseUrl());
        FluentJson fj = new FluentJson()
            .set("status", status)
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

        return exchange(
             url
            ,HttpMethod.POST
            ,prepare(fj.toString())
            ,new StringBuilder().append("publishing page #").append(title).toString()
        ).isPresent();
    }
    public boolean updatePage(String pageId, String newTitle, String newStatus, String newContent) {
        AtomicBoolean result = new AtomicBoolean(false);
        getCurrentVersion(pageId).ifPresent(version -> {
            if (version > 0) {
                log.trace("trying to update page #{}", pageId);
                String url = String.format("%s/rest/api/content/%s", baseUrl(), pageId);
                FluentJson fj = new FluentJson()
                    .set("status", newStatus)
                    .set("version", new FluentJson().set("number", version + 1))
                    .set("type", "page")
                    .set("title", newTitle)
                    .set("body", new FluentJson()
                        .set("storage", new FluentJson()
                            .set("value", newContent)
                            .set("representation", "storage")
                        )
                    );
                log.trace("DATA: {}", fj.toString());
                result.set(
                    exchange(
                        url
                        ,HttpMethod.PUT
                        ,prepare(fj.toString())
                        ,new StringBuilder().append("updating page #").append(newTitle).toString()
                    ).isPresent());
                removeVersion(pageId, version);
        }});
        return result.get();
    }

    public Optional<Integer> getCurrentVersion(String pageId) {
        AtomicReference<Optional<Integer>> result = new AtomicReference<>();
        result.set(Optional.empty());
        getPage(pageId).ifPresent(existingPage -> {
            try {
                log.debug("existing page: ", existingPage);
                result.set(Optional.of(existingPage.version()));
            } catch (Exception e) {
                log.debug("could not get current document version for page #{}", pageId);
            }
        });
        return result.get();
    }
    public boolean removeVersion(String pageId, int versionNumber) {
        AtomicBoolean result = new AtomicBoolean(false);
        log.trace("trying to remove page #{} v.{}", pageId, versionNumber);
        // DELETE /{api|experimental}/content/<id>/version/<number> / ?
        String url = String.format("%s/rest/%s/content/%s/version/%d", baseUrl(), urlsVersions, pageId, versionNumber);
        result.set(
            exchange(
                url
                ,HttpMethod.DELETE
                ,prepare(null)
                ,new StringBuilder().append("removing version " + versionNumber + " of page #").append(pageId).toString()
            ).isPresent());
        return false;
    }

    public boolean tagPage(String space, String title, List<String> tags) {
        Optional<String> pageId = getPageId(space, title);
        if(pageId.isPresent()) {
            return tagPage(pageId.get(), tags);
        } else {
            return false;
        }
    }
    public boolean tagPage(String pageId, List<String> tags) {
        if (tags.size() > 0) {
            String url = String.format("%s/rest/api/content/%s/label", baseUrl(), pageId);
            JSONArray labels = new JSONArray();
            tags.stream()
                    .map(tag -> (JSONObject) new FluentJson().set("name", tag.replaceAll(" ", "_")).set("prefix", "global").get())
                    .forEach(labels::add);
            return exchange(
                    url,
                    HttpMethod.POST,
                    prepare(labels.toString()),
                    new StringBuilder().append("tagging page #").append(pageId).toString()
            ).isPresent();
        } else {
            log.trace("no need to tag page #{} - no tags set for document", pageId);
            return false;
        }
    }
    public Collection<String> getTags(String space, String title) {
        Optional<String> pageId = getPageId(space, title);
        if(pageId.isPresent()) {
            return getTags(pageId.get());
        } else {
            return Collections.emptyList();
        }
    }
    public Collection<String> getTags(String pageId) {
        log.trace("trying to get tags for page #{}", pageId);
        String url = String.format("%s/rest/api/content/%s/label", baseUrl(), pageId);
        AtomicReference<List<String>> result = new AtomicReference<>(new ArrayList<>());
        exchange(
            url
            ,HttpMethod.GET
            ,prepare(null)
            ,new StringBuilder().append("requesting tags for page #").append(pageId).toString()
        ).ifPresent(response -> {
            try {
                result.set(new FluentJson(response.getBody()).get("results")
                    .stream()
                    .map(v -> v.getString("name").replaceAll("\"", ""))
                    .collect(Collectors.toList()));
            } catch (Exception e) {
                log.warn("could not get tag list for page #{}", pageId);
            }
        });
        return result.get();
    }
    public boolean removeTags(String pageId, Collection<String> tags) {
        if (tags.size() > 0) {
            AtomicBoolean result = new AtomicBoolean(true);
            tags.stream().forEach(tag -> {
                String url = String.format("%s/rest/api/content/%s/label?name=%s", baseUrl(), pageId, tag);
                result.set(result.get() & exchange(
                    url,
                    HttpMethod.DELETE,
                    prepare(null),
                    new StringBuilder()
                        .append("removing tag '")
                        .append(tag)
                        .append("' for page #")
                        .append(pageId).toString()).isPresent());
            });
            return result.get();
        } else {
            log.trace("no tags set to be removed");
            return false;
        }
    }

    public int cleanSpace(String space) {
        AtomicInteger result = new AtomicInteger(0);
        if (appConfig.isForce()) {
            getPages(space)
                .stream()
                .forEach(p -> result.addAndGet(deletePage(p.id(), p.title())));
        } else {
            getPages(space)
                .stream()
                .filter(p -> p.labels().stream().anyMatch(protectedLabels::contains))
                .forEach(p -> log.info("Skipping removal of '" + p.title() + "' (" + p.id() + ")"));
            getPages(space)
                .stream()
                .filter(p -> p.labels().stream().noneMatch(protectedLabels::contains))
                .forEach(p -> result.addAndGet(deletePage(p.id(), p.title())));
        }
        return result.get();
    }
    public List<Page> getPages(String space) {
        int index = 0;
        int limit = 25;
        List<Page> result = new ArrayList<>(limit * 2);
        while(true) {
            List<Page> batch = getPages(space, index, limit);
            if (batch.isEmpty())
                break;
            result.addAll(batch);
            index += limit;
        }
        return result;
    }
    private List<Page> getPages(String space, int start, int limit) {
        String url = String.format("%s/rest/api/content?type=page&spaceKey=%s&expand=metadata.labels,version&start=%d&limit=%d",
                                   baseUrl(),
                                   space,
                                   start,
                                   limit);
        List<Page> result = new ArrayList<>();
        exchange(
            url,
            HttpMethod.GET,
            prepare(null),
            new StringBuilder()
                .append("requesting pages from space ")
                .append(space)
                .toString()
        )
        .ifPresent(response -> {
            FluentJson fj = new FluentJson(response.getBody());
            fj.get("results")
                  .stream()
                  .map(pageJson -> parsePageJson(pageJson))
                  .filter(pageOpt -> pageOpt.isPresent())
                  .map(pageOpt -> pageOpt.get())
                  .forEach(result::add);
        });
        return result;
    }

    public boolean canPublish() {
        return
            StringUtils.isNotBlank(baseUrl())
         && StringUtils.isNotBlank(user())
         && StringUtils.isNotBlank(password());
    }

    private HttpEntity<String> prepare(String data) {
        HttpHeaders headers = new HttpHeaders();
        String auth = user() + ":" + password();
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
                case 400:
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

    private String baseUrl() {
        return appConfig.getUrl();
    }
    private String user() {
        return appConfig.getUser();
    }
    private String password() {
        return appConfig.getPass();
    }

    private Optional<Page> parsePageJson(String pageJsonStr) {
        try {
            return parsePageJson(new FluentJson(pageJsonStr));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    private Optional<Page> parsePageJson(FluentJson pageJson) {
//        System.err.println(pageJson.toPrettyString(2));
        try {
            return Optional.ofNullable(new Page()
                    .id(pageJson.getString("id").replaceAll("\"", ""))
                    .title(pageJson.getString("title").replaceAll("\"", ""))
                    .version(pageJson.get("version").getInt("number"))
                    .labels(
                        pageJson.get("metadata").get("labels").get("results")
                            .stream()
                            .map(lr -> lr.getString("name").replaceAll("\"", ""))
                            .collect(Collectors.toList())
                    )
            );
        } catch (Exception e) {
            log.warn("error parsing page json: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
