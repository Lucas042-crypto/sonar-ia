package br.com.younkoudev.SonarIA.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SonarResponse {

    private int total;
    private int p;
    private int ps;
    private Paging paging;
    private int effortTotal;
    private List<Issue> issues;

    @Getter
    @Setter
    public static class Paging {
        private int pageIndex;
        private int pageSize;
        private int total;
    }

    @Getter
    @Setter
    public static class Issue {
        private String key;
        private String rule;
        private String severity;
        private String component;
        private String project;
        private int line;
        private String hash;
        private TextRange textRange;
        private List<Flow> flows;
        private String status;
        private String message;
        private String effort;
        private String debt;
        private String author;
        private List<String> tags;
        private String creationDate;
        private String updateDate;
        private String type;
        private String scope;
        private boolean quickFixAvailable;
    }

    @Getter
    @Setter
    public static class Flow {
        private List<Location> locations;
    }

    @Getter
    @Setter
    public static class Location {
        private String component;
        private TextRange textRange;
        private String msg;
    }

    @Getter
    @Setter
    public static class TextRange {
        private int startLine;
        private int endLine;
        private int startOffset;
        private int endOffset;
    }
}
