package com.carbooking.controller;

import com.carbooking.entity.*;
import com.carbooking.repository.*;
import com.carbooking.service.SchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CarRepository      carRepo;
    private final DriverRepository   driverRepo;
    private final BookingRepository  bookingRepo;
    private final UserRepository     userRepo;
    private final SchedulerService   schedulerService;

    // ===== CAR MANAGEMENT =====

    @GetMapping("/cars")
    public List<Car> getAllCars() {
        return carRepo.findAll();
    }

    @PostMapping("/cars")
    public Car addCar(@RequestBody Car car) {
        return carRepo.save(car);
    }

    @PutMapping("/cars/{id}")
    public ResponseEntity<Car> updateCar(
            @PathVariable Long id,
            @RequestBody Car updated) {
        return carRepo.findById(id)
                .map(car -> {
                    updated.setId(id);
                    return ResponseEntity.ok(carRepo.save(updated));
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/cars/{id}")
    public ResponseEntity<Void> deleteCar(@PathVariable Long id) {
        carRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/cars/{carId}/assign-driver")
    public ResponseEntity<?> assignDriver(
            @PathVariable Long carId,
            @RequestBody Map<String, Object> body) {

        Car    car      = carRepo.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found"));
        String shift    = (String) body.get("shift");
        Long   driverId = null;

        if (body.get("driverId") != null
                && !body.get("driverId").toString().isEmpty()
                && !body.get("driverId").toString().equals("null")) {
            driverId = Long.valueOf(body.get("driverId").toString());
        }

        Driver driver = null;
        if (driverId != null) {
            driver = driverRepo.findById(driverId)
                    .orElseThrow(() -> new RuntimeException("Driver not found"));
            if (driver.getAge() >= 60)
                throw new RuntimeException("Driver must be under 60");
        }

        if      ("MORNING".equals(shift)) car.setMorningDriver(driver);
        else if ("EVENING".equals(shift)) car.setEveningDriver(driver);
        else if ("NIGHT".equals(shift))   car.setNightDriver(driver);
        else throw new RuntimeException("Invalid shift: " + shift);

        return ResponseEntity.ok(carRepo.save(car));
    }

    @GetMapping("/cars/{id}/drivers")
    public ResponseEntity<Map<String, Object>> getCarDrivers(@PathVariable Long id) {
        Car car = carRepo.findById(id).orElseThrow();
        Map<String, Object> result = new HashMap<>();
        result.put("car",           car);
        result.put("morningDriver", car.getMorningDriver());
        result.put("eveningDriver", car.getEveningDriver());
        result.put("nightDriver",   car.getNightDriver());
        return ResponseEntity.ok(result);
    }

    // ===== DRIVER MANAGEMENT =====

    @GetMapping("/drivers")
    public List<Driver> getAllDrivers() {
        return driverRepo.findAll();
    }

    @PostMapping("/drivers")
    public Driver addDriver(@RequestBody Driver driver) {
        if (driver.getAge() >= 60)
            throw new RuntimeException("Driver must be under 60");
        return driverRepo.save(driver);
    }

    @PutMapping("/drivers/{id}")
    public ResponseEntity<Driver> updateDriver(
            @PathVariable Long id,
            @RequestBody Driver updated) {
        return driverRepo.findById(id)
                .map(d -> {
                    if (updated.getAge() >= 60)
                        throw new RuntimeException("Driver must be under 60");
                    updated.setId(id);
                    return ResponseEntity.ok(driverRepo.save(updated));
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/drivers/{id}")
    public ResponseEntity<Void> deleteDriver(@PathVariable Long id) {
        driverRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // =============================================
    // Update Driver Shift
    // PUT /api/admin/drivers/{id}/shift
    // Body: { "shift": "MORNING" | "EVENING" | "NIGHT" }
    // =============================================
    @PutMapping("/drivers/{id}/shift")
    public ResponseEntity<?> updateDriverShift(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        Driver driver = driverRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        String shiftStr = body.get("shift");
        if (shiftStr == null || shiftStr.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Shift is required"));
        }
        try {
            Driver.Shift newShift = Driver.Shift.valueOf(shiftStr.toUpperCase().trim());
            driver.setShift(newShift);
            Driver saved = driverRepo.save(driver);
            return ResponseEntity.ok(Map.of(
                    "message", "Shift updated to " + newShift + " for " + saved.getName(),
                    "driver",  saved
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid shift. Use MORNING, EVENING or NIGHT"));
        }
    }

    // Approve or Reject Leave
    @PutMapping("/drivers/{id}/leave")
    public ResponseEntity<?> handleLeave(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        Driver driver = driverRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        String action = body.get("action");

        if ("APPROVE".equals(action)) {
            driver.setLeaveStatus(Driver.LeaveStatus.APPROVED);
            driver.setOnLeave(true);
            driver.setLeaveStart(driver.getRequestedLeaveStart());
            driver.setLeaveEnd(driver.getRequestedLeaveEnd());
            driver.setIsAvailable(false);
            driverRepo.save(driver);
            return ResponseEntity.ok(Map.of("message", "Leave approved!"));

        } else if ("REJECT".equals(action)) {
            driver.setLeaveStatus(Driver.LeaveStatus.REJECTED);
            driver.setRequestedLeaveStart(null);
            driver.setRequestedLeaveEnd(null);
            driver.setLeaveReason(null);
            driverRepo.save(driver);
            return ResponseEntity.ok(Map.of("message", "Leave rejected!"));
        }

        return ResponseEntity.badRequest().body(Map.of("message", "Invalid action"));
    }

    // =============================================
    // FIX: Instantly restore all drivers whose
    // leave end date has already passed.
    //
    // Use when: scheduler hasn't fired yet, or you
    // need to force-restore a driver immediately.
    //
    // GET /api/admin/fix-driver-availability
    // Also accessible from: http://localhost:8080/api/admin/fix-driver-availability
    // =============================================
    @GetMapping("/fix-driver-availability")
    public ResponseEntity<Map<String, Object>> fixDriverAvailability() {
        LocalDate    today      = LocalDate.now();
        List<Driver> allDrivers = driverRepo.findAll();

        List<String> restored = new ArrayList<>();
        List<String> stillOnLeave = new ArrayList<>();

        for (Driver driver : allDrivers) {
            boolean leaveExpired = driver.getLeaveEnd() != null
                    && driver.getLeaveEnd().isBefore(today);

            if (leaveExpired) {
                driver.setOnLeave(false);
                driver.setIsAvailable(true);
                driver.setLeaveStatus(Driver.LeaveStatus.NONE);
                driver.setLeaveStart(null);
                driver.setLeaveEnd(null);
                driver.setRequestedLeaveStart(null);
                driver.setRequestedLeaveEnd(null);
                driver.setLeaveReason(null);
                driverRepo.save(driver);
                restored.add(driver.getName() + " (ID:" + driver.getId() + ")");
            } else if (driver.getLeaveEnd() != null && !driver.getLeaveEnd().isBefore(today)) {
                stillOnLeave.add(driver.getName() + " — leave ends: " + driver.getLeaveEnd());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("today",           today.toString());
        result.put("message",         restored.isEmpty()
                ? "No drivers needed restoration."
                : restored.size() + " driver(s) restored to AVAILABLE.");
        result.put("restoredCount",   restored.size());
        result.put("restoredDrivers", restored);
        result.put("stillOnLeave",    stillOnLeave);
        return ResponseEntity.ok(result);
    }

    // ===== BOOKING MANAGEMENT =====

    @GetMapping("/bookings")
    public List<Booking> allBookings() {
        return bookingRepo.findAll();
    }

    @PutMapping("/bookings/{id}/status")
    public Booking updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Booking b = bookingRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if (body.containsKey("status")) {
            b.setStatus(Booking.BookingStatus.valueOf(body.get("status")));
        }
        if (body.containsKey("paymentStatus")) {
            b.setPaymentStatus(Booking.PaymentStatus.valueOf(body.get("paymentStatus")));
        }
        return bookingRepo.save(b);
    }

    // ===== USER MANAGEMENT =====

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    // ===== DASHBOARD STATS =====

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBookings",  bookingRepo.count());
        stats.put("totalCars",      carRepo.count());
        stats.put("totalDrivers",   driverRepo.count());
        stats.put("totalUsers",     userRepo.count());
        stats.put("activeBookings", bookingRepo.countActiveBookings());
        return stats;
    }

    @GetMapping("/test-expiry-check")
    public ResponseEntity<String> testExpiryCheck() {
        schedulerService.triggerManualCheck();
        return ResponseEntity.ok("Daily check triggered! Check your admin email.");
    }
}