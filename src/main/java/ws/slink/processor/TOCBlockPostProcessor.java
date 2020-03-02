package ws.slink.processor;

import lombok.extern.slf4j.Slf4j;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;

import java.util.Optional;

@Slf4j
public class TOCBlockPostProcessor extends Postprocessor {

    // https://confluence.atlassian.com/display/CONF55/Table+of+Contents+Macro

    @Override
    public String process(Document document, String convertedDocument) {
        final org.jsoup.nodes.Document doc = Jsoup.parse(convertedDocument);

        org.jsoup.nodes.Document.OutputSettings settings = new org.jsoup.nodes.Document.OutputSettings();
        settings.prettyPrint(false);
        settings.syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
        doc.outputSettings(settings);

        Optional.ofNullable(doc.selectFirst("div#toc")).ifPresent(tocElement -> {
            tocElement.replaceWith(Jsoup.parse(tableOfContentsMacro(), "", Parser.xmlParser()).child(0));
        });

        return doc.body().children().html();
    }

    private String tableOfContentsMacro() {
        return new StringBuilder()
            .append("<div>")
            .append("<ac:structured-macro ac:name=\"toc\">")
            .append("<ac:parameter ac:name=\"printable\">true</ac:parameter>")
            .append("<ac:parameter ac:name=\"style\">circle</ac:parameter>")
            .append("<ac:parameter ac:name=\"indent\">1em</ac:parameter>")
            .append("<ac:parameter ac:name=\"maxLevel\">3</ac:parameter>")
            .append("<ac:parameter ac:name=\"minLevel\">2</ac:parameter>")
            .append("<ac:parameter ac:name=\"class\">bigpink</ac:parameter>")
            .append("<ac:parameter ac:name=\"type\">list</ac:parameter>")
            .append("<ac:parameter ac:name=\"outline\">false</ac:parameter>")
//            .append("<ac:parameter ac:name=\"exclude\">[1//2]</ac:parameter>")
//            .append("<ac:parameter ac:name=\"include\">.*</ac:parameter>")
            .append("</ac:structured-macro>")
            .append("</div>")
            .toString();
    }

}
