package ws.slink.processor;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TreeMacroPreProcessor extends Preprocessor {

    private static final String TREE_MACRO = "^(//)(.*)(pagetree::)(.*)";
    private static final Pattern TREE_PATTERN = Pattern.compile(TREE_MACRO, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Override
    public void process (Document document, PreprocessorReader reader) {
        List<String> lines = reader.readLines();
        List<String> newLines = new ArrayList<>();
        lines.stream().forEach(line -> {
            Matcher m = TREE_PATTERN.matcher(line);
            if (m.matches()) {
                newLines.add(line.replace(m.group(1)+m.group(2), ""));
            } else {
                newLines.add(line);
            }
        });
        reader.restoreLines(newLines);
    }

}
