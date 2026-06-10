package kz.aitu.hrms.user.exception;

/**
 * Thrown when a request conflicts with the current state of a resource —
 * maps to HTTP 409. Used e.g. when an admin tries to edit the immutable
 * SUPER_ADMIN role-permission row.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}