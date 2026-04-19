package com.carbooking.service;

import com.carbooking.entity.Car;
import com.carbooking.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarService {

    private final CarRepository carRepo;

    // Get all available cars
    public List<Car> getAvailableCars() {
        return carRepo
                .findByIsBlockedFalseAndIsAvailableTrue();
    }

    // Get all cars
    public List<Car> getAllCars() {
        return carRepo.findAll();
    }

    // Get car by id
    public Car getCarById(Long id) {
        return carRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Car not found with id: " + id));
    }

    // Add new car
    public Car addCar(Car car) {
        validateCarDocuments(car);
        car.setIsAvailable(true);
        car.setIsBlocked(false);
        log.info("Car added: {}", car.getName());
        return carRepo.save(car);
    }

    // Update car
    public Car updateCar(Long id, Car updated) {
        Car existing = carRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Car not found"));
        updated.setId(id);
        updated.setMorningDriver(
                existing.getMorningDriver());
        updated.setEveningDriver(
                existing.getEveningDriver());
        updated.setNightDriver(
                existing.getNightDriver());
        log.info("Car updated: {}",
                updated.getName());
        return carRepo.save(updated);
    }

    // Delete car
    public void deleteCar(Long id) {
        carRepo.deleteById(id);
        log.info("Car deleted: {}", id);
    }

    // Block car
    public Car blockCar(Long id,
                        String reason) {
        Car car = getCarById(id);
        car.setIsBlocked(true);
        car.setIsAvailable(false);
        log.warn("Car blocked: {} - Reason: {}",
                car.getName(), reason);
        return carRepo.save(car);
    }

    // Unblock car
    public Car unblockCar(Long id) {
        Car car = getCarById(id);
        // Check if documents are valid
        LocalDate today = LocalDate.now();
        if (car.getInsuranceExpiry() != null &&
                car.getInsuranceExpiry()
                        .isBefore(today)) {
            throw new RuntimeException(
                    "Cannot unblock: Insurance expired");
        }
        if (car.getRcExpiry() != null &&
                car.getRcExpiry().isBefore(today)) {
            throw new RuntimeException(
                    "Cannot unblock: RC expired");
        }
        if (car.getFcExpiry() != null &&
                car.getFcExpiry().isBefore(today)) {
            throw new RuntimeException(
                    "Cannot unblock: FC expired");
        }
        car.setIsBlocked(false);
        car.setIsAvailable(true);
        log.info("Car unblocked: {}",
                car.getName());
        return carRepo.save(car);
    }

    // Filter cars
    public List<Car> filterCars(
            Car.CarType type, Integer seats) {
        if (type != null && seats != null) {
            return carRepo
                    .findByTypeAndSeatsGreaterThanEqual(
                            type, seats);
        }
        return getAvailableCars();
    }

    // Validate car documents
    private void validateCarDocuments(Car car) {
        LocalDate today = LocalDate.now();
        if (car.getInsuranceExpiry() != null &&
                car.getInsuranceExpiry()
                        .isBefore(today)) {
            car.setIsBlocked(true);
            log.warn(
                    "Car blocked on add - " +
                            "Insurance expired: {}",
                    car.getName());
        }
        if (car.getRcExpiry() != null &&
                car.getRcExpiry().isBefore(today)) {
            car.setIsBlocked(true);
            log.warn(
                    "Car blocked on add - " +
                            "RC expired: {}",
                    car.getName());
        }
        if (car.getFcExpiry() != null &&
                car.getFcExpiry().isBefore(today)) {
            car.setIsBlocked(true);
            log.warn(
                    "Car blocked on add - " +
                            "FC expired: {}",
                    car.getName());
        }
    }
}