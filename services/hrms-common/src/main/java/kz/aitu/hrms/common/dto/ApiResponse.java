package kz.aitu.hrms.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private Object errors;
    private LocalDateTime timestamp;

    private ApiResponse(boolean success, String message, T data, Object errors) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errors = errors;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Success", data, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, "Created successfully", data, null);
    }

    public static ApiResponse<Void> noContent(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    public static ApiResponse<Void> error(String message, Object errors) {
        return new ApiResponse<>(false, message, null, errors);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, null, null);
    }
}
