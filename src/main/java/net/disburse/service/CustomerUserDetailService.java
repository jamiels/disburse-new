package net.disburse.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomerUserDetailService implements UserDetailsService {
    private String username;
    private String password;

    @Value("${user.username}")
    public void setUsername(String username) {
        this.username = username;
    }

    @Value("${user.password}")
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (this.username.equals(username)) {
            return new User(this.username, new BCryptPasswordEncoder().encode(this.password), Collections.emptyList());
        } else {
            throw new UsernameNotFoundException("User not found");
        }
    }
}
