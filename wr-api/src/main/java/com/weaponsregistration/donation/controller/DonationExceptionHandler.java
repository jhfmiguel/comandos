package com.weaponsregistration.donation.controller;
import com.weaponsregistration.core.controller.CoreExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice(assignableTypes=DonationController.class) public class DonationExceptionHandler extends CoreExceptionHandler {}
