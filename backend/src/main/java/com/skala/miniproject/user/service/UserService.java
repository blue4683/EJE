package com.skala.miniproject.user.service;

import com.skala.miniproject.auth.dto.UserDto;
import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.domain.user.User;
import com.skala.miniproject.domain.user.UserRepository;
import com.skala.miniproject.user.dto.WithdrawRequest;
import com.skala.miniproject.user.repository.ActiveAnalysisCountRepository;
import com.skala.miniproject.user.repository.UserLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserLockRepository userLockRepository;
    private final ActiveAnalysisCountRepository activeAnalysisCountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserDto getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return UserDto.from(user);
    }

    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        User user = userLockRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        validatePassword(request.password());
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        if (activeAnalysisCountRepository.countActiveByUserId(userId) > 0) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_WHILE_PROCESSING);
        }
        userLockRepository.delete(user);
    }

    private void validatePassword(String password) {
        if (password == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        int codePointCount = password.codePointCount(0, password.length());
        if (codePointCount < 8
                || codePointCount > 64
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
