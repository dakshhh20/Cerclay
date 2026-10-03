package com.mittiandmore.controller;

import com.mittiandmore.dto.AddressRequest;
import com.mittiandmore.entity.Address;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;
    private final CustomerRepository customerRepository;

    public AddressController(
            AddressService addressService,
            CustomerRepository customerRepository) {

        this.addressService = addressService;
        this.customerRepository = customerRepository;
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<?> getCustomerAddresses(
            @PathVariable Long customerId,
            Authentication authentication) {

        Customer customer = customerRepository.findById(customerId)
                .orElse(null);

        if (customer == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Customer not found");
        }

        if (!customer.getEmail().equals(authentication.getName())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You are not allowed to access these addresses");
        }

        List<Address> addresses =
                addressService.getCustomerAddresses(customerId);

        return ResponseEntity.ok(addresses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAddressById(
            @PathVariable Long id,
            Authentication authentication) {

        Address address = addressService.getAddressById(id);

        if (address == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Address not found");
        }

        if (!address.getCustomer()
                .getEmail()
                .equals(authentication.getName())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You are not allowed to access this address");
        }

        return ResponseEntity.ok(address);
    }

    @PostMapping("/customer/{customerId}")
    public ResponseEntity<?> createAddress(
            @PathVariable Long customerId,
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {

        Customer customer = customerRepository.findById(customerId)
                .orElse(null);

        if (customer == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Customer not found");
        }

        if (!customer.getEmail().equals(authentication.getName())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You are not allowed to add an address for this customer");
        }

        Address address = new Address();

        address.setName(request.getName());
        address.setPhone(request.getPhone());
        address.setHouse(request.getHouse());
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setAddressType(request.getAddressType());
        address.setDefaultAddress(request.getDefaultAddress());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(addressService.createAddress(customerId, address));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {

        Address existingAddress =
                addressService.getAddressById(id);

        if (existingAddress == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Address not found");
        }

        if (!existingAddress.getCustomer()
                .getEmail()
                .equals(authentication.getName())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You are not allowed to modify this address");
        }

        Address updatedAddress = new Address();

        updatedAddress.setName(request.getName());
        updatedAddress.setPhone(request.getPhone());
        updatedAddress.setHouse(request.getHouse());
        updatedAddress.setStreet(request.getStreet());
        updatedAddress.setCity(request.getCity());
        updatedAddress.setState(request.getState());
        updatedAddress.setPincode(request.getPincode());
        updatedAddress.setAddressType(request.getAddressType());
        updatedAddress.setDefaultAddress(request.getDefaultAddress());

        return ResponseEntity.ok(
                addressService.updateAddress(id, updatedAddress)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAddress(
            @PathVariable Long id,
            Authentication authentication) {

        Address existingAddress =
                addressService.getAddressById(id);

        if (existingAddress == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Address not found");
        }

        if (!existingAddress.getCustomer()
                .getEmail()
                .equals(authentication.getName())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You are not allowed to delete this address");
        }

        addressService.deleteAddress(id);

        return ResponseEntity.ok("Address deleted successfully");
    }
}