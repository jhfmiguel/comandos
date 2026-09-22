package com.comandos.consumption.controller;

import com.comandos.core.controller.CoreExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AmmunitionConsumptionController.class)
public class AmmunitionConsumptionExceptionHandler extends CoreExceptionHandler {}
