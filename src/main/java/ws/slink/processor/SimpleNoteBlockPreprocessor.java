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
public class SimpleNoteBlockPreprocessor extends Preprocessor {
    private static final String CODE_START = "(NOTE: )(.*)";
    private final Pattern p = Pattern.compile(CODE_START, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Override
    public void process(Document document, PreprocessorReader preprocessorReader) {
        List<String> lines = preprocessorReader.readLines();
        List<String> newLines = new ArrayList<>();

        lines.forEach(line -> {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                String newLine = "[simple_note] " + m.group(2);
                log.trace("> old: {}; new: {}", line, newLine);
                newLines.add(newLine);
            } else {
                newLines.add(line);
            }
        });
        preprocessorReader.restoreLines(newLines);
    }
}
