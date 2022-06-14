package org.jack;

import java.util.Optional;
import java.util.StringJoiner;

public record RequestResult(Long userId, Boolean isSuccess, Optional<String> message) {
    public RequestResult(Long userId, Boolean isSuccess) {
        this(userId, isSuccess, Optional.empty());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RequestResult.class.getSimpleName() + "[", "]")
                .add("isSuccess=" + isSuccess)
                .add("message=" + message.orElse(""))
                .toString();
    }
}
