package com.mittiandmore.service;

import com.mittiandmore.entity.Address;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.repository.AddressRepository;
import com.mittiandmore.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;

    public AddressService(
            AddressRepository addressRepository,
            CustomerRepository customerRepository) {

        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
    }

    public List<Address> getCustomerAddresses(Long customerId) {
        return addressRepository.findByCustomerId(customerId);
    }

    public Address getAddressById(Long id) {
        return addressRepository.findById(id)
                .orElse(null);
    }

    @Transactional
    public Address createAddress(
            Long customerId,
            Address address) {

        Customer customer = customerRepository.findById(customerId)
                .orElse(null);

        if (customer == null) {
            return null;
        }

        address.setCustomer(customer);

        if (Boolean.TRUE.equals(address.getDefaultAddress())) {
            clearExistingDefaultAddress(customerId);
        }

        return addressRepository.save(address);
    }

    @Transactional
    public Address updateAddress(
            Long id,
            Address updatedAddress) {

        Address existingAddress = addressRepository.findById(id)
                .orElse(null);

        if (existingAddress == null) {
            return null;
        }

        existingAddress.setName(updatedAddress.getName());
        existingAddress.setPhone(updatedAddress.getPhone());
        existingAddress.setHouse(updatedAddress.getHouse());
        existingAddress.setStreet(updatedAddress.getStreet());
        existingAddress.setCity(updatedAddress.getCity());
        existingAddress.setState(updatedAddress.getState());
        existingAddress.setPincode(updatedAddress.getPincode());
        existingAddress.setAddressType(updatedAddress.getAddressType());

        if (Boolean.TRUE.equals(updatedAddress.getDefaultAddress())) {

            Long customerId = existingAddress.getCustomer().getId();

            clearExistingDefaultAddress(
                    customerId,
                    existingAddress.getId()
            );

            existingAddress.setDefaultAddress(true);

        } else {
            existingAddress.setDefaultAddress(false);
        }

        return addressRepository.save(existingAddress);
    }

    @Transactional
    public void deleteAddress(Long id) {
        Address existingAddress = addressRepository.findById(id)
                .orElse(null);

        if (existingAddress == null) {
            return;
        }

        addressRepository.delete(existingAddress);
    }

    private void clearExistingDefaultAddress(Long customerId) {
        clearExistingDefaultAddress(customerId, null);
    }

    private void clearExistingDefaultAddress(
            Long customerId,
            Long addressBeingUpdated) {

        List<Address> addresses =
                addressRepository.findByCustomerId(customerId);

        for (Address address : addresses) {

            if (addressBeingUpdated != null
                    && address.getId().equals(addressBeingUpdated)) {
                continue;
            }

            if (Boolean.TRUE.equals(address.getDefaultAddress())) {
                address.setDefaultAddress(false);
            }
        }

        addressRepository.saveAll(addresses);
    }
}