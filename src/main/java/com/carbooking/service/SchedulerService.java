package com.carbooking.service;

import com.carbooking.entity.*;
import com.carbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final CarRepository    carRepo;
    private final DriverRepository driverRepo;
    private final EmailService     emailService;

    @Value("${admin.email}")
    private String adminEmail;

    /**
     * Runs every day at 8 AM.
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void runDailyChecks() {
        log.info("Running daily checks...");
        checkCarExpiryDates();
        checkDriverLicenseExpiry();
        autoReturnDriversFromLeave();   // FIX: run this before availability check
        checkCarDriverAvailability();
        log.info("Daily checks done.");
    }

    /**
     * FIX — Auto return drivers from leave.
     *
     * BUG that was here before:
     *   Condition was:  !driver.getLeaveEnd().isAfter(today)
     *   This means:     leaveEnd <= today  (i.e. leaveEnd is today OR already past)
     *   Problem:        When leaveEnd == today the driver is STILL on leave for
     *                   the rest of today, so we should NOT return them until
     *                   leaveEnd is STRICTLY BEFORE today (i.e. yesterday or earlier).
     *
     * FIXED condition:  leaveEnd.isBefore(today)
     *   This means:     leaveEnd < today  (yesterday or earlier)
     *   So the driver is only returned the MORNING AFTER their last leave day,
     *   which is exactly when the 8AM scheduler fires.
     *
     * Also added: reset isAvailable = true and all leave fields so the DB
     * flag is always consistent after the scheduler runs.
     */
    private void autoReturnDriversFromLeave() {
        LocalDate today      = LocalDate.now();
        List<Driver> allDrivers = driverRepo.findAll();

        for (Driver driver : allDrivers) {

            // Only process drivers who have a leave end date set
            if (driver.getLeaveEnd() == null) continue;

            // FIX: use strict isBefore — leave ended yesterday or earlier
            boolean leaveHasEnded = driver.getLeaveEnd().isBefore(today);

            if (!leaveHasEnded) continue;

            // Check if the driver was actually on leave (any state)
            boolean wasOnLeave =
                    Boolean.TRUE.equals(driver.getOnLeave())
                            || driver.getLeaveStatus() == Driver.LeaveStatus.APPROVED
                            || driver.getLeaveStatus() == Driver.LeaveStatus.PENDING;

            if (!wasOnLeave) {
                // Leave dates present but driver not flagged as on-leave —
                // still clean up stale date fields
                driver.setLeaveStart(null);
                driver.setLeaveEnd(null);
                driver.setRequestedLeaveStart(null);
                driver.setRequestedLeaveEnd(null);
                driver.setLeaveReason(null);
                driverRepo.save(driver);
                continue;
            }

            // ── Fully restore driver ──
            driver.setOnLeave(false);
            driver.setIsAvailable(true);          // FIX: explicitly reset to true
            driver.setLeaveStatus(Driver.LeaveStatus.NONE);
            driver.setLeaveStart(null);
            driver.setLeaveEnd(null);
            driver.setRequestedLeaveStart(null);
            driver.setRequestedLeaveEnd(null);
            driver.setLeaveReason(null);

            Driver saved = driverRepo.save(driver);

            log.info(
                    "Driver {} (ID:{}) auto-returned from leave. "
                            + "isAvailable={}, leaveStatus={} saved to DB.",
                    saved.getName(), saved.getId(),
                    saved.getIsAvailable(), saved.getLeaveStatus());

            // Notify admin
            try {
                emailService.sendEmail(
                        adminEmail,
                        "Driver Returned from Leave — " + driver.getName(),
                        "Dear Admin,\n\n"
                                + "Driver: "  + driver.getName()  + "\n"
                                + "ID:     "  + driver.getId()     + "\n"
                                + "Phone:  "  + driver.getPhone()  + "\n"
                                + "Shift:  "  + driver.getShift()  + "\n\n"
                                + "Leave has ended. Driver is now AVAILABLE.\n"
                                + "DB isAvailable flag has been reset to TRUE.\n\n"
                                + "DriveEasy System");
            } catch (Exception e) {
                log.error("Failed to send leave-return email for driver {}: {}",
                        driver.getName(), e.getMessage());
            }
        }
    }

    /**
     * Check if any car's permanent driver is on leave today.
     * Notify admin to assign a temporary driver if needed.
     */
    private void checkCarDriverAvailability() {
        List<Car> allCars = carRepo.findAll();
        for (Car car : allCars) {
            if (Boolean.TRUE.equals(car.getIsBlocked())) continue;
            checkShiftDriver(car, car.getMorningDriver(), "MORNING");
            checkShiftDriver(car, car.getEveningDriver(), "EVENING");
            checkShiftDriver(car, car.getNightDriver(),   "NIGHT");
        }
    }

    private void checkShiftDriver(Car car, Driver driver, String shift) {
        if (driver == null) return;
        if (driver.isOnLeaveToday() || Boolean.TRUE.equals(driver.getIsBlocked())) {
            log.warn("Car {} has no {} driver today (driver: {})",
                    car.getName(), shift, driver.getName());
            try {
                emailService.sendEmail(
                        adminEmail,
                        "No " + shift + " Driver for: " + car.getName(),
                        "Dear Admin,\n\n"
                                + "Car: "   + car.getName() + " (ID: " + car.getId() + ")\n"
                                + "Shift: " + shift         + "\n\n"
                                + "The permanent driver " + driver.getName()
                                + " is on LEAVE or BLOCKED today.\n\n"
                                + "Please assign a temporary driver:\n"
                                + "http://localhost:8080/admin.html\n\n"
                                + "DriveEasy System");
            } catch (Exception e) {
                log.error("Failed to send driver-unavailable email: {}", e.getMessage());
            }
        }
    }

    private void checkCarExpiryDates() {
        LocalDate today  = LocalDate.now();
        LocalDate warn30 = today.plusDays(30);
        LocalDate warn60 = today.plusDays(60);
        List<Car> allCars = carRepo.findAll();

        for (Car car : allCars) {
            // Insurance
            if (car.getInsuranceExpiry() != null) {
                if (car.getInsuranceExpiry().isBefore(today)) {
                    if (!Boolean.TRUE.equals(car.getIsBlocked())) {
                        car.setIsBlocked(true);
                        carRepo.save(car);
                        sendEmail("CAR BLOCKED - Insurance Expired",
                                "Car: " + car.getName()
                                        + "\nInsurance expired: " + car.getInsuranceExpiry()
                                        + "\nCar has been BLOCKED. Please renew insurance.\n\nDriveEasy System");
                    }
                } else if (car.getInsuranceExpiry().isBefore(warn30)) {
                    sendEmail("Insurance Expiring Soon",
                            "Car: " + car.getName()
                                    + "\nInsurance expires: " + car.getInsuranceExpiry()
                                    + "\nPlease renew soon.\n\nDriveEasy System");
                }
            }
            // RC
            if (car.getRcExpiry() != null) {
                if (car.getRcExpiry().isBefore(today)) {
                    if (!Boolean.TRUE.equals(car.getIsBlocked())) {
                        car.setIsBlocked(true);
                        carRepo.save(car);
                        sendEmail("CAR BLOCKED - RC Expired",
                                "Car: " + car.getName()
                                        + "\nRC expired: " + car.getRcExpiry()
                                        + "\nCar has been BLOCKED.\n\nDriveEasy System");
                    }
                } else if (car.getRcExpiry().isBefore(warn60)) {
                    sendEmail("RC Expiring Soon",
                            "Car: " + car.getName()
                                    + "\nRC expires: " + car.getRcExpiry()
                                    + "\nPlease renew soon.\n\nDriveEasy System");
                }
            }
            // FC
            if (car.getFcExpiry() != null) {
                if (car.getFcExpiry().isBefore(today)) {
                    if (!Boolean.TRUE.equals(car.getIsBlocked())) {
                        car.setIsBlocked(true);
                        carRepo.save(car);
                        sendEmail("CAR BLOCKED - FC Expired",
                                "Car: " + car.getName()
                                        + "\nFC expired: " + car.getFcExpiry()
                                        + "\nCar has been BLOCKED.\n\nDriveEasy System");
                    }
                } else if (car.getFcExpiry().isBefore(warn60)) {
                    sendEmail("FC Expiring Soon",
                            "Car: " + car.getName()
                                    + "\nFC expires: " + car.getFcExpiry()
                                    + "\nPlease renew soon.\n\nDriveEasy System");
                }
            }
        }
    }

    private void checkDriverLicenseExpiry() {
        LocalDate today  = LocalDate.now();
        LocalDate warn30 = today.plusDays(30);
        List<Driver> allDrivers = driverRepo.findAll();

        for (Driver driver : allDrivers) {
            if (driver.getLicenseExpiry() == null) continue;
            if (driver.getLicenseExpiry().isBefore(today)) {
                if (!Boolean.TRUE.equals(driver.getIsBlocked())) {
                    driver.setIsBlocked(true);
                    driver.setIsAvailable(false);
                    driverRepo.save(driver);
                    sendEmail("DRIVER BLOCKED - License Expired",
                            "Driver: " + driver.getName()
                                    + "\nLicense expired: " + driver.getLicenseExpiry()
                                    + "\nDriver has been BLOCKED. Please renew license.\n\nDriveEasy System");
                }
            } else if (driver.getLicenseExpiry().isBefore(warn30)) {
                sendEmail("Driver License Expiring Soon",
                        "Driver: " + driver.getName()
                                + "\nLicense expires: " + driver.getLicenseExpiry()
                                + "\nPlease renew soon.\n\nDriveEasy System");
            }
        }
    }

    @Scheduled(cron = "0 0 0 1 1 ?")
    public void annualDriverIncrement() {
        driverRepo.findAll().forEach(d -> {
            d.setYearsOfService(d.getYearsOfService() + 1);
            if (d.getSalary() != null) {
                d.setSalary(d.getSalary().multiply(
                        java.math.BigDecimal.valueOf(1.05)));
            }
            driverRepo.save(d);
        });
        log.info("Annual increment done");
    }

    public void triggerManualCheck() {
        log.info("Manual expiry check triggered by admin");
        runDailyChecks();
    }

    private void sendEmail(String subject, String body) {
        try {
            emailService.sendEmail(adminEmail, subject, body);
        } catch (Exception e) {
            log.error("Failed to send email '{}': {}", subject, e.getMessage());
        }
    }
}