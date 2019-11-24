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
public class CodeBlockPreProcessor extends Preprocessor {

    private static final String CODE_START = "(.*)(\\[source)(.*\\])(.*)";
    private static final Pattern START_PATTERN = Pattern.compile(CODE_START, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Override
    public void process (Document document, PreprocessorReader reader) {
        List<String> lines = reader.readLines();
        List<String> newLines = new ArrayList<>();

        lines.stream().forEach(line -> {
            Matcher m = START_PATTERN.matcher(line);
            if (m.matches()) {
//                String newLine = m.group(1) + "[code" + m.group(3) + m.group(4);
//                log.trace("> old: {}; new: {}", line, newLine);
//                newLines.add(newLine);
                newLines.add(line.replace(m.group(2), "[code"));
            } else {
                newLines.add(line);
            }
        });
        reader.restoreLines(newLines);
    }
}
