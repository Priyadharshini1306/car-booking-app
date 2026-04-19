package com.carbooking.dto;
import com.carbooking.entity.Booking.BookingType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingRequest {
    @NotNull private Long carId;
    @NotNull private BookingType bookingType;
    @NotNull private LocalDateTime startDatetime;
    @NotNull private LocalDateTime endDatetime;
    private String pickupLocation;
    private String dropLocation;
    private boolean withDriver = false;
}
