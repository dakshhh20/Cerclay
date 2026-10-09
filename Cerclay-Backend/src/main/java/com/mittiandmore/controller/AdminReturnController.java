package com.mittiandmore.controller;

import com.mittiandmore.dto.*;
import com.mittiandmore.service.RefundService;
import com.mittiandmore.service.ReturnService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/returns")
public class AdminReturnController {

    private final ReturnService returns;
    private final RefundService refunds;

    public AdminReturnController(ReturnService returns, RefundService refunds) {
        this.returns = returns;
        this.refunds = refunds;
    }

    @GetMapping
    public ResponseEntity<List<ReturnResponse>> all() {
        return ResponseEntity.ok(returns.all());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReturnResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(returns.get(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ReturnResponse> approve(
        @PathVariable Long id,
        @Valid @RequestBody(required = false) ReturnDecisionRequest req
    ) {
        return ResponseEntity.ok(returns.approve(id, req == null ? null : req.getNote()));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ReturnResponse> reject(
        @PathVariable Long id,
        @Valid @RequestBody(required = false) ReturnDecisionRequest req
    ) {
        return ResponseEntity.ok(returns.reject(id, req == null ? null : req.getNote()));
    }

    @PostMapping("/{id}/received")
    public ResponseEntity<ReturnResponse> received(
        @PathVariable Long id,
        @Valid @RequestBody(required = false) ReturnDecisionRequest req
    ) {
        return ResponseEntity.ok(returns.markReceived(id, req == null ? null : req.getNote()));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<RefundResponse> refund(@PathVariable Long id, @Valid @RequestBody RefundRequest req) {
        return ResponseEntity.ok(refunds.create(id, req));
    }

    @PostMapping("/refunds/{refundId}/manual-processed")
    public ResponseEntity<RefundResponse> manualProcessed(@PathVariable Long refundId) {
        return ResponseEntity.ok(refunds.markManualProcessed(refundId));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ReturnResponse> close(
        @PathVariable Long id,
        @Valid @RequestBody(required = false) ReturnDecisionRequest req
    ) {
        return ResponseEntity.ok(returns.markClosed(id, req == null ? null : req.getNote()));
    }

    @GetMapping("/refunds")
    public ResponseEntity<List<RefundResponse>> allRefunds() {
        return ResponseEntity.ok(refunds.all());
    }
}
