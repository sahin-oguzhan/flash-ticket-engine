package com.oguzhan.flash_ticket_engine.repository;

import com.oguzhan.flash_ticket_engine.domain.entity.TicketNaive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketNaiveRepository extends JpaRepository<TicketNaive, Long> {
}
