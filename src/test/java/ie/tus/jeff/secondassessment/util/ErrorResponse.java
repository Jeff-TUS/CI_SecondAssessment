package ie.tus.jeff.secondassessment.util;

/**
 * Test-only POJO that mirrors the error body produced by {@code GlobalExceptionHandler}.
 *
 * Shape:
 * <pre>
 * {
 *   "timestamp": "2025-01-01T12:00:00",
 *   "status":    404,
 *   "error":     "Not Found",
 *   "message":   "Department not found with id: 99"
 * }
 * </pre>
 */
public class ErrorResponse {

    private String timestamp;
    private int status;
    private String error;
    private String message;

    public ErrorResponse() {}

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
