package ws.slink.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang.StringUtils;

@Getter
@Setter
@Accessors(fluent = true)
public class Document {

    private String inputFilename;
    private String title;
    private String space;
    private String parent;
    private String contents;

    @Override
    public String toString() {
        return new StringBuilder()
            .append(title)
            .append(" (")
            .append(parent)
            .append("):")
            .append(space)
            .append(" (")
            .append(inputFilename)
            .append(")")
            .toString();
    }

    public boolean canPublish() {
        return StringUtils.isNotBlank(title) && StringUtils.isNotBlank(space);
    }
}
