package io.ap2.a2a.extension.spec;

/**
 * Exception to indicate a general failure related to the A2A protocol.
 */
public class AP2Exception extends RuntimeException {

    /**
     * Constructs a new {@code AP2Exception} instance. The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public AP2Exception() {
    }

    /**
     * Constructs a new {@code AP2Exception} instance with an initial message. No cause is specified.
     *
     * @param msg the message
     */
    public AP2Exception(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code AP2Exception} instance with an initial cause. If a non-{@code null} cause
     * is specified, its message is used to initialize the message of this {@code AP2Exception}; otherwise
     * the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public AP2Exception(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code AP2Exception} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     */
    public AP2Exception(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}

