package com.weaponsregistration.custody.controller;

import com.weaponsregistration.core.controller.CoreExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CustodyController.class)
public class CustodyExceptionHandler extends CoreExceptionHandler {}
