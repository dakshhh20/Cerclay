package com.mittiandmore.service;

import com.mittiandmore.entity.Customer;
import com.mittiandmore.repository.CustomerRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Primary
public class CustomerUserDetailsService
        implements UserDetailsService {

    private final CustomerRepository customerRepository;

    public CustomerUserDetailsService(
            CustomerRepository customerRepository) {

        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Customer customer =
                customerRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new UsernameNotFoundException(
                                        "Customer not found"
                                )
                        );

        return User.builder()
                .username(customer.getEmail())
                .password(customer.getPassword())
                .roles(customer.getRole())
                .disabled(!customer.getActive())
                .build();
    }
}