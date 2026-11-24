
package com.webapp.bankingportal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import com.webapp.bankingportal.entity.OtpInfo;

@Repository
public interface OtpInfoRepository extends JpaRepository<OtpInfo, Long> {

    Optional<OtpInfo> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OtpInfo o WHERE o.accountNumber = :accountNumber")
    Optional<OtpInfo> findByAccountNumberForUpdate(
            @Param("accountNumber") String accountNumber
    );
}
