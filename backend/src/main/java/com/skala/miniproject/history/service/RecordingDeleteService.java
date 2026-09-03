package com.skala.miniproject.history.service;

import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.domain.recording.Recording;
import com.skala.miniproject.history.repository.RecordingDeleteRepository;
import com.skala.miniproject.user.repository.WithdrawalUserLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordingDeleteService {

    private final WithdrawalUserLockRepository userLockRepository;
    private final RecordingDeleteRepository recordingDeleteRepository;

    @Transactional
    public void deleteRecording(Long userId, Long recordingId) {
        userLockRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        Recording recording = recordingDeleteRepository.findByIdAndUserId(recordingId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (recordingDeleteRepository.countActiveByRecordingId(recordingId) > 0) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_WHILE_PROCESSING);
        }

        recordingDeleteRepository.delete(recording);
    }
}
