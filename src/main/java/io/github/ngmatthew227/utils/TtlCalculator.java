package io.github.ngmatthew227.utils;

import org.springframework.session.Session;

public class TtlCalculator {

  public static int calculateTtlInSeconds(long currentTime, Session session) {
    long secondsSinceAccess = (currentTime - session.getLastAccessedTime().toEpochMilli()) / 1000;
    long secondsToLive = session.getMaxInactiveInterval().getSeconds() - secondsSinceAccess;
    if (secondsToLive <= 0) {
      throw new IllegalArgumentException("Session has already expired");
    }
    return (int) secondsToLive;
  }
}
