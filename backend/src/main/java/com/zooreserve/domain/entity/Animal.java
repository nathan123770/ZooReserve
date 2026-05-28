package com.zooreserve.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("animal")
public class Animal extends BaseEntity {
  private Long zoneId;
  private String name;
  private String species;
  private String description;
  private String mediaUrl;
  private String status;

  public Long getZoneId() {
    return zoneId;
  }

  public void setZoneId(Long zoneId) {
    this.zoneId = zoneId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSpecies() {
    return species;
  }

  public void setSpecies(String species) {
    this.species = species;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getMediaUrl() {
    return mediaUrl;
  }

  public void setMediaUrl(String mediaUrl) {
    this.mediaUrl = mediaUrl;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
