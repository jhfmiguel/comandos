package com.comandos.audit.controller;

import com.comandos.core.controller.CoreExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuditController.class)
public class AuditExceptionHandler extends CoreExceptionHandler {}
