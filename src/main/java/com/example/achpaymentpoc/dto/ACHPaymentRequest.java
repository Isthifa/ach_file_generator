package com.example.achpaymentpoc.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ACHPaymentRequest {

    private String originatorCName;
    private long originatorRoutingNumber;
    private long originatorAccountNumber;
    private String receiverName;
    private String receiverRoutingNumber;
    private String receiverAccountNumber;
    private double amount;
    private String transactionType;
    private String originatorBDestination;
}