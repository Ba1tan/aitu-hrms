package kz.aitu.hrms.user.security;

import kz.aitu.hrms.user.entity.User;
import kz.aitu.hrms.user.repository.RolePermissionRepository;
import kz.aitu.hrms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        user.setPermissionCodes(rolePermissionRepository.findPermissionCodesByRole(user.getRole()));
        return user;
    }
}