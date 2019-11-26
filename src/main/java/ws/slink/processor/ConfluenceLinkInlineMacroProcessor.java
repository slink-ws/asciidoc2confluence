package ws.slink.processor;

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.asciidoctor.extension.Name;

import java.util.HashMap;
import java.util.Map;

@Name ("cflink")
public class ConfluenceLinkInlineMacroProcessor extends InlineMacroProcessor {

    private final String confluenceSpaceKey;

    public ConfluenceLinkInlineMacroProcessor(String confluenceSpaceKey) {
        this.confluenceSpaceKey = confluenceSpaceKey;
    }

    @Override
    public Object process (ContentNode parent, String target, Map<String, Object> attributes) {
        try {
            String title = attributes.get("1").toString();
            String[] parts = target.split(":", 2);
            String space, page;
            if (parts.length > 1) {
                space = parts[0];
                page = parts[1];
            } else {
                space = confluenceSpaceKey;
                page = parts[0];
            }
            Map<String, Object> options = new HashMap<>();
            options.put("type", ":link");
            options.put("target", "/display/" + space + "/" + page);
            return createPhraseNode(parent, "anchor", title, attributes, options);
        } catch (Exception e) {
            Map<String, Object> options = new HashMap<>();
            options.put("type", ":link");
            options.put("target", target);
            return createPhraseNode(parent, "anchor", target, attributes, options);
        }
    }

}
