package com.viecinema.booking.repository;

import com.viecinema.booking.entity.BookingCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingComboRepository extends JpaRepository<BookingCombo, Integer> {
}
