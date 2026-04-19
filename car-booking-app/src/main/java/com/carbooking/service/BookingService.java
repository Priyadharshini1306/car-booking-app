package com.carbooking.service;

import com.carbooking.dto.BookingRequest;
import com.carbooking.entity.*;
import com.carbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepo;
    private final CarRepository carRepo;
    private final DriverRepository driverRepo;
    private final UserRepository userRepo;
    private final EmailService emailService;

    @Value("${admin.email}")
    private String adminEmail;

    // =============================================
    // CREATE BOOKING
    // =============================================
    public Booking createBooking(
            BookingRequest req,
            String userEmail) {

        // Find user
        User user = userRepo
                .findByEmail(userEmail)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"));

        // Find car
        Car car = carRepo
                .findById(req.getCarId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Car not found"));

        // Check if car is blocked
        if (Boolean.TRUE.equals(car.getIsBlocked()))
            throw new RuntimeException(
                    "Car is blocked due to " +
                            "expired documents");

        // Check if car is available
        if (Boolean.FALSE.equals(car.getIsAvailable()))
            throw new RuntimeException(
                    "Car is not available");

        // Check for booking conflicts
        List<Booking> conflicts =
                bookingRepo.findOverlappingBookings(
                        car.getId(),
                        req.getStartDatetime(),
                        req.getEndDatetime());
        if (!conflicts.isEmpty())
            throw new RuntimeException(
                    "Car already booked for " +
                            "this time slot");

        // Calculate total price
        BigDecimal totalAmount = calculatePrice(
                req.getBookingType(),
                req.getStartDatetime(),
                req.getEndDatetime(),
                car);

        // Allocate driver if requested
        Driver driver = null;
        if (req.isWithDriver()) {
            driver = allocateDriver(
                    car,
                    req.getStartDatetime(),
                    req.getEndDatetime());
            if (driver == null)
                throw new RuntimeException(
                        "No driver available. " +
                                "Try without driver or " +
                                "choose different time.");
        }

        // Build and save booking
        Booking booking = Booking.builder()
                .user(user)
                .car(car)
                .driver(driver)
                .bookingType(req.getBookingType())
                .startDatetime(req.getStartDatetime())
                .endDatetime(req.getEndDatetime())
                .pickupLocation(req.getPickupLocation())
                .dropLocation(req.getDropLocation())
                .totalAmount(totalAmount)
                .status(Booking.BookingStatus.CONFIRMED)
                .paymentStatus(Booking.PaymentStatus.PENDING)
                .build();

        Booking saved = bookingRepo.save(booking);

        log.info(
                "Booking #{} created - Car: {}," +
                        " User: {}, Driver: {}",
                saved.getId(),
                car.getName(),
                user.getEmail(),
                driver != null ?
                        driver.getName() : "No driver");

        // ── Send confirmation email to USER ──
        sendBookingConfirmationToUser(
                saved, user, car, driver);

        // ── Notify ADMIN about new booking ──
        sendNewBookingAlertToAdmin(
                saved, user, car, driver);

        return saved;
    }

    // =============================================
    // EMAIL TO USER - BOOKING CONFIRMATION
    // This sends to the USER's own email address
    // e.g. john@gmail.com gets the booking details
    // =============================================
    private void sendBookingConfirmationToUser(
            Booking booking,
            User user,
            Car car,
            Driver driver) {
        try {
            String driverSection;
            if (driver != null) {
                driverSection =
                        "DRIVER DETAILS\n" +
                                "================================\n" +
                                "Driver Name  : " +
                                driver.getName() + "\n" +
                                "Driver Phone : " +
                                driver.getPhone() + "\n" +
                                "Driver Shift : " +
                                driver.getShift() + "\n";
            } else {
                driverSection =
                        "Driver       : No driver " +
                                "(self-drive booking)\n";
            }

            String bookingTypeNote =
                    booking.getBookingType() ==
                            Booking.BookingType.HOURLY
                            ? "Hourly Rental - Pay per hour"
                            : "Daily Rental - Pay per day";

            String subject =
                    "✅ Booking Confirmed! #" +
                            booking.getId() +
                            " - DriveEasy";

            String body =
                    "Dear " + user.getName() + ",\n\n" +
                            "🎉 Your car booking is CONFIRMED!\n\n" +
                            "================================\n" +
                            "BOOKING SUMMARY\n" +
                            "================================\n" +
                            "Booking ID    : #" +
                            booking.getId() + "\n" +
                            "Status        : CONFIRMED\n" +
                            "Booking Type  : " +
                            bookingTypeNote + "\n\n" +
                            "================================\n" +
                            "CAR DETAILS\n" +
                            "================================\n" +
                            "Car Name      : " +
                            car.getName() + "\n" +
                            "Brand         : " +
                            (car.getBrand() != null
                                    ? car.getBrand()
                                    : "N/A") + "\n" +
                            "Car Type      : " +
                            car.getType() + "\n" +
                            "Fuel Type     : " +
                            (car.getFuelType() != null
                                    ? car.getFuelType()
                                    : "N/A") + "\n" +
                            "Transmission  : " +
                            (car.getTransmission() != null
                                    ? car.getTransmission()
                                    : "N/A") + "\n\n" +
                            "================================\n" +
                            "TRIP DETAILS\n" +
                            "================================\n" +
                            "From          : " +
                            booking.getStartDatetime()
                                    .toLocalDate() +
                            " at " +
                            booking.getStartDatetime()
                                    .toLocalTime() + "\n" +
                            "To            : " +
                            booking.getEndDatetime()
                                    .toLocalDate() +
                            " at " +
                            booking.getEndDatetime()
                                    .toLocalTime() + "\n" +
                            "Pickup        : " +
                            (booking.getPickupLocation() != null
                                    ? booking.getPickupLocation()
                                    : "Not specified") + "\n" +
                            "Drop          : " +
                            (booking.getDropLocation() != null
                                    ? booking.getDropLocation()
                                    : "Not specified") + "\n\n" +
                            "================================\n" +
                            driverSection +
                            "\n================================\n" +
                            "PAYMENT DETAILS\n" +
                            "================================\n" +
                            "Total Amount  : Rs." +
                            booking.getTotalAmount() + "\n" +
                            "Payment Status: PENDING\n" +
                            "Payment Mode  : Cash to driver " +
                            "at pickup\n\n" +
                            "================================\n" +
                            "You can track your booking at:\n" +
                            "http://localhost:8080" +
                            "/dashboard.html\n\n" +
                            "Thank you for choosing " +
                            "DriveEasy! Have a safe trip.\n\n" +
                            "DriveEasy Team\n" +
                            "support: pri8870627054@gmail.com";

            // SEND TO USER'S OWN EMAIL
            emailService.sendEmail(
                    user.getEmail(),
                    subject,
                    body);

            log.info(
                    "Booking confirmation email " +
                            "sent to USER: {}",
                    user.getEmail());

        } catch (Exception e) {
            log.error(
                    "Failed to send booking " +
                            "confirmation email to user {}: {}",
                    user.getEmail(),
                    e.getMessage());
        }
    }

    // =============================================
    // EMAIL TO ADMIN - NEW BOOKING ALERT
    // =============================================
    private void sendNewBookingAlertToAdmin(
            Booking booking,
            User user,
            Car car,
            Driver driver) {
        try {
            String subject =
                    "🚗 New Booking #" +
                            booking.getId() +
                            " - " + car.getName() +
                            " by " + user.getName();

            String body =
                    "Dear Admin,\n\n" +
                            "A new booking has been made!\n\n" +
                            "================================\n" +
                            "CUSTOMER DETAILS\n" +
                            "================================\n" +
                            "Name    : " + user.getName() + "\n" +
                            "Email   : " + user.getEmail() + "\n" +
                            "Phone   : " +
                            (user.getPhone() != null
                                    ? user.getPhone()
                                    : "N/A") + "\n\n" +
                            "================================\n" +
                            "BOOKING DETAILS\n" +
                            "================================\n" +
                            "Booking ID : #" +
                            booking.getId() + "\n" +
                            "Car        : " +
                            car.getName() +
                            (car.getBrand() != null
                                    ? " (" + car.getBrand() + ")"
                                    : "") + "\n" +
                            "Type       : " +
                            booking.getBookingType() + "\n" +
                            "From       : " +
                            booking.getStartDatetime()
                                    .toLocalDate() + "\n" +
                            "To         : " +
                            booking.getEndDatetime()
                                    .toLocalDate() + "\n" +
                            "Driver     : " +
                            (driver != null
                                    ? driver.getName() +
                                    " (" +
                                    driver.getPhone() +
                                    ")"
                                    : "No driver") + "\n" +
                            "Amount     : Rs." +
                            booking.getTotalAmount() + "\n" +
                            "Payment    : PENDING\n\n" +
                            "================================\n" +
                            "View in Admin Panel:\n" +
                            "http://localhost:8080" +
                            "/admin.html\n\n" +
                            "DriveEasy System";

            emailService.sendEmail(
                    adminEmail, subject, body);

            log.info(
                    "New booking alert sent " +
                            "to admin for booking #{}",
                    booking.getId());

        } catch (Exception e) {
            log.error(
                    "Failed to send new booking " +
                            "alert to admin: {}",
                    e.getMessage());
        }
    }

    // =============================================
    // CANCEL BOOKING
    // =============================================
    public Booking cancelBooking(
            Long bookingId, String email) {

        Booking booking = bookingRepo
                .findById(bookingId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Booking not found"));

        if (!booking.getUser().getEmail()
                .equals(email))
            throw new RuntimeException(
                    "Not authorized");

        if (booking.getStatus() ==
                Booking.BookingStatus.COMPLETED)
            throw new RuntimeException(
                    "Cannot cancel completed booking");

        booking.setStatus(
                Booking.BookingStatus.CANCELLED);

        Booking cancelled = bookingRepo.save(booking);

        // ── Send cancellation email to USER ──
        sendCancellationEmailToUser(cancelled);

        return cancelled;
    }

    // =============================================
    // EMAIL TO USER - CANCELLATION CONFIRMATION
    // =============================================
    private void sendCancellationEmailToUser(
            Booking booking) {
        try {
            User user = booking.getUser();
            Car  car  = booking.getCar();

            String subject =
                    "❌ Booking Cancelled #" +
                            booking.getId() +
                            " - DriveEasy";

            String body =
                    "Dear " + user.getName() + ",\n\n" +
                            "Your booking has been CANCELLED.\n\n" +
                            "================================\n" +
                            "CANCELLED BOOKING DETAILS\n" +
                            "================================\n" +
                            "Booking ID  : #" +
                            booking.getId() + "\n" +
                            "Car         : " +
                            car.getName() +
                            (car.getBrand() != null
                                    ? " (" + car.getBrand() + ")"
                                    : "") + "\n" +
                            "From        : " +
                            booking.getStartDatetime()
                                    .toLocalDate() + "\n" +
                            "To          : " +
                            booking.getEndDatetime()
                                    .toLocalDate() + "\n" +
                            "Amount      : Rs." +
                            booking.getTotalAmount() + "\n\n" +
                            "================================\n" +
                            "If you did NOT cancel this " +
                            "booking, please contact us " +
                            "immediately.\n\n" +
                            "Book a new car anytime at:\n" +
                            "http://localhost:8080/cars.html\n\n" +
                            "Thank you for choosing " +
                            "DriveEasy!\n\n" +
                            "DriveEasy Team\n" +
                            "support: pri8870627054@gmail.com";

            // SEND TO USER'S OWN EMAIL
            emailService.sendEmail(
                    user.getEmail(),
                    subject,
                    body);

            log.info(
                    "Cancellation email sent " +
                            "to USER: {}",
                    user.getEmail());

        } catch (Exception e) {
            log.error(
                    "Failed to send cancellation " +
                            "email: {}",
                    e.getMessage());
        }
    }

    // =============================================
    // HELPER METHODS
    // =============================================
    private Driver allocateDriver(
            Car car,
            LocalDateTime start,
            LocalDateTime end) {

        Driver.Shift shift = getShiftFromTime(start);

        log.info(
                "Allocating driver for car: {}" +
                        ", shift: {}",
                car.getName(), shift);

        Driver permanentDriver =
                getPermanentDriver(car, shift);

        if (permanentDriver != null) {
            if (isDriverAvailableForBooking(
                    permanentDriver, start, end)) {
                log.info(
                        "Using permanent driver: {}",
                        permanentDriver.getName());
                return permanentDriver;
            } else {
                log.info(
                        "Permanent driver {} " +
                                "not available",
                        permanentDriver.getName());
                notifyAdminDriverUnavailable(
                        car, permanentDriver, shift);
            }
        }

        return findFreeUnassignedDriver(
                shift, permanentDriver, start, end);
    }

    private void notifyAdminDriverUnavailable(
            Car car,
            Driver driver,
            Driver.Shift shift) {
        try {
            emailService.sendEmail(
                    adminEmail,
                    "⚠️ No Driver for Car: " +
                            car.getName(),
                    "Dear Admin,\n\n" +
                            "The permanent " + shift +
                            " driver for:\n" +
                            "Car: " + car.getName() +
                            " (ID: " + car.getId() + ")\n" +
                            "Driver: " + driver.getName() +
                            " (ID: " + driver.getId() + ")\n\n" +
                            "is UNAVAILABLE.\n\n" +
                            "System will auto-assign " +
                            "a free driver.\n\n" +
                            "http://localhost:8080" +
                            "/admin.html\n\n" +
                            "DriveEasy System");
        } catch (Exception e) {
            log.error(
                    "Failed to notify admin: {}",
                    e.getMessage());
        }
    }

    private Driver findFreeUnassignedDriver(
            Driver.Shift preferredShift,
            Driver permanentDriver,
            LocalDateTime start,
            LocalDateTime end) {

        List<Car> allCars = carRepo.findAll();
        java.util.Set<Long> assignedDriverIds =
                new java.util.HashSet<>();
        for (Car c : allCars) {
            if (c.getMorningDriver() != null)
                assignedDriverIds.add(
                        c.getMorningDriver().getId());
            if (c.getEveningDriver() != null)
                assignedDriverIds.add(
                        c.getEveningDriver().getId());
            if (c.getNightDriver() != null)
                assignedDriverIds.add(
                        c.getNightDriver().getId());
        }

        Long excludeId = permanentDriver != null
                ? permanentDriver.getId() : null;

        List<Driver> allDrivers = driverRepo.findAll();
        Driver best = null;
        long minBookings = Long.MAX_VALUE;

        // First: unassigned same shift
        for (Driver d : allDrivers) {
            if (excludeId != null &&
                    d.getId().equals(excludeId))
                continue;
            if (assignedDriverIds.contains(d.getId()))
                continue;
            if (d.getShift() != preferredShift)
                continue;
            if (!isDriverAvailableForBooking(
                    d, start, end))
                continue;
            long count =
                    getActiveBookingCount(d.getId());
            if (count < minBookings) {
                minBookings = count;
                best = d;
            }
        }

        // Second: unassigned any shift
        if (best == null) {
            for (Driver d : allDrivers) {
                if (excludeId != null &&
                        d.getId().equals(excludeId))
                    continue;
                if (assignedDriverIds.contains(
                        d.getId()))
                    continue;
                if (!isDriverAvailableForBooking(
                        d, start, end))
                    continue;
                long count =
                        getActiveBookingCount(d.getId());
                if (count < minBookings) {
                    minBookings = count;
                    best = d;
                }
            }
        }

        // Last resort: any available driver
        if (best == null) {
            for (Driver d : allDrivers) {
                if (excludeId != null &&
                        d.getId().equals(excludeId))
                    continue;
                if (!isDriverAvailableForBooking(
                        d, start, end))
                    continue;
                long count =
                        getActiveBookingCount(d.getId());
                if (count < minBookings) {
                    minBookings = count;
                    best = d;
                }
            }
        }

        if (best != null)
            log.info(
                    "Free driver allocated: {}",
                    best.getName());
        else
            log.warn("No free driver found");

        return best;
    }

    private Driver getPermanentDriver(
            Car car, Driver.Shift shift) {
        if (shift == Driver.Shift.MORNING)
            return car.getMorningDriver();
        if (shift == Driver.Shift.EVENING)
            return car.getEveningDriver();
        if (shift == Driver.Shift.NIGHT)
            return car.getNightDriver();
        return null;
    }

    private boolean isDriverAvailableForBooking(
            Driver driver,
            LocalDateTime start,
            LocalDateTime end) {

        if (!driver.isTrulyAvailable()) {
            log.info(
                    "Driver {} not truly available",
                    driver.getName());
            return false;
        }

        boolean hasConflict =
                bookingRepo.findAll().stream()
                        .filter(b ->
                                b.getDriver() != null &&
                                        b.getDriver().getId()
                                                .equals(driver.getId()) &&
                                        b.getStatus() !=
                                                Booking.BookingStatus.CANCELLED &&
                                        b.getStatus() !=
                                                Booking.BookingStatus.COMPLETED)
                        .anyMatch(b ->
                                b.getStartDatetime()
                                        .isBefore(end) &&
                                        b.getEndDatetime()
                                                .isAfter(start));

        if (hasConflict) {
            log.info(
                    "Driver {} has booking conflict",
                    driver.getName());
            return false;
        }

        return true;
    }

    private Driver.Shift getShiftFromTime(
            LocalDateTime dateTime) {
        int hour = dateTime.getHour();
        if (hour >= 6 && hour < 14)
            return Driver.Shift.MORNING;
        else if (hour >= 14 && hour < 22)
            return Driver.Shift.EVENING;
        else
            return Driver.Shift.NIGHT;
    }

    private long getActiveBookingCount(
            Long driverId) {
        return bookingRepo.findAll().stream()
                .filter(b ->
                        b.getDriver() != null &&
                                b.getDriver().getId()
                                        .equals(driverId) &&
                                b.getStatus() !=
                                        Booking.BookingStatus.CANCELLED &&
                                b.getStatus() !=
                                        Booking.BookingStatus.COMPLETED)
                .count();
    }

    private BigDecimal calculatePrice(
            Booking.BookingType type,
            LocalDateTime start,
            LocalDateTime end,
            Car car) {
        if (type == Booking.BookingType.HOURLY) {
            long hours = ChronoUnit.HOURS
                    .between(start, end);
            if (hours < 1) hours = 1;
            return car.getRentPerHour()
                    .multiply(
                            BigDecimal.valueOf(hours));
        } else {
            long days = ChronoUnit.DAYS.between(
                    start.toLocalDate(),
                    end.toLocalDate());
            if (days < 1) days = 1;
            return car.getRentPerDay()
                    .multiply(
                            BigDecimal.valueOf(days));
        }
    }

    public List<Booking> getMyBookings(
            String email) {
        User user = userRepo
                .findByEmail(email).orElseThrow();
        return bookingRepo
                .findByUserId(user.getId());
    }
}