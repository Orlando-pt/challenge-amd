package com.db.awmd.challenge.exception;

public class TransferSameAccountException extends RuntimeException {

  public TransferSameAccountException(String message) {
    super(message);
  }
}
