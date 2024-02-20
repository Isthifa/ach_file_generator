package com.example.achpaymentpoc.services.impl;

import com.example.achpaymentpoc.dto.ACHPaymentRequest;
import com.example.achpaymentpoc.services.ACHService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ACHServiceImpl implements ACHService {

    @Override
    public String generateACHFile(List<ACHPaymentRequest> paymentRequests) {
        StringBuilder nachaFileBuilder = new StringBuilder(); // Create a StringBuilder to store the NACHA file content

        // Generate File Header Record
        String fileHeaderRecord = generateFileHeaderRecord(paymentRequests.get(0));
        nachaFileBuilder.append(fileHeaderRecord).append("\n");

        int entryCount = 0; // Total number of entry detail records
        int batchCount = 0; // Total number of batches
        long entryHash = 0; // Sum of the routing numbers of all the entry detail records in the file
        int batchHeaderCount = 0; // Total number of batch header records
        int batchHeadFooterCount = 0;

        // Generate Batch Header, Entry Detail, and Batch Control Records for each batch
        for (ACHPaymentRequest request : paymentRequests) {
            // Generate Batch Header Record
            String batchHeaderRecord = generateBatchHeaderRecord(request, batchHeaderCount++);
            nachaFileBuilder.append(batchHeaderRecord).append("\n");
            // Generate Entry Detail Records
            List<String> entryDetailRecords = generateEntryDetailRecords(request,++batchCount);
            entryCount += entryDetailRecords.size();
            for(String entryDetails:entryDetailRecords){
                nachaFileBuilder.append(entryDetails).append("\n");
                String routingNumber = entryDetails.substring(3,11);
                entryHash += Long.parseLong(routingNumber);
            }
            entryHash %= 10000000000L;

            // Generate Batch Control Record
            String batchControlRecord = generateBatchControlRecord(request, entryDetailRecords.size(),entryHash,batchHeadFooterCount++);
            nachaFileBuilder.append(batchControlRecord).append("\n");
        }

        // Generate File Control Record
        String fileControlRecord = generateFileControlRecord(batchCount, entryCount,entryHash);
        nachaFileBuilder.append(fileControlRecord).append("\n");
        String padding = new String(new char[94]).replace("\0", "9");// 94 characters per line, 10 lines per block
        nachaFileBuilder.append(padding);

        return nachaFileBuilder.toString();
    }


    private static int fileIdModifierCounter = 0;
    private String generateFileHeaderRecord(ACHPaymentRequest request) {
        // Generate File Header Record with provided ACHPaymentRequest
        char fileIdModifier = (char) ('A' + fileIdModifierCounter++);
        if (fileIdModifier > 'Z') {
            fileIdModifier = 'A';
            fileIdModifierCounter = 0;
        }

        // Formatting the fields to fit the specifications
        String recordTypeCode = "1";
        String priorityCode = "01";
        long immediateDestination = request.getOriginatorRoutingNumber();
        long immediateOrigin = request.getOriginatorAccountNumber();
        Date date = new Date();
        int currentYear = date.getYear() - 100;
        int currentMonth = date.getMonth() + 1;
        int currentDay = date.getDate(); // Using getDate() instead of getDay()
        String fileCreationDate = String.format("%02d%02d%02d", currentYear, currentMonth, currentDay);
        String fileCreationTime = "0000";
        String fileIDModifier = String.valueOf(fileIdModifier);
        String recordSize = "094";
        String blockingFactor = "10";
        String formatCode = "1";
        String immediateBDestinationName = request.getOriginatorBDestination()+"              ";
        String immediateOriginName = request.getOriginatorCName()+"             ";
        String referenceCode = "00000000"; // 8 blank spaces

        // Using %010d for long values immediateDestination and immediateOrigin
        return String.format("%s%s %d %d%s%s%s%s%s%s%s %s %s",
                recordTypeCode, priorityCode, immediateDestination, immediateOrigin, fileCreationDate, fileCreationTime,
                fileIDModifier, recordSize, blockingFactor, formatCode, immediateBDestinationName, immediateOriginName, referenceCode);
    }





    private String generateBatchHeaderRecord(ACHPaymentRequest request, int batchHeaderCount) {
        // Generate Batch Header Record with provided ACHPaymentRequest and batch count
        String recordTypeCode = "5";
        String serviceClassCode = "220"; //it can be 200, 220, 225, 280 or 225 , 200 for credit, 220 for debit, 225 for mixed, 280 for prenote
        String companyName = request.getOriginatorCName()+"       ";
        String companyDiscretionaryData = "                    "; //optional field
        long companyIdentification = Long.parseLong("1"+request.getOriginatorAccountNumber()); // 9 digit routing number
        String standardEntryClassCode = "PPD"; // PPD, CCD, WEB, TEL, ARC, BOC, POP, RCK, IAT, MTE, POS, SHR, XCK used for different types of transactions like payroll, vendor payments, consumer payments, telephone initiated entries, internet initiated entries, accounts receivable entries, back office conversion, point of purchase, re-presented check entries, international ACH transactions, machine transfer entries, point of sale, and share draft presentment
        String companyEntryDescription = "ACH PAYMEN"; // 10 character description
        String companyDescriptiveDate = "220214"; // YYMMDD
        String effectiveEntryDate = "220214"; // YYMMDD
        String settlementDate = " "; // julian date setlement date filled by the bank
        String originatorStatusCode = "1"; // 1 for single entry, 2 for recurring, 3 for first in a series, 4 for final in a series
        String originatorDFIIdentification = String.valueOf(request.getOriginatorAccountNumber()).substring(0, 8); // 8 digit routing number
        String batchNumber = String.format("%07d", batchHeaderCount); // 7 digit batch number 0 and increment by 1 for each batch
        return String.format("%s%s%s%s%d%s%s %s%s %s%s%s%s",recordTypeCode,
                serviceClassCode, companyName, companyDiscretionaryData, companyIdentification,
                standardEntryClassCode, companyEntryDescription, companyDescriptiveDate, effectiveEntryDate,
                settlementDate, originatorStatusCode, originatorDFIIdentification, batchNumber);
    }

    private List<String> generateEntryDetailRecords(ACHPaymentRequest request, int batchCount) {
        // Generate Entry Detail Records for provided ACHPaymentRequest
        List<String> entryDetailRecords = new ArrayList<>();

        // Example Entry Detail Record
        String recordTypeCode = "6";
        String transactionCode = "22"; // 22 for credit, 27 for debit
        String receivingDFIIdentification = String.valueOf(request.getReceiverRoutingNumber()).substring(0, 8); // 8 digit routing number
        String checkDigit = String.valueOf(request.getReceiverRoutingNumber()).substring(8); // 1 digit routing number
        String receivingDFIAccountNumber = request.getReceiverAccountNumber()+"        "; // 17 digit account number
        String amount = String.format("%010d", (long) request.getAmount()); // 10 digit amount
        String individualIdentificationNumber = "123456789012345"; // 15 digit identification number
        String individualName = request.getReceiverName()+"              "; // 22 digit name
        String discretionaryData = "A1"; // 2 digit discretionary data
        String addendaRecordIndicator = "0"; // 1 digit addenda record indicator 0 for no addenda
        String traceNumber = String.valueOf(request.getOriginatorAccountNumber()).substring(0, 8) + String.format("%07d", batchCount); // 15 digit trace number

        entryDetailRecords.add(String.format("%s%s%s%s%s%s%s%s%s%s%s", recordTypeCode, transactionCode,
                receivingDFIIdentification, checkDigit, receivingDFIAccountNumber, amount,
                individualIdentificationNumber, individualName, discretionaryData, addendaRecordIndicator, traceNumber));

        return entryDetailRecords;
    }

    private String generateBatchControlRecord(ACHPaymentRequest request, int entryCount,long entryHashe,int batchHeadFooterCount) {
        // Generate Batch Control Record with provided ACHPaymentRequest and entry count
        String recordTypeCode = "8";
        String serviceClassCode = "220"; //it can be 200, 220, 225, 280 or 225 , 200 for credit, 220 for debit, 225 for mixed, 280 for prenote
        String entryAddendaCount = String.format("%06d",entryCount); // Example entry/addenda count (number of entry detail records in the batch
        String entryHash = String.format("%010d", entryHashe);; // Example entry hash (sum of the routing numbers of all the entry detail records in the batch)
        String totalDebitAmount = "000000000"; // Example total debit amount - 12 digit
        String totalCreditAmount = "123456789"; // Example total credit amount - 12 digit
        String companyIdentification = "0"+ request.getOriginatorAccountNumber(); // 8 digit routing number
        String messageAuthenticationCode = "                   "; // Optional field
        String reserved = "      "; // Blank field
        String originatingDFIIdentification = String.valueOf(request.getOriginatorAccountNumber()).substring(0, 8); // 8 digit routing number
        String batchNumber = String.format("%07d", batchHeadFooterCount); // 7 digit batch number 0 and increment by 1 for each batch

        return String.format("%s%s%s%s%012d%012d%s%s%s%s%s", recordTypeCode, serviceClassCode, entryAddendaCount,
                entryHash, Long.parseLong(totalDebitAmount), Long.parseLong(totalCreditAmount),
                companyIdentification, messageAuthenticationCode, reserved, originatingDFIIdentification, batchNumber);
    }


    private String generateFileControlRecord(int batchCount, int entryCount,long entryHashe) {
        // Generate File Control Record with provided batch and entry counts

        String recordTypeCode = "9";
        String batchCountString = String.format("%06d", batchCount); //number of batches
        String blockCount = "000001"; // number of blocks in the file 10 records per block so 1 block
        String entryAddendaCount = String.format("%08d", entryCount); // Example entry/addenda count
        String entryHash = String.format("%010d", entryHashe); // Example entry hash
        String totalDebitAmount = "000000000"; // Example total debit amount
        String totalCreditAmount = "123456789"; // Example total credit amount
        String reserved = "                                       "; // Reserved field with spaces (39 spaces) or constant 39 spaces blank
        return String.format("%s%s%s%s%s%012d%012d%s", recordTypeCode, batchCountString,blockCount, entryAddendaCount,
                entryHash, Long.parseLong(totalDebitAmount), Long.parseLong(totalCreditAmount), reserved);
    }




    public void writeACHFile(List<ACHPaymentRequest> paymentRequests, String filePath) throws IOException {
        // Generate ACH file content
        String achFileContent = generateACHFile(paymentRequests);

        // Write ACH file content to a text file
        Path path = Paths.get(filePath);
        Files.write(path, achFileContent.getBytes());

        System.out.println("ACH file written successfully to: " + filePath);
    }

    @Override
    public ResponseEntity<String> validateACHPaymentRequest(List<ACHPaymentRequest> achPaymentRequest) {
        if(achPaymentRequest.get(0).getOriginatorCName().isEmpty() || achPaymentRequest.get(0).getReceiverName().isEmpty() || achPaymentRequest.get(0).getReceiverRoutingNumber().isEmpty() || achPaymentRequest.get(0).getReceiverAccountNumber().isEmpty() || achPaymentRequest.get(0).getOriginatorBDestination().isEmpty() || achPaymentRequest.get(0).getOriginatorRoutingNumber() == 0 || achPaymentRequest.get(0).getOriginatorAccountNumber() == 0 || achPaymentRequest.get(0).getAmount() == 0 || achPaymentRequest.get(0).getTransactionType().isEmpty()){
            return ResponseEntity.badRequest().body("Invalid ACH Payment Request");
        }
        return ResponseEntity.ok("Valid ACH Payment Request");
    }

    @Override
    public void uploadFileToServer(String filePath) throws IOException {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA); // Set the content type to multipart/form-data

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(); //linked multi value map to store the file
        body.add("file", Files.readAllBytes(Paths.get(filePath))); // Add the file to the body

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String serverUrl = "http://dummybank.com/api/upload"; //

        restTemplate.postForEntity(serverUrl, requestEntity, String.class);
    }



}