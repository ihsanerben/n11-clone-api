package com.ihsanerben.n11_clone_api.product.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ihsanerben.n11_clone_api.auth.service.ResendEmailService;
import com.ihsanerben.n11_clone_api.auth.service.TokenService;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
class ProductCatalogIT {
  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    r.add("spring.datasource.username", POSTGRES::getUsername);
    r.add("spring.datasource.password", POSTGRES::getPassword);
    r.add("app.cors.allowed-origin", () -> "http://localhost:5173");
    r.add("app.auth.jwt-secret", () -> "integration-test-secret-key-at-least-32-characters");
    r.add("app.auth.access-expiration-ms", () -> "1200000");
    r.add("app.auth.refresh-expiration-days", () -> "14");
    r.add("app.auth.verification-expiration-hours", () -> "24");
    r.add("app.auth.password-reset-expiration-minutes", () -> "60");
    r.add("app.auth.refresh-cookie-name", () -> "refreshToken");
    r.add("app.auth.cookie-secure", () -> "false");
    r.add("app.email.api-key", () -> "test");
    r.add("app.email.from", () -> "test@example.com");
    r.add("app.email.frontend-base-url", () -> "http://localhost:5173");
  }

  @Autowired MockMvc mvc;
  @Autowired UserRepository users;
  @Autowired SellerRepository sellers;
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;
  @Autowired TokenService tokens;
  @MockitoBean ResendEmailService email;

  @Test
  void shouldManageCatalogWithOwnershipAndSoftDelete() throws Exception {
    User admin = user("catalog-admin", UserRole.ADMIN);
    User sellerUser = user("catalog-seller", UserRole.SELLER);
    User other = user("other-seller", UserRole.SELLER);
    seller(sellerUser, "Main Store");
    seller(other, "Other Store");
    String adminToken = tokens.createAccessToken(admin);
    String sellerToken = tokens.createAccessToken(sellerUser);
    String otherToken = tokens.createAccessToken(other);
    mvc.perform(
            post("/api/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(APPLICATION_JSON)
                .content("{\"name\":\"Electronics\"}"))
        .andExpect(status().isCreated());
    Long categoryId =
        categories.findAll().stream()
            .filter(category -> category.getName().equals("Electronics"))
            .findFirst()
            .orElseThrow()
            .getId();
    String body =
        "{\"name\":\"Phone\",\"description\":\"Smart phone\",\"price\":999.99,\"stockQuantity\":5,\"categoryId\":"
            + categoryId
            + ",\"imageUrl\":\"https://example.com/phone.jpg\"}";
    mvc.perform(
            post("/api/seller/products")
                .header("Authorization", "Bearer " + sellerToken)
                .contentType(APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());
    Long productId =
        products.findByNameAndSellerUserId("Phone", sellerUser.getId()).orElseThrow().getId();
    mvc.perform(get("/api/products?search=phone&categoryId=" + categoryId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(productId));
    mvc.perform(get("/api/products?search=smart"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(productId));
    mvc.perform(get("/api/products?search=electronics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(productId));
    mvc.perform(
            put("/api/seller/products/{id}", productId)
                .header("Authorization", "Bearer " + otherToken)
                .contentType(APPLICATION_JSON)
                .content(body))
        .andExpect(status().isNotFound());
    mvc.perform(
            delete("/api/seller/products/{id}", productId)
                .header("Authorization", "Bearer " + sellerToken))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/products/{id}", productId)).andExpect(status().isNotFound());
    mvc.perform(
            put("/api/seller/products/{id}/reactivate", productId)
                .header("Authorization", "Bearer " + sellerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void shouldRejectStaleProductUpdate() {
    User owner = user("lock-seller", UserRole.SELLER);
    Seller seller = seller(owner, "Lock Store");
    Category category = categories.saveAndFlush(Category.builder().name("Lock Category").build());
    Instant now = Instant.now();
    Product saved =
        products.saveAndFlush(
            Product.builder()
                .seller(seller)
                .category(category)
                .name("Lock Product")
                .price(BigDecimal.TEN)
                .stockQuantity(2)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build());
    Product first = products.findById(saved.getId()).orElseThrow();
    Product stale = products.findById(saved.getId()).orElseThrow();
    first.setStockQuantity(1);
    products.saveAndFlush(first);
    stale.setStockQuantity(0);
    assertThatThrownBy(() -> products.saveAndFlush(stale))
        .isInstanceOf(ObjectOptimisticLockingFailureException.class);
  }

  private User user(String name, UserRole role) {
    return users.saveAndFlush(
        User.builder()
            .username(name)
            .email(name + "@example.com")
            .password("$2a$10$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu")
            .role(role)
            .emailVerified(true)
            .createdAt(Instant.now())
            .build());
  }

  private Seller seller(User user, String store) {
    return sellers.saveAndFlush(
        Seller.builder()
            .user(user)
            .storeName(store)
            .status(SellerStatus.APPROVED)
            .appliedAt(Instant.now())
            .reviewedAt(Instant.now())
            .build());
  }
}
