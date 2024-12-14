package com.example.fingerprint_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fingerprint_service.model.ErrorResponse;
import com.example.fingerprint_service.model.FingerprintRequest;
import com.example.fingerprint_service.model.ResidentAuth;
import com.example.fingerprint_service.service.FingerprintService;

@RestController
@RequestMapping("/api/fingerprint")
public class FingerprintController {
  private static final Logger logger = LoggerFactory.getLogger(FingerprintController.class);
  private final FingerprintService fingerprintService;

  @Autowired
  public FingerprintController(FingerprintService fingerprintService) {
    this.fingerprintService = fingerprintService;
  }

  @PostMapping("/identify")
  public ResponseEntity<?> identifyUser(@RequestBody FingerprintRequest request) {
    try {
      logger.info("Received identification request");

      if (request.getFingerprintData() == null) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("Fingerprint data is required"));
      }

      logger.info("Processing fingerprint data of length: {}",
          request.getFingerprintData().length());

      ResidentAuth resident = fingerprintService.identifyUser(request.getFingerprintData());
      if (resident != null) {
        return ResponseEntity.ok(resident);
      }
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new ErrorResponse("No matching user found"));
    } catch (Exception e) {
      logger.error("Error during identification", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new ErrorResponse(e.getMessage()));
    }
  }
}
