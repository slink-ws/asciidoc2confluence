package ws.slink.processor;

import lombok.extern.slf4j.Slf4j;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class CodeBlockPostProcessor extends Postprocessor {

    private static final String  CODE_START      = "(.*)(<pre>)([$]{6})([A-Za-z]+)([$]{6})(.*)";
    private static final String  CODE_END        = "(.*)([$]{6})(</pre>)(.*)";
    private static final String  CODE_ONELINE    = "(.*)(<pre>)([$]{6})([A-Za-z]+)([$]{6})(.*)([$]{6})(</pre>)(.*)";
    private static final Pattern START_PATTERN   = Pattern.compile(CODE_START, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern END_PATTERN     = Pattern.compile(CODE_END, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern ONELINE_PATTERN = Pattern.compile(CODE_ONELINE, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Override
    public String process(Document document, String convertedDocument) {
        return Arrays.stream(convertedDocument.split("\n"))
            .map(this::processString)
            .collect(Collectors.joining("\n"));
    }

    private String processString(String string) {

        Matcher matcher = ONELINE_PATTERN.matcher(string);
        if (matcher.matches())
            return matcher.group(1) + codeOpenElement(matcher.group(4)) + unescapeSymbols(matcher.group(6)) + codeCloseElement() + matcher.group(9);

        matcher = START_PATTERN.matcher(string);
        if (matcher.matches())
            return matcher.group(1) + codeOpenElement(matcher.group(4)) + unescapeSymbols(matcher.group(6));

        matcher = END_PATTERN.matcher(string);
        if (matcher.matches())
            return unescapeSymbols(string.replace(matcher.group(2) + matcher.group(3), "")) + codeCloseElement();

        return string;
    }

    // see:
    //    https://confluence.atlassian.com/display/CONF55/Code+Block+Macro
    //    https://stackoverflow.com/questions/30194918/how-do-you-insert-a-confluence-macro-into-a-page-created-by-the-confluence-rest
    private String codeOpenElement(String language) {
        return new StringBuilder()
            .append("<ac:structured-macro ac:name=\"code\">")
            .append("<ac:parameter ac:name=\"title\"></ac:parameter>")
            .append("<ac:parameter ac:name=\"theme\">default</ac:parameter>")
            .append("<ac:parameter ac:name=\"linenumbers\">false</ac:parameter>")
            .append("<ac:parameter ac:name=\"language\">")
            .append(language)
            .append("</ac:parameter>")
            .append("<ac:parameter ac:name=\"firstline\">0001</ac:parameter>")
            .append("<ac:parameter ac:name=\"collapse\">false</ac:parameter>")
            .append("<ac:plain-text-body>")
            .append("<![CDATA[")
            .toString()
        ;
    }
    private String codeCloseElement() {
        return new StringBuilder()
            .append("]]>")
            .append("</ac:plain-text-body>")
            .append("</ac:structured-macro>")
            .toString()
        ;
    }

    private String unescapeSymbols(String input) {
        return input
            .replaceAll("&lt;", "<")
            .replaceAll("&gt;", ">")
            .replaceAll("&amp;", "&");
    }

}
