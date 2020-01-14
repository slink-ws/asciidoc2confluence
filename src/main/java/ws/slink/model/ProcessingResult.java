package ws.slink.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.concurrent.atomic.AtomicInteger;

@Setter
@Getter
@Accessors(fluent = true)
public class ProcessingResult {

    public static final ProcessingResult SUCCESS = new ProcessingResult().success();
    public static final ProcessingResult FAILURE = new ProcessingResult().failure();
    public static final ProcessingResult HIDDEN  = new ProcessingResult().hidden();

    private AtomicInteger successful = new AtomicInteger(0);
    private AtomicInteger failed = new AtomicInteger(0);
    private AtomicInteger skippedHidden = new AtomicInteger(0);

    public ProcessingResult success() {
        successful.addAndGet(1);
        return this;
    }
    public ProcessingResult failure() {
        failed.addAndGet(1);
        return this;
    }
    public ProcessingResult hidden() {
        skippedHidden.addAndGet(1);
        return this;
    }
    public ProcessingResult merge(ProcessingResult result) {
        this.successful.addAndGet(result.successful.get());
        this.failed.addAndGet(result.failed.get());
        this.skippedHidden.addAndGet(result.skippedHidden.get());
        return this;
    }
}
