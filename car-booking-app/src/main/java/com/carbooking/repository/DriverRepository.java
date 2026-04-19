package com.carbooking.repository;
import com.carbooking.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    List<Driver> findByIsAvailableTrueAndIsBlockedFalseAndOnLeaveFalse();
    List<Driver> findByLicenseExpiryBefore(LocalDate date);
}

