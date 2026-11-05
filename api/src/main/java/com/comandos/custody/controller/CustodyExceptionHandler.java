package com.comandos.custody.controller;

import com.fariamiguel.app.core.controller.CoreExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CustodyController.class)
public class CustodyExceptionHandler extends CoreExceptionHandler {}
