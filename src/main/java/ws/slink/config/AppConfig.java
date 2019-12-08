package ws.slink.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "a2c")
public class AppConfig {

    private String input;
    private String dir;
    private String url;
    private String user;
    private String pass;
    private String space;
    private boolean debug;
    private boolean force;
    private List<String> clean = new ArrayList<>();

}
