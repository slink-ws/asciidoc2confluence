package ws.slink.processor;

import lombok.extern.slf4j.Slf4j;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ConfluenceLinkMacroPreProcessor extends Preprocessor {

    private static final String LINK_START = "(.*)(link:)(.*)(\\[)(.*)(\\])(.*)";
    private static final Pattern START_PATTERN = Pattern.compile(LINK_START, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Override
    public void process (Document document, PreprocessorReader reader) {
        List<String> lines = reader.readLines();
        List<String> newLines = new ArrayList<>();
        lines.stream().forEach(line -> {
            Matcher m = START_PATTERN.matcher(line);
            if (m.matches()) {
                newLines.add(line.replace(m.group(2), "cflink:"));
            } else {
                newLines.add(line);
            }
        });
        reader.restoreLines(newLines);
    }
}
