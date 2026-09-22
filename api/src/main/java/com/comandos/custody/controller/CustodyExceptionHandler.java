package com.comandos.custody.controller;

import com.comandos.core.controller.CoreExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CustodyController.class)
public class CustodyExceptionHandler extends CoreExceptionHandler {}
