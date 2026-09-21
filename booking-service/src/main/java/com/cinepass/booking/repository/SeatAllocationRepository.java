package com.cinepass.booking.repository;

import com.cinepass.booking.entity.SeatAllocation;
import org.springframework.data.jpa.repository.*;
import java.util.List;

public interface SeatAllocationRepository extends JpaRepository<SeatAllocation, Long> {
    List<SeatAllocation> findByBookingId(Long bookingId);
    List<SeatAllocation> findByShowId(Long showId);
    void deleteByBookingId(Long bookingId);
}
