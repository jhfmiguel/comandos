package com.weaponsregistration.consumption.controller;

import com.weaponsregistration.core.controller.CoreExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AmmunitionConsumptionController.class)
public class AmmunitionConsumptionExceptionHandler extends CoreExceptionHandler {}
