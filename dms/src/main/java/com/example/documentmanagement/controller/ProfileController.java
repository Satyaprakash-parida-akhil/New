package com.example.documentmanagement.controller;

import com.example.documentmanagement.dto.response.ApiResponse;
import com.example.documentmanagement.dto.response.ProfileResponse;
import com.example.documentmanagement.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.documentmanagement.util.MessageConstants;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "User profile management")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Value("${app.upload.dir:uploads/profile-photos}")
    private String uploadDir;

    @Value("${app.file.storage.location:./uploads}")
    private String fileStorageLocation;

    @GetMapping("/me")
    @Operation(summary = "Get current user's profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        ProfileResponse profile = profileService.getMyProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.PROFILE_LOADED, profile));
    }

    @PutMapping(value = "/me", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Update profile info")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String dateOfBirth,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String block,
            @RequestParam(required = false) String town,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String village,
            @RequestParam(required = false) String landmark,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String pinCode,
            @RequestParam(required = false) String zone,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        ProfileResponse profile = profileService.updateProfile(
                userDetails.getUsername(), fullName, gender, dateOfBirth, 
                address, block, town, state, village, landmark, district, country, pinCode, zone, photo);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.PROFILE_UPDATED, profile));
    }

    @PostMapping("/pay")
    @Operation(summary = "Complete payment to activate profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> completePayment(
            @AuthenticationPrincipal UserDetails userDetails) {
        ProfileResponse profile = profileService.completePayment(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.PAYMENT_COMPLETED_ACTIVE, profile));
    }

    @PostMapping("/generate-referral")
    @Operation(summary = "Generate a referral code one-time only")
    public ResponseEntity<ApiResponse<ProfileResponse>> generateReferralCode(
            @AuthenticationPrincipal UserDetails userDetails) {
        ProfileResponse profile = profileService.generateReferralCode(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.Success.REFERRAL_CODE_GENERATED, profile));
    }

    @GetMapping("/photo/{filename:.+}")
    @Operation(summary = "Serve profile photo")
    public ResponseEntity<Resource> getPhoto(@PathVariable String filename) {
        try {
            // First look in profile photos upload directory
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                // Fallback to general files storage uploads directory
                filePath = Paths.get(fileStorageLocation).resolve(filename).normalize();
                resource = new UrlResource(filePath.toUri());
            }
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = "image/jpeg";
            if (filename.endsWith(".png")) contentType = "image/png";
            else if (filename.endsWith(".gif")) contentType = "image/gif";
            else if (filename.endsWith(".webp")) contentType = "image/webp";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
