package com.comandos.disposal.controller;

import com.comandos.core.controller.CoreExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = DisposalController.class)
public class DisposalExceptionHandler extends CoreExceptionHandler {}
