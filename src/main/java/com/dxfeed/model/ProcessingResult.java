package com.dxfeed.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Setter
@Getter
@Accessors(fluent = true)
public class ProcessingResult {

    public enum ResultType {
        RT_NONE,
        RT_FILE_SUCCESS, // successfully processed file
        RT_DIR_FAILURE,  // directory processing failure
        RT_FILE_FAILURE, // file processing failure
        RT_PUB_SUCCESS,  // successfully published
        RT_UPD_SUCCESS,  // successfully updated
        RT_PUB_FAILURE,  // publication failure
        RT_UPD_FAILURE,  // update failure
        RT_SKP_HIDDEN,   // skipped hidden file
        RT_DEL_SUCCESS,  // removal success
        RT_DEL_FAILURE,  // removal failure
//        RT_FILE_SKIPPED, // document skipped due to being "hidden"
//        RT_DIR_SKIPPED,  // if no .properies file found or no needed data exists in .properties file
//        RT_FILE_PRINTED  // file printed instead of being published
    }

    private Map<ResultType, AtomicInteger> results = new ConcurrentHashMap<>();

    public ProcessingResult() {
    }

    public ProcessingResult(ResultType type) {
        this.add(type);
    }

    public ProcessingResult add(ResultType key) {
        if (!results.containsKey(key))
            results.put(key, new AtomicInteger(0));
        results.get(key).addAndGet(1);
        return this;
    }

    public AtomicInteger get(ResultType key) {
        return results.getOrDefault(key, new AtomicInteger(0));
    }

    public ProcessingResult merge(ResultType other) {
        this.add(other);
        return this;
    }

    public ProcessingResult merge(ProcessingResult other) {
        Set keyset = new HashSet(this.results.keySet());
        keyset.addAll(other.results.keySet());
        keyset.stream().forEach(
                key -> {
                    AtomicInteger ai = this.results.getOrDefault(key, new AtomicInteger(0));
                    ai.addAndGet(other.results.getOrDefault(key, new AtomicInteger()).get());
                    this.results.put((ResultType)key, ai);
                }
        );
        return this;
    }

}
