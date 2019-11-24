package ws.slink.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

@Slf4j
public class NoticeBlockPostProcessor extends Postprocessor {

    @Override
    public String process(Document document, String convertedDocument) {
        final org.jsoup.nodes.Document doc = Jsoup.parse(convertedDocument);
        doc.select("div.admonitionblock").stream().forEach(element -> {
            String noticeType = element.className().replace("admonitionblock ", "");
            Element content = element.selectFirst("td.content");
            org.jsoup.nodes.Document newElement = Jsoup.parse(admonitionBlock(noticeType, content.html()), "", Parser.xmlParser());
            element.replaceWith(newElement.child(0));
        });
        return doc.body().toString();
    }


    // see:
    //    https://confluence.atlassian.com/display/CONF55/Code+Block+Macro
    private String admonitionBlock(String noticeType, String content) {
        String confluenceNoticeType = getConfluenceNoticeType(noticeType);
        return new StringBuilder()
            .append("<ac:structured-macro ac:name=\"")
            .append(confluenceNoticeType)
            .append("\">")
                .append("<ac:parameter ac:name=\"icon\">false</ac:parameter>")
                .append("<ac:parameter ac:name=\"title\">")
                    .append(StringUtils.capitalize(confluenceNoticeType))
                .append("</ac:parameter>")
                .append("<ac:rich-text-body>")
                    .append(content)
                .append("</ac:rich-text-body>")
            .append("</ac:structured-macro>")
            .toString();
    }

    private String getConfluenceNoticeType(String noticeType) {
        switch(noticeType.toLowerCase()) {
            case "important": return "warning";
            case "warning"  : return "note";
            case "tip"      : return "tip";
            case "note"     :
            default         : return "info";
        }
    }
}
