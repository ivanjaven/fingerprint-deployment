package com.example.fingerprint_service.service;

import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.digitalpersona.uareu.Engine;
import com.digitalpersona.uareu.Fmd;
import com.digitalpersona.uareu.UareUGlobal;
import com.example.fingerprint_service.model.ResidentAuth;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FingerprintService {
  private static final Logger logger = LoggerFactory.getLogger(FingerprintService.class);
  private static final int TARGET_FALSEMATCH_RATE = Engine.PROBABILITY_ONE / 100000;

  private final DatabaseService databaseService;
  private final Engine engine;

  @Autowired
  public FingerprintService(DatabaseService databaseService) {
    this.databaseService = databaseService;
    this.engine = UareUGlobal.GetEngine();
  }

  public ResidentAuth identifyUser(String fingerprintRawJson) {
    try {
      logger.info("Starting user identification process");

      // Create FMD from the received raw data JSON
      Fmd capturedFmd = createFmdFromRawJson(fingerprintRawJson);
      if (capturedFmd == null) {
        logger.error("Failed to create valid FMD from provided data");
        return null;
      }

      List<ResidentAuth> residents = databaseService.getAllResidents();
      logger.info("Fetched {} residents from database for comparison", residents.size());

      for (ResidentAuth resident : residents) {
        try {
          String storedFingerprintJson = resident.getFingerprintBase64();
          if (storedFingerprintJson == null || storedFingerprintJson.trim().isEmpty()) {
            logger.debug("Skipping resident {} - no stored fingerprint", resident.getResidentId());
            continue;
          }

          Fmd storedFmd = createFmdFromRawJson(storedFingerprintJson);
          if (storedFmd == null) {
            logger.warn("Invalid stored fingerprint for resident: {}", resident.getResidentId());
            continue;
          }

          int falsematch_rate = engine.Compare(capturedFmd, 0, storedFmd, 0);
          logger.debug("Comparison result for resident {}: falsematch_rate = {}, target = {}",
              resident.getResidentId(), falsematch_rate, TARGET_FALSEMATCH_RATE);

          if (falsematch_rate < TARGET_FALSEMATCH_RATE) {
            logger.info("Match found! Resident ID: {}", resident.getResidentId());
            return resident;
          }
        } catch (Exception e) {
          logger.error("Error comparing fingerprints for resident {}: {}",
              resident.getResidentId(), e.getMessage());
        }
      }

      logger.info("No matching resident found");
      return null;

    } catch (Exception e) {
      logger.error("Unexpected error during fingerprint identification: {}", e.getMessage());
      return null;
    }
  }

  private Fmd createFmdFromRawJson(String rawJson) {
    try {
      logger.info("Starting FMD creation from raw data");
      logger.info("Raw data preview: {}", rawJson.substring(0, Math.min(100, rawJson.length())));

      ObjectMapper mapper = new ObjectMapper();
      JsonNode rootNode = mapper.readTree(rawJson);

      // Get the base64url encoded data string
      String base64UrlData = rootNode.get("Data").asText();

      // Convert base64url to standard base64
      String standardBase64 = base64UrlData
          .replace('_', '+')
          .replace('-', '/')
          .replace(".", "=");

      // Decode the inner JSON string
      String decodedJsonString = new String(Base64.getDecoder().decode(standardBase64));

      JsonNode dataNode = mapper.readTree(decodedJsonString);
      logger.info("Decoded inner JSON structure: {}",
          decodedJsonString.substring(0, Math.min(100, decodedJsonString.length())));

      // Format info extraction
      JsonNode formatNode = dataNode.get("Format");
      if (formatNode == null) {
        logger.error("Format node not found in decoded JSON structure");
        return null;
      }

      int width = formatNode.get("iWidth").asInt();
      int height = formatNode.get("iHeight").asInt();
      int dpi = formatNode.get("iXdpi").asInt();

      logger.info("Extracted dimensions - Width: {}, Height: {}, DPI: {}", width, height, dpi);

      // Get and convert the inner base64url image data
      String base64UrlImageData = dataNode.get("Data").asText();
      String standardBase64ImageData = base64UrlImageData
          .replace('_', '+')
          .replace('-', '/')
          .replace(".", "=");

      byte[] imageData = Base64.getDecoder().decode(standardBase64ImageData);
      logger.info("Decoded image data length: {}", imageData.length);

      // Create FMD
      Fmd fmd = engine.CreateFmd(
          imageData, // raw image data
          width, // image width
          height, // image height
          dpi, // resolution
          0, // purpose
          0, // finger position
          Fmd.Format.ANSI_378_2004 // format
      );

      logger.info("Successfully created FMD");
      return fmd;

    } catch (Exception e) {
      logger.error("Error creating FMD from raw JSON: {}", e.getMessage());
      logger.error("Stack trace:", e);
      return null;
    }
  }

  public boolean verifyFingerprint(String storedFingerprintJson, String capturedFingerprintJson) {
    try {
      Fmd storedFmd = createFmdFromRawJson(storedFingerprintJson);
      Fmd capturedFmd = createFmdFromRawJson(capturedFingerprintJson);

      if (storedFmd == null || capturedFmd == null) {
        logger.error("Failed to create valid FMDs for comparison");
        return false;
      }

      int falsematch_rate = engine.Compare(capturedFmd, 0, storedFmd, 0);
      logger.debug("Verification result: falsematch_rate = {}, target = {}",
          falsematch_rate, TARGET_FALSEMATCH_RATE);

      return falsematch_rate < TARGET_FALSEMATCH_RATE;

    } catch (Exception e) {
      logger.error("Error in verification process: {}", e.getMessage());
      return false;
    }
  }

  public boolean validateFingerprintData(String fingerprintRawJson) {
    try {
      return createFmdFromRawJson(fingerprintRawJson) != null;
    } catch (Exception e) {
      logger.error("Error validating fingerprint data: {}", e.getMessage());
      return false;
    }
  }
}
