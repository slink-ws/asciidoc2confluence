package com.dxfeed.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.ContentModel;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Contexts;
import org.asciidoctor.extension.Name;
import org.asciidoctor.extension.Reader;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

@Name("code")
@Contexts({Contexts.LISTING})
@ContentModel(ContentModel.SIMPLE)
@Slf4j
public class CodeBlockProcessor extends BlockProcessor {

    private static final String MARKER = "$$$$$$";

    @Value("${asciidoc.code.language.default}")
    private String defaultCodeLanguage;

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        String content = reader.read();
        String language = (String)attributes.get("2");
        if (StringUtils.isBlank(language))
            language = defaultCodeLanguage;
        Block block = createBlock(
             parent
            ,"listing"
            ,new StringBuilder()
                .append(MARKER)
                .append(language)
                .append(MARKER)
                .append(unescapeSymbols(content))
                .append(MARKER)
                .toString()
            ,attributes);
        return block;
    }

    private String unescapeSymbols(String input) {
        return input
            .replaceAll("&lt;", "<")
            .replaceAll("&gt;", ">")
            .replaceAll("&amp;", "&");
    }

}
