package com.example.helloworld.api;

import org.hibernate.validator.constraints.Length;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Saying {
    private long id;

    @Length(max = 3)
    private String content;

    @JsonCreator
    public Saying(@JsonProperty("id") long id,
            @JsonProperty("content") String content) {
        this.id = id;
        this.content = content;
    }

    @JsonProperty
    public long getId() {
        return id;
    }

    @JsonProperty
    public String getContent() {
        return content;
    }
}
