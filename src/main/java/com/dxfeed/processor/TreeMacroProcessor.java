package com.dxfeed.processor;

import lombok.extern.slf4j.Slf4j;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockMacroProcessor;
import org.asciidoctor.extension.Name;

import java.util.Map;

@Name("pagetree")
@Slf4j
public class TreeMacroProcessor extends BlockMacroProcessor {

    @Override
    public Object process (StructuralNode parent, String target, Map<String, Object> attributes) {
        String content = new StringBuilder()
            .append("<div>")
            .append("<ac:structured-macro ac:name=\"pagetree\">\n")
                .append("<ac:parameter ac:name=\"reverse\">false</ac:parameter>\n")
                .append("<ac:parameter ac:name=\"sort\">natural</ac:parameter>\n")
                .append("<ac:parameter ac:name=\"root\">\n")
                    .append("<ac:link>\n")
                        .append("<ri:page ri:content-title=\"")
                        .append(target.replaceAll("\\+", " "))
                        .append("\"/>\n")
                    .append("</ac:link>\n")
                .append("</ac:parameter>\n")
                .append("<ac:parameter ac:name=\"startDepth\">3</ac:parameter>\n")
                .append("<ac:parameter ac:name=\"excerpt\">true</ac:parameter>\n")
                .append("<ac:parameter ac:name=\"searchBox\">false</ac:parameter>\n")
                .append("<ac:parameter ac:name=\"expandCollapseAll\">false</ac:parameter>\n")
            .append("</ac:structured-macro>\n")
            .append("</div>")
            .toString()
        ;

        return createBlock(parent, "pass", content);
    }
}
