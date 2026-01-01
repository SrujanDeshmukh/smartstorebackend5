package com.raghunath.smartstore.security;

import com.raghunath.smartstore.entity.User;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.repository.UserRepository;
import com.raghunath.smartstore.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SmartStoreUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Try to load as User
        User user = userRepository.findByEmail(email.toLowerCase().trim()).orElse(null);
        if (user != null) {
            return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                    .password(user.getPassword())
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                    .accountLocked(user.isAccountLocked())
                    .disabled(!user.isActive())
                    .build();
        }

        // Try to load as Vendor
        Vendor vendor = vendorRepository.findByEmail(email.toLowerCase().trim()).orElse(null);
        if (vendor != null) {
            return org.springframework.security.core.userdetails.User.withUsername(vendor.getEmail())
                    .password(vendor.getPassword())
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_VENDOR")))
                    .accountLocked(vendor.isAccountLocked())
                    .disabled(!vendor.isActive())
                    .build();
        }

        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}
