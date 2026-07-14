package com.space.space_bundle.controller;

import com.space.space_bundle.dto.checkerport.CheckerPortArcRequest;
import com.space.space_bundle.dto.checkerport.CheckerPortCorrectionRequest;
import com.space.space_bundle.dto.checkerport.CheckerPortVoucherRequest;
import com.space.space_bundle.entity.ResultCheckerPricing;
import com.space.space_bundle.entity.ResultsTransaction;
import com.space.space_bundle.service.ResultsCheckerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/results-checker")
@RequiredArgsConstructor
public class ResultsCheckerController {

    private final ResultsCheckerService resultsCheckerService;

    // Ideally extract userId from SecurityContext, for now we can pass null or parse it
    @PostMapping("/vouchers")
    public ResponseEntity<ResultsTransaction> buyVoucher(@RequestBody CheckerPortVoucherRequest request) {
        return ResponseEntity.ok(resultsCheckerService.buyVoucher(request, null));
    }

    @PostMapping("/checks")
    public ResponseEntity<ResultsTransaction> checkResult(@RequestBody CheckerPortArcRequest request) {
        return ResponseEntity.ok(resultsCheckerService.checkResult(request, null));
    }

    @PostMapping("/checks/{referenceId}/correction")
    public ResponseEntity<ResultsTransaction> submitCorrection(
            @PathVariable String referenceId,
            @RequestBody CheckerPortCorrectionRequest request) {
        return ResponseEntity.ok(resultsCheckerService.submitCorrection(referenceId, request));
    }

    @GetMapping("/transactions/{referenceId}")
    public ResponseEntity<ResultsTransaction> getTransaction(@PathVariable String referenceId) {
        return resultsCheckerService.getTransaction(referenceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pricing")
    public ResponseEntity<Iterable<ResultCheckerPricing>> getPricing() {
        return ResponseEntity.ok(resultsCheckerService.getAllPricing());
    }
}
