package com.ihsanerben.n11_clone_api.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ihsanerben.n11_clone_api.address.entity.Address;
import com.ihsanerben.n11_clone_api.address.repository.AddressRepository;
import com.ihsanerben.n11_clone_api.auth.service.ResendEmailService;
import com.ihsanerben.n11_clone_api.auth.service.TokenService;
import com.ihsanerben.n11_clone_api.category.entity.Category;
import com.ihsanerben.n11_clone_api.category.repository.CategoryRepository;
import com.ihsanerben.n11_clone_api.order.entity.Order;
import com.ihsanerben.n11_clone_api.order.entity.OrderItem;
import com.ihsanerben.n11_clone_api.order.entity.OrderStatus;
import com.ihsanerben.n11_clone_api.order.repository.OrderItemRepository;
import com.ihsanerben.n11_clone_api.order.repository.OrderRepository;
import com.ihsanerben.n11_clone_api.product.entity.Product;
import com.ihsanerben.n11_clone_api.product.repository.ProductRepository;
import com.ihsanerben.n11_clone_api.seller.entity.Seller;
import com.ihsanerben.n11_clone_api.seller.entity.SellerStatus;
import com.ihsanerben.n11_clone_api.seller.repository.SellerRepository;
import com.ihsanerben.n11_clone_api.user.entity.User;
import com.ihsanerben.n11_clone_api.user.entity.UserRole;
import com.ihsanerben.n11_clone_api.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class OrderManagementIT {
  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("app.cors.allowed-origin", () -> "http://localhost:5173");
    registry.add("app.auth.jwt-secret", () -> "integration-test-secret-key-at-least-32-characters");
    registry.add("app.auth.access-expiration-ms", () -> "1200000");
    registry.add("app.auth.refresh-expiration-days", () -> "14");
    registry.add("app.auth.verification-expiration-hours", () -> "24");
    registry.add("app.auth.password-reset-expiration-minutes", () -> "60");
    registry.add("app.auth.refresh-cookie-name", () -> "refreshToken");
    registry.add("app.auth.cookie-secure", () -> "false");
    registry.add("app.email.api-key", () -> "test");
    registry.add("app.email.from", () -> "test@example.com");
    registry.add("app.email.frontend-base-url", () -> "http://localhost:5173");
  }

  @Autowired MockMvc mvc;
  @Autowired UserRepository users;
  @Autowired SellerRepository sellers;
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;
  @Autowired AddressRepository addresses;
  @Autowired OrderRepository orders;
  @Autowired OrderItemRepository orderItems;
  @Autowired TokenService tokens;
  @MockitoBean ResendEmailService email;

  @Test
  void buyerShouldSeeOnlyOwnOrdersAndCancelEligibleOrder() throws Exception {
    User buyer = user("management-buyer", UserRole.USER);
    User otherBuyer = user("management-other-buyer", UserRole.USER);
    OrderFixture own = order("Buyer Order", "Buyer Store", buyer, OrderStatus.PENDING, 3, 2);
    OrderFixture other =
        order("Other Order", "Other Buyer Store", otherBuyer, OrderStatus.PENDING, 4, 1);
    String buyerToken = tokens.createAccessToken(buyer);

    mvc.perform(get("/api/orders").header("Authorization", bearer(buyerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(own.order().getId()));
    mvc.perform(
            get("/api/orders/{id}", other.order().getId())
                .header("Authorization", bearer(buyerToken)))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/orders/{id}/cancel", own.order().getId())
                .header("Authorization", bearer(buyerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    assertThat(products.findById(own.product().getId()).orElseThrow().getStockQuantity())
        .isEqualTo(5);
    mvc.perform(
            post("/api/orders/{id}/cancel", own.order().getId())
                .header("Authorization", bearer(buyerToken)))
        .andExpect(status().isConflict());
  }

  @Test
  void sellerShouldFollowForwardStatusTransitionsOnOwnOrders() throws Exception {
    User buyer = user("seller-flow-buyer", UserRole.USER);
    OrderFixture fixture =
        order("Seller Flow Product", "Seller Flow Store", buyer, OrderStatus.PENDING, 5, 1);
    User sellerUser = fixture.order().getSeller().getUser();
    String sellerToken = tokens.createAccessToken(sellerUser);
    User outsider = user("seller-flow-outsider", UserRole.SELLER);
    seller(outsider, "Outsider Store");

    mvc.perform(get("/api/seller/orders").header("Authorization", bearer(sellerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(fixture.order().getId()));
    updateSeller(fixture.order().getId(), sellerToken, OrderStatus.SHIPPED)
        .andExpect(status().isConflict());
    updateSeller(fixture.order().getId(), sellerToken, OrderStatus.CONFIRMED)
        .andExpect(status().isOk());
    updateSeller(fixture.order().getId(), sellerToken, OrderStatus.SHIPPED)
        .andExpect(status().isOk());
    updateSeller(fixture.order().getId(), sellerToken, OrderStatus.DELIVERED)
        .andExpect(status().isOk());
    updateSeller(fixture.order().getId(), sellerToken, OrderStatus.CONFIRMED)
        .andExpect(status().isConflict());
    updateSeller(fixture.order().getId(), tokens.createAccessToken(outsider), OrderStatus.CONFIRMED)
        .andExpect(status().isNotFound());
  }

  @Test
  void adminShouldListAndOverrideOrdersWithInventoryConsistency() throws Exception {
    User buyer = user("admin-order-buyer", UserRole.USER);
    User admin = user("order-admin", UserRole.ADMIN);
    OrderFixture fixture =
        order("Admin Product", "Admin Order Store", buyer, OrderStatus.DELIVERED, 2, 2);
    String adminToken = tokens.createAccessToken(admin);

    mvc.perform(
            get("/api/admin/orders")
                .header("Authorization", bearer(tokens.createAccessToken(buyer))))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/orders").header("Authorization", bearer(adminToken)))
        .andExpect(status().isOk());
    updateAdmin(fixture.order().getId(), adminToken, OrderStatus.CANCELLED)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    assertThat(products.findById(fixture.product().getId()).orElseThrow().getStockQuantity())
        .isEqualTo(4);
    updateAdmin(fixture.order().getId(), adminToken, OrderStatus.SHIPPED)
        .andExpect(status().isOk());
    assertThat(products.findById(fixture.product().getId()).orElseThrow().getStockQuantity())
        .isEqualTo(2);
  }

  @Test
  void checkoutShouldBeRateLimitedPerAuthenticatedUser() throws Exception {
    User buyer = user("rate-limit-buyer", UserRole.USER);
    Address address =
        addresses.saveAndFlush(
            Address.builder()
                .user(buyer)
                .recipientName("Rate Limited Buyer")
                .phone("+90 555 111 22 33")
                .addressLine("Rate Street 1")
                .city("Istanbul")
                .postalCode("34000")
                .defaultAddress(true)
                .build());
    String token = tokens.createAccessToken(buyer);
    for (int request = 0; request < 5; request++) {
      mvc.perform(
              post("/api/orders")
                  .header("Authorization", bearer(token))
                  .contentType(APPLICATION_JSON)
                  .content("{\"addressId\":" + address.getId() + "}"))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(
            post("/api/orders")
                .header("Authorization", bearer(token))
                .contentType(APPLICATION_JSON)
                .content("{\"addressId\":" + address.getId() + "}"))
        .andExpect(status().isTooManyRequests());
  }

  private org.springframework.test.web.servlet.ResultActions updateSeller(
      Long orderId, String token, OrderStatus status) throws Exception {
    return mvc.perform(
        put("/api/seller/orders/{id}/status", orderId)
            .header("Authorization", bearer(token))
            .contentType(APPLICATION_JSON)
            .content("{\"status\":\"" + status + "\"}"));
  }

  private org.springframework.test.web.servlet.ResultActions updateAdmin(
      Long orderId, String token, OrderStatus status) throws Exception {
    return mvc.perform(
        put("/api/admin/orders/{id}/status", orderId)
            .header("Authorization", bearer(token))
            .contentType(APPLICATION_JSON)
            .content("{\"status\":\"" + status + "\"}"));
  }

  private OrderFixture order(
      String productName,
      String storeName,
      User buyer,
      OrderStatus status,
      int stock,
      int quantity) {
    Category category =
        categories.saveAndFlush(Category.builder().name(productName + " Category").build());
    User sellerUser = user(storeName.toLowerCase().replace(" ", "-"), UserRole.SELLER);
    Seller seller = seller(sellerUser, storeName);
    Instant now = Instant.now();
    Product product =
        products.saveAndFlush(
            Product.builder()
                .seller(seller)
                .category(category)
                .name(productName)
                .price(BigDecimal.TEN)
                .stockQuantity(stock)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build());
    Order order =
        orders.saveAndFlush(
            Order.builder()
                .buyer(buyer)
                .seller(seller)
                .status(status)
                .totalAmount(BigDecimal.TEN.multiply(BigDecimal.valueOf(quantity)))
                .recipientName("Order Recipient")
                .phone("+90 555 111 22 33")
                .addressLine("Order Street 1")
                .city("Istanbul")
                .postalCode("34000")
                .createdAt(now)
                .build());
    orderItems.saveAndFlush(
        OrderItem.builder()
            .order(order)
            .product(product)
            .quantity(quantity)
            .unitPrice(BigDecimal.TEN)
            .build());
    return new OrderFixture(order, product);
  }

  private Seller seller(User user, String storeName) {
    return sellers.saveAndFlush(
        Seller.builder()
            .user(user)
            .storeName(storeName)
            .status(SellerStatus.APPROVED)
            .appliedAt(Instant.now())
            .reviewedAt(Instant.now())
            .build());
  }

  private User user(String username, UserRole role) {
    return users.saveAndFlush(
        User.builder()
            .username(username)
            .email(username + "@example.com")
            .password("$2a$10$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu")
            .role(role)
            .emailVerified(true)
            .createdAt(Instant.now())
            .build());
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  private record OrderFixture(Order order, Product product) {}
}
