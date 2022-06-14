package org.jack;

import lombok.ToString;

public record UserRequest(Long orderId, Long userId, Integer requestAmount) {
}
