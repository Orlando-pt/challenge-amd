package com.db.awmd.challenge.exception;

public class TransferNoAmountException extends RuntimeException{

    public TransferNoAmountException(String message) {
      super(message);
    }
}
