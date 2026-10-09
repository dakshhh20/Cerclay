package com.mittiandmore.dto;

import com.mittiandmore.entity.Customer;

public class CustomerResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private Boolean active;
    private Boolean emailVerified;
    private Boolean phoneVerified;

    public CustomerResponse() {}

    public CustomerResponse(
        Long id,
        String name,
        String email,
        String phone,
        Boolean active,
        Boolean emailVerified,
        Boolean phoneVerified
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.active = active;
        this.emailVerified = emailVerified;
        this.phoneVerified = phoneVerified;
    }

    public static CustomerResponse fromEntity(Customer customer) {
        return new CustomerResponse(
            customer.getId(),
            customer.getName(),
            customer.getEmail(),
            customer.getPhone(),
            customer.getActive(),
            customer.getEmailVerified(),
            customer.getPhoneVerified()
        );
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public Boolean getActive() {
        return active;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public Boolean getPhoneVerified() {
        return phoneVerified;
    }
}
