package com.dxfeed.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public class Document {

    private String inputFilename;
    private String outputFilename;
    private String title;
    private String space;
    private String contents;

    @Override
    public String toString() {
        return new StringBuilder()
                .append(title)
                .append(":")
                .append(space)
                .append(" (")
                .append(inputFilename)
                .append(":")
                .append(outputFilename)
                .append(")")
                .toString();
    }
}
