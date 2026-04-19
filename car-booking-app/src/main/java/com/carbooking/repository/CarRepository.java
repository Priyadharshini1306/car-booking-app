package com.carbooking.repository;
import com.carbooking.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findByTypeAndSeatsGreaterThanEqual(
            Car.CarType type, Integer seats);

    List<Car> findByIsBlockedFalseAndIsAvailableTrue();

    List<Car> findByInsuranceExpiryBefore(LocalDate date);
    List<Car> findByRcExpiryBefore(LocalDate date);
    List<Car> findByFcExpiryBefore(LocalDate date);
}

