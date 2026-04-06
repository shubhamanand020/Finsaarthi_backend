package com.finsaarthi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ApplicationAlreadyExistsException extends RuntimeException {

    public ApplicationAlreadyExistsException(Long studentId, Long scholarshipId) {
        super("Student with ID '" + studentId +
              "' has already applied for scholarship with ID '" + scholarshipId + "'.");
    }
}