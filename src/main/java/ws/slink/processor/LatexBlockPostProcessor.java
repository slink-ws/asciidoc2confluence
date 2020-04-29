package ws.slink.processor;

import lombok.extern.slf4j.Slf4j;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;

import java.util.regex.Pattern;

@Slf4j
public class LatexBlockPostProcessor extends Postprocessor {

    @Override
    public String process(Document document, String convertedDocument) {
        return convertedDocument
            .replaceAll(Pattern.quote("\\" + "[" + "\\" + "l"), "<div class=\"math-left\">(mathjax-block(")
            .replaceAll(Pattern.quote("\\" + "[" + "\\" + "r"), "<div class=\"math-right\">(mathjax-block(")
            .replaceAll(Pattern.quote("\\" + "["), "<div>(mathjax-block(")
            .replaceAll(Pattern.quote("\\" + "]"), ")mathjax-block)</div>")
            .replaceAll(Pattern.quote("\\" + "("), "(mathjax-inline(")
            .replaceAll(Pattern.quote("\\" + ")"), ")mathjax-inline)")
        ;
    }


}
