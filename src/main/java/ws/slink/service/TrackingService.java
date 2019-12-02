package ws.slink.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TrackingService {

    private final Map<String, Integer> publishedTitles;

    public TrackingService() {
        publishedTitles = new ConcurrentHashMap<>();
    }

    public boolean contains(String title) {
        return publishedTitles.containsKey(title);
    }

    public void add(String title) {
        publishedTitles.putIfAbsent(title, 0);
        publishedTitles.compute(title, (a,b) -> b+1);
    }

    public Map<String, Integer> get() {
        return publishedTitles.entrySet().stream().filter(e -> e.getValue() > 1).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

}
