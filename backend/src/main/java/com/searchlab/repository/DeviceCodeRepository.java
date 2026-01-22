package com.searchlab.repository;

import com.searchlab.model.entity.DeviceCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DeviceCodeRepository extends JpaRepository<DeviceCode, Long> {
    Optional<DeviceCode> findByDeviceCode(String deviceCode);
    Optional<DeviceCode> findByUserCode(String userCode);

    /**
     * Clean up expired device codes (older than specified time)
     */
    @Modifying
    @Query("DELETE FROM DeviceCode d WHERE d.expiresAt < :cutoffTime")
    void deleteExpiredCodes(@Param("cutoffTime") LocalDateTime cutoffTime);
}
