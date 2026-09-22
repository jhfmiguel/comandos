package com.weaponsregistration.audit.controller;

import com.weaponsregistration.core.controller.CoreExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuditController.class)
public class AuditExceptionHandler extends CoreExceptionHandler {}
