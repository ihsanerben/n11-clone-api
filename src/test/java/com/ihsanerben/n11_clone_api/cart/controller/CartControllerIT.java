package com.ihsanerben.n11_clone_api.cart.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ihsanerben.n11_clone_api.auth.service.ResendEmailService;
import com.ihsanerben.n11_clone_api.auth.service.TokenService;
import com.ihsanerben.n11_clone_api.cart.entity.CartItem;
import com.ihsanerben.n11_clone_api.cart.repository.CartItemRepository;
import com.ihsanerben.n11_clone_api.category.entity.Category;
import com.ihsanerben.n11_clone_api.category.repository.CategoryRepository;
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
import java.util.List;
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
class CartControllerIT {
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
  @Autowired CartItemRepository cartItems;
  @Autowired TokenService tokens;
  @MockitoBean ResendEmailService email;

  @Test
  void shouldManageCartAcrossSellersWithoutExceedingStock() throws Exception {
    User buyer = user("cart-buyer", UserRole.USER);
    User otherBuyer = user("other-cart-buyer", UserRole.USER);
    Category category = categories.saveAndFlush(Category.builder().name("Cart Category").build());
    Product first = product("First Product", "First Store", category, 3, BigDecimal.TEN);
    Product second = product("Second Product", "Second Store", category, 5, BigDecimal.valueOf(20));
    String buyerToken = tokens.createAccessToken(buyer);
    String otherToken = tokens.createAccessToken(otherBuyer);

    add(buyerToken, first.getId(), 2).andExpect(status().isCreated());
    add(buyerToken, first.getId(), 2).andExpect(status().isConflict());
    add(buyerToken, second.getId(), 1)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.totalAmount").value(40))
        .andExpect(jsonPath("$.items[0].storeName").value("First Store"))
        .andExpect(jsonPath("$.items[1].storeName").value("Second Store"));

    List<CartItem> buyerItems = cartItems.findAllByCartUserIdOrderById(buyer.getId());
    Long firstItemId = buyerItems.getFirst().getId();
    mvc.perform(
            put("/api/cart/items/{itemId}", firstItemId)
                .header("Authorization", bearer(otherToken))
                .contentType(APPLICATION_JSON)
                .content("{\"quantity\":1}"))
        .andExpect(status().isNotFound());
    mvc.perform(
            put("/api/cart/items/{itemId}", firstItemId)
                .header("Authorization", bearer(buyerToken))
                .contentType(APPLICATION_JSON)
                .content("{\"quantity\":4}"))
        .andExpect(status().isConflict());
    mvc.perform(
            put("/api/cart/items/{itemId}", firstItemId)
                .header("Authorization", bearer(buyerToken))
                .contentType(APPLICATION_JSON)
                .content("{\"quantity\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalAmount").value(30));

    mvc.perform(
            delete("/api/cart/items/{itemId}", firstItemId)
                .header("Authorization", bearer(buyerToken)))
        .andExpect(status().isNoContent());
    mvc.perform(delete("/api/cart").header("Authorization", bearer(buyerToken)))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/cart").header("Authorization", bearer(buyerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0))
        .andExpect(jsonPath("$.totalAmount").value(0));
  }

  private org.springframework.test.web.servlet.ResultActions add(
      String token, Long productId, int quantity) throws Exception {
    return mvc.perform(
        post("/api/cart/items")
            .header("Authorization", bearer(token))
            .contentType(APPLICATION_JSON)
            .content("{\"productId\":" + productId + ",\"quantity\":" + quantity + "}"));
  }

  private Product product(
      String name, String storeName, Category category, int stock, BigDecimal price) {
    User sellerUser = user(storeName.toLowerCase().replace(" ", "-"), UserRole.SELLER);
    Seller seller =
        sellers.saveAndFlush(
            Seller.builder()
                .user(sellerUser)
                .storeName(storeName)
                .status(SellerStatus.APPROVED)
                .appliedAt(Instant.now())
                .reviewedAt(Instant.now())
                .build());
    Instant now = Instant.now();
    return products.saveAndFlush(
        Product.builder()
            .seller(seller)
            .category(category)
            .name(name)
            .price(price)
            .stockQuantity(stock)
            .active(true)
            .createdAt(now)
            .updatedAt(now)
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
}
