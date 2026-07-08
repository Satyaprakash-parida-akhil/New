package com.example.documentmanagement.controller;

import com.example.documentmanagement.dto.response.ApiResponse;
import com.example.documentmanagement.entity.User;
import com.example.documentmanagement.service.UserService;
import com.example.documentmanagement.util.MessageConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Tag(name = "Location Dropdowns", description = "APIs for dynamic cascading location dropdowns")
public class LocationController {

    private final UserService userService;

    @GetMapping("/states")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get distinct states from users")
    public ResponseEntity<ApiResponse<List<String>>> getStates() {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.STATES_FETCHED, userService.getDistinctStates()));
    }

    @GetMapping("/districts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get distinct districts for a state")
    public ResponseEntity<ApiResponse<List<String>>> getDistricts(@RequestParam String state) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.DISTRICTS_FETCHED, userService.getDistinctDistrictsByState(state)));
    }

    @GetMapping("/blocks")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get distinct blocks for a district")
    public ResponseEntity<ApiResponse<List<String>>> getBlocks(@RequestParam String district) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.BLOCKS_FETCHED, userService.getDistinctBlocksByDistrict(district)));
    }

    @GetMapping("/pincodes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get distinct PIN codes for a block")
    public ResponseEntity<ApiResponse<List<String>>> getPinCodes(@RequestParam String block) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.PIN_CODES_FETCHED, userService.getDistinctPinCodesByBlock(block)));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get users for a PIN code")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsersByPinCode(@RequestParam String pinCode) {
        List<UserDto> users = userService.getUsersByPinCode(pinCode)
                .stream()
                .map(u -> new UserDto(u.getId(), u.getFullName(), u.getPhoneNumber()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.USERS_RETRIEVED, users));
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    static class UserDto {
        private Long id;
        private String fullName;
        private String phoneNumber;
    }
}
