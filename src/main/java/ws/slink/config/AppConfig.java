package ws.slink.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


/**
 * accepts following configuration parameters from any spring-supported configuration source
 * (properties file, command line arguments, environment variables):
 *
 *   a2c.url   - confluence server URL
 *   a2c.user  - confluence server user
 *   a2c.pass  - confluence server password
 *   a2c.input - input file for processing
 *   a2c.dir   - input directory for processing
 *   a2c.space - confluence space override
 *   a2c.clean - CSV list of confluence spaces to be cleaned up
 *   a2c.force - perform forced clean up (remove all including 'protected' pages)
 *   a2c.debug - print document to stdout in case of processing error
 *
 */

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
