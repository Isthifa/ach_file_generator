package com.example.achpaymentpoc.services;

import com.example.achpaymentpoc.dto.ACHPaymentRequest;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;

public interface ACHService {

   String generateACHFile(List<ACHPaymentRequest> paymentRequests);

   void writeACHFile(List<ACHPaymentRequest> paymentRequests, String filePath) throws IOException;

   ResponseEntity<String> validateACHPaymentRequest(List<ACHPaymentRequest> achPaymentRequest);

   void uploadFileToServer(String filePath) throws IOException;
}
