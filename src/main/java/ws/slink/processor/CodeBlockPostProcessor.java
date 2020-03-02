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

    // https://confluence.atlassian.com/display/CONF55/Code+Block+Macro

    @Override
    public String process(Document document, String convertedDocument) {
        ThreadLocal<Boolean> inCodeBlock = ThreadLocal.withInitial(() -> false);
        return Arrays.stream(convertedDocument.split("\n"))
            .map(s -> this.processString(s, inCodeBlock))
            .collect(Collectors.joining("\n"));
    }

    private String processString(String string, ThreadLocal<Boolean> inCodeBlock) {
        Matcher matcherA = ONELINE_PATTERN.matcher(string);
        Matcher matcherB = START_PATTERN.matcher(string);
        Matcher matcherC = END_PATTERN.matcher(string);
        String result;
        if (matcherA.matches()) {
            result = unescapeSymbols(matcherA.group(1) + codeOpenElement(matcherA.group(4)) + matcherA.group(6) + codeCloseElement() + matcherA.group(9));
        } else if (matcherB.matches()) {
            result = unescapeSymbols(matcherB.group(1) + codeOpenElement(matcherB.group(4)) + matcherB.group(6));
            inCodeBlock.set(true);
        } else if (matcherC.matches()) {
            result = unescapeSymbols(string.replace(matcherC.group(2) + matcherC.group(3), "") + codeCloseElement());
            inCodeBlock.set(false);
        } else {
            result = (inCodeBlock.get()) ? unescapeSymbols(string) : string;
        }
//        System.err.println(" > " + result);
        return result;
    }

    private String unescapeSymbols(String input) {
        return input
            .replaceAll("&lt;", "<")
            .replaceAll("&gt;", ">")
            .replaceAll("&amp;", "&");
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

}
