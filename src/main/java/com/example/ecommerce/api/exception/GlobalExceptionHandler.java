package com.example.ecommerce.api.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String TRACE = "trace";
    @Value("${reflectoring.trace:false}")
    private boolean printStackTrace;

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<Object> handleInvalidAuthenticationCredentials(InvalidCredentialsException ex,
                                                                       WebRequest request) {
        return buildErrorResponse(ex,ex.getMessage(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<Object> handleConflictException(ConflictException ex,
                                                          WebRequest request) {
        return buildErrorResponse(ex, ex.getMessage(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Object> handleProductNotFoundException(ProductNotFoundException ex,
                                                                 WebRequest request) {
        return buildErrorResponse(ex, ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Object> handleCartItemNotFound(CartItemNotFoundException ex,
                                                         WebRequest request) {
        return buildErrorResponse(ex, ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Object> handleOrderNotFoundException(OrderNotFoundException ex,
                                                               WebRequest request) {
        return buildErrorResponse(ex, ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(EmptyCartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Object> handleEmptyCartException(EmptyCartException ex,
                                                           WebRequest request) {
        return buildErrorResponse(ex, ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @Override
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Validation error. Check 'errors' field for details.");


        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errorResponse.addValidationError(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity.unprocessableEntity().body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseEntity<Object> handleInputConstraintViolation(ConstraintViolationException ex,
                                                                 WebRequest request) {
        return buildErrorResponse(ex, ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY, request);
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Object> handleAllUncaughtExceptions(RuntimeException ex,
                                                              WebRequest request) {
        return buildErrorResponse(ex, "Unknown error occurred", HttpStatus.INTERNAL_SERVER_ERROR, request);
    }


    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        return buildErrorResponse(ex, ex.getMessage(), statusCode, request);
    }

    private ResponseEntity<Object> buildErrorResponse(Exception ex,
                                                      String message,
                                                      HttpStatusCode status,
                                                      WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(status.value(), message);

        if (printStackTrace && isTraceOn(request)) {
            errorResponse.setStackTrace(Arrays.toString(ex.getStackTrace()));
        }

        return ResponseEntity.status(status).body(errorResponse);

    }

    private boolean isTraceOn(WebRequest request) {
        String[] value = request.getParameterValues(TRACE);
        return Objects.nonNull(value) && value.length > 0 && value[0].contentEquals("true");
    }


}
