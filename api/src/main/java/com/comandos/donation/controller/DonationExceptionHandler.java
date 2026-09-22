package com.comandos.donation.controller;
import com.comandos.core.controller.CoreExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice(assignableTypes=DonationController.class) public class DonationExceptionHandler extends CoreExceptionHandler {}
