package org.jack;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class RequestPromise implements Serializable {
    @Serial
    private static final long serialVersionUID = 1910280669511856835L;

    private final UserRequest request;
    private RequestResult result;
}
