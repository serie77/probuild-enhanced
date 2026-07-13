package com.probuild.platform.data.repo;

import com.probuild.platform.data.entity.TradeCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeCardRepository extends JpaRepository<TradeCard, String> {
    List<TradeCard> findByCustomerEmailIgnoreCase(String customerEmail);
}
