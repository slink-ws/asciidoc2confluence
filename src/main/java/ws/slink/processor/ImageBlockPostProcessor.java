package ws.slink.processor;

import org.apache.commons.lang.StringUtils;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class ImageBlockPostProcessor extends Postprocessor {

    @Override
    public String process(Document document, String convertedDocument) {
        final org.jsoup.nodes.Document doc = Jsoup.parse(convertedDocument);

        org.jsoup.nodes.Document.OutputSettings settings = new org.jsoup.nodes.Document.OutputSettings();
        settings.prettyPrint(false);
        settings.syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
        doc.outputSettings(settings);

        // process center-aligned images
        doc.select("div.imageblock.text-center").stream().forEach(element -> {
//            System.err.println(element.toString());
            updateImage(element, "image-center");
            element.removeClass("text-center");
        });

        // process left-aligned images
        doc.select("div.imageblock.text-left").stream().forEach(element -> {
            updateImage(element, "");
            element.removeClass("text-left");
        });

        // process non-aligned images
        doc.select("div.imageblock").stream().forEach(element -> {
            updateImage(element, "");
            element.removeClass("text-left");
        });

        // process right-aligned images
        doc.select("div.imageblock.text-right").stream().forEach(element -> {
            updateImage(element, "image-right");
            element.removeClass("text-right");
        });

        return doc.body().toString();
    }

    private void updateImage(Element element, String alignment) {
//        Element content = element.select("div.content").first();
//        if (null != content) {
//            content.tagName("p");
//            content.removeClass("content");
//            content.getElementsByTag("img").forEach(image -> {
//                image.wrap("<span class=\"confluence-embedded-file-wrapper confluence-embedded-manual-size " + ((StringUtils.isEmpty(alignment)) ? "" : alignment+"-wrapper") + "\">\n");
//                image.addClass("confluence-embedded-image");
//                image.addClass("confluence-external-resource");
//                image.addClass(alignment);
//                image.attr("data-image-src", image.attr("src"));
//            });
//            element.replaceWith(content);
//        }
        element.select("div.content").forEach(content -> {
            content.tagName("p");
            content.removeClass("content");
            content.getElementsByTag("img").forEach(image -> {
                image.wrap("<span class=\"confluence-embedded-file-wrapper confluence-embedded-manual-size " +
                            ((StringUtils.isEmpty(alignment)) ? "" : alignment+"-wrapper") + "\">\n");
                image.addClass("confluence-embedded-image");
                image.addClass("confluence-external-resource");
                image.addClass(alignment);
                image.attr("data-image-src", image.attr("src"));
            });
        });
        element.addClass("paragraph");
        element.removeClass("imageblock");
    }
}
