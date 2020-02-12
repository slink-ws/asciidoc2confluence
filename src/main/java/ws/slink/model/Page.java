package ws.slink.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@Accessors(fluent = true)
public class Page {

    String id;
    String title;
    int version;
    List<String> labels;

    public String toString() {
        return new StringBuilder()
            .append(id)
            .append(":")
            .append(title)
            .append(" v.")
            .append(version)
            .append(" ")
            .append(labels.toString())
            .toString()
        ;
    }
}
