package ws.slink.processor;

import lombok.extern.slf4j.Slf4j;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockMacroProcessor;
import org.asciidoctor.extension.Name;

import java.util.Map;

@Name("children")
@Slf4j
public class ChildrenMacroProcessor extends BlockMacroProcessor {

    @Override
    public Object process (StructuralNode parent, String target, Map<String, Object> attributes) {
        String content = new StringBuilder()
            .append("<div>")
            .append("<ac:structured-macro ac:name=\"children\">\n")
                .append("<ac:parameter ac:name=\"reverse\">false</ac:parameter>\n")
                .append("<ac:parameter ac:name=\"sort\">title</ac:parameter>\n") // creation | title | modified
                .append("<ac:parameter ac:name=\"style\">h4</ac:parameter>\n")
                .append("<ac:parameter ac:name=\"excerpt\">false</ac:parameter>\n")
                .append("<ac:parameter ac:name=\"first\">99</ac:parameter>\n")
                .append("<ac:parameter ac:name=\"depth\">3</ac:parameter>\n")
                .append("<ac:parameter ac:name=\"all\">true</ac:parameter>\n")
                .append("<ac:parameter ac:name=\"page\">\n")
                    .append("<ac:link>\n")
                        .append("<ri:page ri:content-title=\"")
                        .append(target.replaceAll("\\+", " "))
                        .append("\"/>\n")
                    .append("</ac:link>\n")
                .append("</ac:parameter>\n")
            .append("</ac:structured-macro>\n")
            .append("</div>")
            .toString()
        ;
        return createBlock(parent, "pass", content);
    }
}
