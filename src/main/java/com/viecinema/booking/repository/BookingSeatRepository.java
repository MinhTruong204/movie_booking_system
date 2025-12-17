package com.viecinema.booking.repository;

import com.viecinema.booking.entity.Booking;
import com.viecinema.booking.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.jar.JarFile;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Integer> {
    List<BookingSeat> findAllByBooking(Booking booking);
}
