package com.mittiandmore.controller;

import com.mittiandmore.config.DualClientSecurityContextRepository;
import com.mittiandmore.dto.AdminLoginRequest;
import com.mittiandmore.dto.AdminLoginResponse;
import com.mittiandmore.entity.Admin;
import com.mittiandmore.repository.AdminRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final AuthenticationManager authenticationManager;
    private final AdminRepository adminRepository;

    private final SecurityContextRepository securityContextRepository =
        new DualClientSecurityContextRepository().adminRepository();

    public AdminAuthController(
        @Qualifier("adminAuthenticationManager") AuthenticationManager authenticationManager,
        AdminRepository adminRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.adminRepository = adminRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
        @RequestBody AdminLoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            Admin admin = adminRepository.findByEmail(request.getEmail()).orElseThrow();

            AdminLoginResponse response = new AdminLoginResponse(
                admin.getId(),
                admin.getName(),
                admin.getEmail(),
                admin.getRole()
            );

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid admin email or password");
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin account is disabled");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("CERCLAY_ADMIN_SECURITY_CONTEXT");
        }

        return ResponseEntity.ok("Admin logout successful");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Admin not logged in");
        }

        Admin admin = adminRepository.findByEmail(authentication.getName()).orElse(null);

        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Admin not found");
        }

        AdminLoginResponse response = new AdminLoginResponse(
            admin.getId(),
            admin.getName(),
            admin.getEmail(),
            admin.getRole()
        );

        return ResponseEntity.ok(response);
    }
}
