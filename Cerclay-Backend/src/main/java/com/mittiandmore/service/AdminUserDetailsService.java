package com.mittiandmore.service;

import com.mittiandmore.entity.Admin;
import com.mittiandmore.repository.AdminRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    public AdminUserDetailsService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Admin admin = adminRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));

        return User.builder()
            .username(admin.getEmail())
            .password(admin.getPassword())
            .roles(admin.getRole())
            .disabled(!admin.getActive())
            .build();
    }
}
