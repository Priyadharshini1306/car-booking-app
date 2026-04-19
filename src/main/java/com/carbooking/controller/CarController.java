package com.carbooking.controller;
import com.carbooking.entity.Car;
import com.carbooking.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarRepository carRepo;

    // Public: list all available cars
    @GetMapping("/public/available")
    public List<Car> getAvailableCars() {
        return carRepo.findByIsBlockedFalseAndIsAvailableTrue();
    }

    // Public: filter by type and seats
    @GetMapping("/public/filter")
    public List<Car> filterCars(
            @RequestParam(required=false) Car.CarType type,
            @RequestParam(required=false) Integer seats) {
        if (type != null && seats != null)
            return carRepo.findByTypeAndSeatsGreaterThanEqual(type, seats);
        return carRepo.findByIsBlockedFalseAndIsAvailableTrue();
    }

    // Get single car
    @GetMapping("/public/{id}")
    public ResponseEntity<Car> getCar(@PathVariable Long id) {
        return carRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
