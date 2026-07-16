package com.ihsanerben.n11_clone_api.order.entity;

import com.ihsanerben.n11_clone_api.seller.entity.Seller;
import com.ihsanerben.n11_clone_api.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "buyer_id")
  private User buyer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "seller_id")
  private Seller seller;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrderStatus status;

  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount;

  @Column(name = "recipient_name", nullable = false, length = 150)
  private String recipientName;

  @Column(nullable = false, length = 30)
  private String phone;

  @Column(name = "address_line", nullable = false, length = 500)
  private String addressLine;

  @Column(nullable = false, length = 100)
  private String city;

  @Column(name = "postal_code", nullable = false, length = 20)
  private String postalCode;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
