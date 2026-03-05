package com.thinkjava.platform.user;

public class EmailAlreadyUsedException extends RuntimeException {
  public EmailAlreadyUsedException() {
    super("Email already used");
  }
}