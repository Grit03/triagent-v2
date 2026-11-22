package io.triagent.common;

/** 도메인 예외가 이 클래스를 상속하면 {@link GlobalExceptionHandler}가 404로 변환합니다. 공통 핸들러를 고칠 필요가 없습니다. */
public abstract class NotFoundException extends RuntimeException {

    protected NotFoundException(String message) {
        super(message);
    }
}
