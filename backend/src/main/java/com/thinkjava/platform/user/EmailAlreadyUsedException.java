package com.thinkjava.platform.user;

public class EmailAlreadyUsedException extends RuntimeException {
  public EmailAlreadyUsedException() {
    super("User with this email already exists");
  }
}