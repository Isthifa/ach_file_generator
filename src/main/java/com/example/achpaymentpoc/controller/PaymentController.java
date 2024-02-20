package com.example.achpaymentpoc.controller;

import com.example.achpaymentpoc.dto.ACHPaymentRequest;
import com.example.achpaymentpoc.services.impl.ACHServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private ACHServiceImpl achService;


    @Operation(summary = "Generate NACHA file format json", description = "Generate NACHA file format json from the given ACH payment requests.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "NACHA file format JSON generated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"originatorCName\": \"Company A\", \"originatorRoutingNumber\": \"123456789\", \"originatorAccountNumber\": \"987654321\", \"receiverName\": \"Vendor B\", \"receiverRoutingNumber\": \"987654321\", \"receiverAccountNumber\": \"123456789\", \"amount\": 1000.00, \"transactionType\": \"debit\", \"originatorBDestination\": \"USA Bank\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Failed to generate NACHA file"
            )
    })
    @PostMapping("/ach")
    public ResponseEntity<String> generateNACHAFile(@RequestBody List<ACHPaymentRequest> paymentRequests) {
        try {
            // Generate NACHA file format
            String nachaFile = achService.generateACHFile(paymentRequests);
            // Return the NACHA file as a response
            return ResponseEntity.ok(nachaFile);
        } catch (Exception e) {
            // Handle exceptions and return an error response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to generate NACHA file");
        }
    }

    @Operation(summary = "Generate NACHA file format in text file", description = "Generate NACHA file format in text file from the given ACH payment requests.")
    @PostMapping("/generate")
    public String generateTextFile(@RequestBody List<ACHPaymentRequest> paymentRequests) {
        String filePath = "achFile.txt";
        try {
            // Generate NACHA file format
            achService.writeACHFile(paymentRequests, filePath);
            // Return the file path
            return filePath;
        } catch (Exception e) {
            // Handle exceptions and return an error response
            return "Failed to generate NACHA file";
        }
    }

    @Operation(summary = "Validate ACH payment request", description = "Validate the given ACH payment request.")
    @PostMapping("/validate")
    public ResponseEntity<String> validateACHPaymentRequest(@RequestBody List<ACHPaymentRequest> achPaymentRequest) {
        // Validate the ACH payment request
        return ResponseEntity.ok(achService.validateACHPaymentRequest(achPaymentRequest).getBody());
    }

    @Operation(summary = "upload ACH payment file to the server", description = "upload ACH payment file to the server")
    @PostMapping("/upload")
    public ResponseEntity<String> uploadACHFile(@RequestBody FileUploadRequest request) throws IOException {
        try {
            achService.uploadFileToServer(request.getFilePath());
            return ResponseEntity.ok("ACH file uploaded successfully to the server");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload ACH file to the server");
        }
    }
}

//    @PostMapping("/ach")
//    public ResponseEntity<?> createACHPayment(@RequestBody List<ACHPaymentRequest> request) {
//        return ResponseEntity.ok(achService.generateACHFile(request));
//    }
//
//    @GetMapping("/ach/{id}")
//    public ResponseEntity<?> getACHPayment(@PathVariable String id) {
//        return ResponseEntity.ok(achService.getPayment(id));
//    }


