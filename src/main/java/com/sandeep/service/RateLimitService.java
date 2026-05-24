package com.sandeep.service;

import com.sandeep.config.AppProperties;
import com.sandeep.exception.RateLimitExceededException;
import com.sandeep.exception.ResourceNotFoundException;
import com.sandeep.model.Role;
import com.sandeep.model.User;
import com.sandeep.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final UserRepository userRepository;
    private final AppProperties appProperties;

    @Transactional
    public void checkAndIncrement(Long userId, Set<Role> roles) {
        if (roles.contains(Role.ROLE_ADMIN)) return;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LocalDate today = LocalDate.now();
        if (user.getLastRequestDate() == null || !user.getLastRequestDate().equals(today)) {
            user.setDailyRequestCount(0);
            user.setLastRequestDate(today);
        }

        int limit = roles.contains(Role.ROLE_PREMIUM)
                ? appProperties.getRateLimit().getPremiumRequestsPerDay()
                : appProperties.getRateLimit().getUserRequestsPerDay();

        if (user.getDailyRequestCount() >= limit) {
            throw new RateLimitExceededException(
                    "Daily limit of " + limit + " requests reached. " +
                    (roles.contains(Role.ROLE_PREMIUM) ? "Contact support." : "Upgrade to PREMIUM for more requests.")
            );
        }

        user.setDailyRequestCount(user.getDailyRequestCount() + 1);
        userRepository.save(user);
    }
}
