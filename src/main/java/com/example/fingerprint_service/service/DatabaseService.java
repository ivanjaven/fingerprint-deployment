package com.example.fingerprint_service.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.fingerprint_service.model.ResidentAuth;

@Service
public class DatabaseService {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

  @Autowired
  private JdbcTemplate jdbcTemplate;

  public List<ResidentAuth> getAllResidents() {
    logger.info("Fetching all residents with auth info from database");
    String sql = """
        SELECT r.resident_id, r.full_name, r.fingerprint_base64,
               a.auth_id, a.username, a.role
        FROM residents r
        JOIN auth a ON r.resident_id = a.resident_id
        WHERE r.fingerprint_base64 IS NOT NULL
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> {
      ResidentAuth resident = new ResidentAuth();
      resident.setResidentId(rs.getLong("resident_id"));
      resident.setFullName(rs.getString("full_name"));
      resident.setFingerprintBase64(rs.getString("fingerprint_base64"));
      resident.setAuthId(rs.getLong("auth_id"));
      resident.setUsername(rs.getString("username"));
      resident.setRole(rs.getString("role"));
      return resident;
    });
  }
}
