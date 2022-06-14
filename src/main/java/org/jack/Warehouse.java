package org.jack;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class Warehouse {
    public static final int ENQUEUE_TIMEOUT = 500;
    public static final int USER_REQUEST_PROCESS_TIMEOUT = ENQUEUE_TIMEOUT;
    public static final int BATCH_SIZE = 6;
    private Integer stock = 5;
    /**
     * Simulate for Message Queue
     */
    private final BlockingQueue<RequestPromise> queue = new LinkedBlockingQueue<>(5);

    public Warehouse() {
        process();
    }

    /**
     * Simulate for Message Queue - Producer
     *
     * @param request
     * @return
     */
    public RequestResult register(UserRequest request) {
        var requestPromise = new RequestPromise(request);
        boolean enqueueSuccess;
        try {
            enqueueSuccess = queue.offer(requestPromise, ENQUEUE_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new RequestResult(request.userId(), false, Optional.of("Internal server error, unable to use queue."));
        }
        if (!enqueueSuccess) {
            return new RequestResult(request.userId(), false, Optional.of("System busy, unable to process this user request."));
        }

        synchronized (requestPromise) {
            try {
                requestPromise.wait(USER_REQUEST_PROCESS_TIMEOUT);
                if (requestPromise.getResult() == null) {
                    return new RequestResult(request.userId(), false, Optional.of("Process timeout."));
                } else {
                    return requestPromise.getResult();
                }
            } catch (InterruptedException e) {
                return new RequestResult(request.userId(), false, Optional.of("Process has been interrupted."));
            }
        }
    }

    /**
     * Simulate as Message Queue - Consumer
     *
     * Only single thread, but scalable.
     * Support to combination for multiple request processing.
     */
    private void process() {
        var executor = Executors.newCachedThreadPool();
        executor.submit(() -> {
            while (true) {
                if (queue.isEmpty()) {
                    try {
                        Thread.sleep(10);
                        continue;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }

                var tempBatchAmount = new AtomicInteger();
                IntSupplier afterBatchStockSupplier = () -> stock - tempBatchAmount.get();

                var batchPartitions = IntStream.range(0, BATCH_SIZE)
                        .mapToObj(i -> queue.poll())
                        .filter(Objects::nonNull)
                        .collect(Collectors.partitioningBy(item -> {
                            var withInRange = afterBatchStockSupplier.getAsInt() - item.getRequest().requestAmount() >= 0;
                            if (withInRange) {
                                tempBatchAmount.addAndGet(item.getRequest().requestAmount());
                            }
                            return withInRange;
                        }));

                batchPartitions.get(false).forEach(item -> item.setResult(new RequestResult(item.getRequest().userId(), false, Optional.of("Requested amount is greater than stock. current stock: " + stock))));

                // Simulate the cost for DB recording
                try {
                    Thread.sleep(RandomGenerator.getDefault().nextInt(20, 100));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                stock = afterBatchStockSupplier.getAsInt();

                batchPartitions.get(true)
                        .forEach(requestPromise -> {
                            requestPromise.setResult(new RequestResult(requestPromise.getRequest().userId(), true));
                            synchronized (requestPromise) {
                                requestPromise.notify();
                            }
                        });
            }
        })
        ;
    }
}
