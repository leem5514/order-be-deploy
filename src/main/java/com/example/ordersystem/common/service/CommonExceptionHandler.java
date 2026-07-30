package com.example.ordersystem.common.service;

import com.example.ordersystem.common.dto.CommonErrorDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.persistence.EntityNotFoundException;

@ControllerAdvice
public class CommonExceptionHandler {

    // controller 단에서 발생하는 모든 EntityNotFoundException catch
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<CommonErrorDto> entityNotFoundHandler(EntityNotFoundException e) {
        e.printStackTrace();
        CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.NOT_FOUND.value(), e.getMessage());
        return new ResponseEntity<>(commonErrorDto, HttpStatus.NOT_FOUND);
    }

    // 권한 부족(@PreAuthorize 등)으로 인한 접근 거부는 500이 아니라 403으로 응답해야 한다.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CommonErrorDto> accessDeniedHandler(AccessDeniedException e) {
        CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.FORBIDDEN.value(), "접근 권한이 없습니다.");
        return new ResponseEntity<>(commonErrorDto, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonErrorDto> illegalHandler(IllegalArgumentException e) {
        e.printStackTrace();
        CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        return new ResponseEntity<>(commonErrorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonErrorDto> validHandler(MethodArgumentNotValidException e) {
        e.printStackTrace();
        CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST.value(), e.getFieldError().getDefaultMessage());
        return new ResponseEntity<>(commonErrorDto, HttpStatus.BAD_REQUEST);
    }

    // 동시 요청끼리 겹쳐서(레이스) 외래키 제약 등 DB 무결성 제약을 위반한 경우, 500 대신
    // 409(Conflict)로 응답한다. 각 서비스에서 사전 체크로 최대한 걸러내지만, 그 체크와 실제 커밋
    // 사이의 아주 좁은 타이밍에 다른 트랜잭션이 끼어드는 잔여 레이스에 대한 최후 방어선이다.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<CommonErrorDto> dataIntegrityViolationHandler(DataIntegrityViolationException e) {
        e.printStackTrace();
        CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.CONFLICT.value(), "다른 요청과 겹쳐서 처리하지 못했습니다. 다시 시도해주세요.");
        return new ResponseEntity<>(commonErrorDto, HttpStatus.CONFLICT);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonErrorDto> exceptionHandler(Exception e) {
        e.printStackTrace();
        CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST.value(), "argument is not valid");
        return new ResponseEntity<>(commonErrorDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
