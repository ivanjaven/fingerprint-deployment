package com.example.fingerprint_service.model;

public class ResidentAuth {
  private Long residentId;
  private String fullName;
  private String fingerprintBase64;
  private Long authId;
  private String username;
  private String role;

  // Default constructor
  public ResidentAuth() {
  }

  // Getters and setters
  public Long getResidentId() {
    return residentId;
  }

  public void setResidentId(Long residentId) {
    this.residentId = residentId;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getFingerprintBase64() {
    return fingerprintBase64;
  }

  public void setFingerprintBase64(String fingerprintBase64) {
    this.fingerprintBase64 = fingerprintBase64;
  }

  public Long getAuthId() {
    return authId;
  }

  public void setAuthId(Long authId) {
    this.authId = authId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }
}
