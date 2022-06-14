package org.jack;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.stream.IntStream;

@Slf4j
public class Main {
    public static void main(String[] args) {
        var executor = Executors.newCachedThreadPool();
        var warehouse = new Warehouse();
        IntStream.range(0, 10)
                .mapToObj(i -> new UserRequest(i + 100L, (long) i, 1))
                .parallel()
                .map(request -> executor.submit(() -> warehouse.register(request)))
                .forEach(featureRequest -> {
                    try {
                        var result = featureRequest.get(3000, TimeUnit.MILLISECONDS);
                        log.debug("Thread-{}   User: {}  request result: {}", Thread.currentThread().getName(), result.userId(), result);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        log.error(e.getMessage(), e);
                    }
                });

    }
}
