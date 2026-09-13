package com.weaponsregistration.disposal.controller;

import com.weaponsregistration.core.controller.CoreExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = DisposalController.class)
public class DisposalExceptionHandler extends CoreExceptionHandler {}
