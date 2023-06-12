package com.ngmatthew227;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.session.MapSession;
import org.springframework.session.PrincipalNameIndexResolver;
import org.springframework.session.Session;

public class CassandraHttpSession implements Session {

  private static final PrincipalNameIndexResolver<Session> PRINCIPAL_NAME_RESOLVER = new PrincipalNameIndexResolver<>();

  private String savedPrincipalName = null;
  private String currentPrincipalName = null;
  private final MapSession cached;

  CassandraHttpSession() {
    cached = new MapSession();
  }

  CassandraHttpSession(String id) {
    cached = new MapSession(id);
  }

  private void onMaybeChangedPrincipalName() {
    this.currentPrincipalName = PRINCIPAL_NAME_RESOLVER.resolveIndexValueFor(cached);
  }

  public String getSavedPrincipalName() {
    return savedPrincipalName;
  }

  public void setSavedPrincipalName(String savedPrincipalName) {
    this.savedPrincipalName = savedPrincipalName;
  }

  public String getCurrentPrincipalName() {
    return currentPrincipalName;
  }

  public void setCurrentPrincipalName(String currentPrincipalName) {
    this.currentPrincipalName = currentPrincipalName;
  }

  public void onSave() {
    this.savedPrincipalName = this.currentPrincipalName;
  }

  @Override
  public String getId() {
    return cached.getId();
  }

  @Override
  public String changeSessionId() {
    return cached.changeSessionId();
  }

  @Override
  public <T> T getAttribute(String attributeName) {
    return cached.getAttribute(attributeName);
  }

  public Map<String, Object> getAttributes() {
    Set<String> attrNames = getAttributeNames();
    Map<String, Object> attrs = new HashMap<>(attrNames.size());
    for (String attrName : attrNames) {
      attrs.put(attrName, getAttribute(attrName));
    }
    return attrs;
  }

  @Override
  public Set<String> getAttributeNames() {
    return cached.getAttributeNames();
  }

  @Override
  public void setAttribute(String attributeName, Object attributeValue) {
    cached.setAttribute(attributeName, attributeValue);
    onMaybeChangedPrincipalName();
  }

  @Override
  public void removeAttribute(String attributeName) {
    cached.removeAttribute(attributeName);
    onMaybeChangedPrincipalName();
  }

  public void setCreationTime(Instant createTime) {
    cached.setCreationTime(createTime);
  }

  @Override
  public Instant getCreationTime() {
    return cached.getCreationTime();
  }

  @Override
  public void setLastAccessedTime(Instant lastAccessedTime) {
    cached.setLastAccessedTime(lastAccessedTime);
  }

  @Override
  public Instant getLastAccessedTime() {
    return cached.getLastAccessedTime();
  }

  @Override
  public Duration getMaxInactiveInterval() {
    return cached.getMaxInactiveInterval();
  }

  @Override
  public void setMaxInactiveInterval(Duration interval) {
    cached.setMaxInactiveInterval(interval);
  }

  @Override
  public boolean isExpired() {
    return cached.isExpired();
  }

  @Override
  public String toString() {
    Map<String, Object> attributes = getAttributes();

    return MessageFormat.format(
        "CassandraHttpSession [id= {0}, creationTime={1}, lastAccessTime={2}, maxInactiveInterval={3}, attributes={4}]",
        this.getId(),
        this.getCreationTime(), this.getLastAccessedTime(), this.getMaxInactiveInterval(), attributes);
  }

}
