package com.carbooking.repository;
import com.carbooking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Find all bookings for a user
    List<Booking> findByUserId(Long userId);

    // Check for overlapping bookings (conflict detection)
    @Query("SELECT b FROM Booking b WHERE b.car.id = :carId " +
            "AND b.status NOT IN ('CANCELLED', 'COMPLETED') " +
            "AND b.startDatetime < :endDt " +
            "AND b.endDatetime > :startDt")
    List<Booking> findOverlappingBookings(
            @Param("carId") Long carId,
            @Param("startDt") LocalDateTime startDt,
            @Param("endDt") LocalDateTime endDt
    );

    // Find bookings by status
    List<Booking> findByStatus(Booking.BookingStatus status);

    // Find bookings for admin stats
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = 'CONFIRMED'")
    Long countActiveBookings();
    // Count active bookings for a driver
    @Query("SELECT COUNT(b) FROM Booking b " +
            "WHERE b.driver.id = :driverId " +
            "AND b.status NOT IN " +
            "('CANCELLED', 'COMPLETED')")
    Long countActiveBookingsByDriver(
            @Param("driverId") Long driverId);

    // Find bookings by driver
    List<Booking> findByDriverId(Long driverId);
}

