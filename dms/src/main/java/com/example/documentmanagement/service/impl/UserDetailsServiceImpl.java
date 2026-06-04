package com.example.documentmanagement.service.impl;

import com.example.documentmanagement.entity.User;
import com.example.documentmanagement.repository.UserRepository;
import com.example.documentmanagement.service.CustomUserDetailsService;
import com.example.documentmanagement.util.MessageConstants;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements CustomUserDetailsService {

    private final UserRepository userRepository;
    private final MessageSource messageSource;

    public UserDetailsServiceImpl(UserRepository userRepository, MessageSource messageSource) {
        this.userRepository = userRepository;
        this.messageSource = messageSource;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    String msg = messageSource.getMessage(MessageConstants.USER_NOT_FOUND, null, "User not found",
                            LocaleContextHolder.getLocale());
                    return new UsernameNotFoundException(msg + ": " + username);
                });

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isActive(),
                true,
                true,
                true,
                user.getRoles().stream()
                        .map(role -> {
                            String name = role.getName();
                            if (!name.startsWith("ROLE_")) {
                                name = "ROLE_" + name;
                            }
                            return new SimpleGrantedAuthority(name);
                        })
                        .toList());
    }
}
