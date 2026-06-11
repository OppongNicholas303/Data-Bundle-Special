package com.space.space_bundle.controller;

import com.space.space_bundle.dto.ApiResponse;
import com.space.space_bundle.dto.MashupPackageResponse;
import com.space.space_bundle.service.MashupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/mashup")
@RequiredArgsConstructor
public class MashupController {

    private final MashupService mashupService;

    @GetMapping("/packages")
    public ResponseEntity<ApiResponse<List<MashupPackageResponse>>> getPackages() {
        return ResponseEntity.ok(ApiResponse.success(mashupService.getPublicPackages()));
    }
}
