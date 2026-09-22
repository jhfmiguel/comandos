package com.comandos.maintenance.controller;import com.comandos.core.controller.CoreExceptionHandler;import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice(assignableTypes=MaintenanceController.class)public class MaintenanceExceptionHandler extends CoreExceptionHandler{}
