package ws.slink.processor;

import lombok.extern.slf4j.Slf4j;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class SimpleNoteBlockPostProcessor extends Postprocessor {
    private static final String SIMPLE_NOTE = "(<p>)(\\[simple_note])([A-Za-z]*)(.*)(</p>)";
    private static final Pattern PATTERN = Pattern.compile(SIMPLE_NOTE, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private final StringBuilder stringBuilder = new StringBuilder();

    @Override
    public String process(Document document, String s) {
        return
            Arrays.stream(s.split("\n"))
                .map(this::processString)
                .collect(Collectors.joining("\n"));
    }

    private String processString(String string) {
        Matcher matcher = PATTERN.matcher(string);
        if (matcher.matches()) {
            return replace(matcher.group(4));
        }
        return string;
    }

    // see:
    //    https://confluence.atlassian.com/display/CONF55/Note+Macro
    //    https://stackoverflow.com/questions/30194918/how-do-you-insert-a-confluence-macro-into-a-page-created-by-the-confluence-rest
    private String replace(String s) {
        stringBuilder.setLength(0);
        stringBuilder
            .append("<ac:structured-macro ac:name=\"note\">")
            .append("<ac:parameter ac:name=\"icon\">false</ac:parameter>")
            .append("<ac:parameter ac:name=\"title\">Note</ac:parameter>")
            .append("<ac:rich-text-body>")
            .append(s)
            .append("</ac:rich-text-body>")
            .append("</ac:structured-macro>");
        return stringBuilder.toString();
    }
}
