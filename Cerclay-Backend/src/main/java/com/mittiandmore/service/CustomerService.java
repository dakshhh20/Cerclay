package com.mittiandmore.service;

import com.mittiandmore.dto.CustomerResponse;
import com.mittiandmore.dto.CustomerUpdateRequest;
import com.mittiandmore.dto.RegisterRequest;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.repository.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder) {

        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /*
     * ADMIN
     *
     * Returns safe customer DTOs instead of exposing
     * Customer entities directly.
     */
    public List<CustomerResponse> getAllCustomerResponses() {

        return customerRepository.findAll()
                .stream()
                .map(CustomerResponse::fromEntity)
                .toList();
    }

    /*
     * INTERNAL
     *
     * Used by backend services when the actual
     * Customer entity is required.
     */
    public Customer getCustomerById(Long id) {

        return customerRepository.findById(id)
                .orElse(null);
    }

    /*
     * Convert Customer entity to a safe API response.
     */
    public CustomerResponse toResponse(Customer customer) {

        return CustomerResponse.fromEntity(customer);
    }

    public CustomerResponse setActive(Long id, boolean active) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        customer.setActive(active);
        return CustomerResponse.fromEntity(customerRepository.save(customer));
    }

    /*
     * CUSTOMER REGISTRATION
     *
     * Customer creation happens through the
     * authentication/registration flow.
     */
    public Customer registerCustomer(RegisterRequest request) {

        if (customerRepository
                .findByEmail(request.getEmail())
                .isPresent()) {

            throw new RuntimeException(
                    "Email already registered"
            );
        }

        if (customerRepository
                .findByPhone(request.getPhone())
                .isPresent()) {

            throw new RuntimeException(
                    "Phone number already registered"
            );
        }

        Customer customer = new Customer();

        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());

        customer.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        customer.setActive(true);

        return customerRepository.save(customer);
    }

    /*
     * CUSTOMER PROFILE UPDATE
     *
     * Only fields explicitly allowed by
     * CustomerUpdateRequest can be changed.
     */
    public Customer updateCustomer(
            Long id,
            CustomerUpdateRequest request) {

        Customer existingCustomer =
                customerRepository.findById(id)
                        .orElse(null);

        if (existingCustomer == null) {
            return null;
        }

        /*
         * Check whether the new email is already
         * being used by another customer.
         */
        customerRepository
                .findByEmail(request.getEmail())
                .ifPresent(customer -> {

                    if (!customer.getId().equals(id)) {
                        throw new RuntimeException(
                                "Email already registered"
                        );
                    }
                });

        /*
         * Check whether the new phone number is
         * already being used by another customer.
         */
        customerRepository
                .findByPhone(request.getPhone())
                .ifPresent(customer -> {

                    if (!customer.getId().equals(id)) {
                        throw new RuntimeException(
                                "Phone number already registered"
                        );
                    }
                });

        /*
         * Detect contact information changes.
         */
        boolean emailChanged =
                !Objects.equals(
                        existingCustomer.getEmail(),
                        request.getEmail()
                );

        boolean phoneChanged =
                !Objects.equals(
                        existingCustomer.getPhone(),
                        request.getPhone()
                );

        existingCustomer.setName(
                request.getName()
        );

        existingCustomer.setEmail(
                request.getEmail()
        );

        existingCustomer.setPhone(
                request.getPhone()
        );

        /*
         * A changed email must be verified again.
         */
        if (emailChanged) {
            existingCustomer.setEmailVerified(false);
        }

        /*
         * A changed phone number must be verified again.
         */
        if (phoneChanged) {
            existingCustomer.setPhoneVerified(false);
        }

        /*
         * IMPORTANT:
         *
         * We intentionally do NOT modify:
         *
         * - password
         * - role
         * - active
         * - googleId
         *
         * Those require separate controlled flows.
         */

        return customerRepository.save(existingCustomer);
    }
}