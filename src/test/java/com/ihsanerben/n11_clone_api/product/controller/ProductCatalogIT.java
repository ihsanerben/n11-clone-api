package com.ihsanerben.n11_clone_api.product.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ihsanerben.n11_clone_api.auth.service.EmailService;
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
    r.add("app.email.from", () -> "test@example.com");
    r.add("app.email.frontend-base-url", () -> "http://localhost:5173");
  }

  @Autowired MockMvc mvc;
  @Autowired UserRepository users;
  @Autowired SellerRepository sellers;
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;
  @Autowired TokenService tokens;
  @MockitoBean EmailService email;

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

  @Test
  void shouldExposeRoleScopedSellerAndAdminProductListings() throws Exception {
    User sellerUser = user("listing-seller", UserRole.SELLER);
    User otherSellerUser = user("listing-other-seller", UserRole.SELLER);
    User regularUser = user("listing-user", UserRole.USER);
    User admin = user("listing-admin", UserRole.ADMIN);
    Seller seller = seller(sellerUser, "Listing Store");
    Seller otherSeller = seller(otherSellerUser, "Other Listing Store");
    Category category =
        categories.saveAndFlush(Category.builder().name("Listing Category").build());
    Instant base = Instant.now();
    Product active =
        product("IntegrationPhone Active", seller, category, true, base.minusSeconds(10));
    Product inactive = product("IntegrationPhone Inactive", seller, category, false, base);
    product("IntegrationPhone Other", otherSeller, category, true, base.plusSeconds(10));
    String sellerToken = tokens.createAccessToken(sellerUser);
    String regularToken = tokens.createAccessToken(regularUser);
    String adminToken = tokens.createAccessToken(admin);

    mvc.perform(get("/api/seller/products?page=0&size=20&sort=createdAt,desc"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/seller/products").header("Authorization", "Bearer " + regularToken))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/seller/products?page=0&size=20&sort=createdAt,desc")
                .header("Authorization", "Bearer " + sellerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].id").value(inactive.getId()))
        .andExpect(jsonPath("$.content[0].active").value(false))
        .andExpect(jsonPath("$.content[1].id").value(active.getId()))
        .andExpect(jsonPath("$.content[1].active").value(true));

    mvc.perform(get("/api/admin/products").header("Authorization", "Bearer " + sellerToken))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/admin/products?active=false&search=integrationphone&categoryId="
                    + category.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(inactive.getId()));
    mvc.perform(
            get("/api/admin/products?search=integrationphone&categoryId=" + category.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(3));
  }

  private Product product(
      String name, Seller seller, Category category, boolean active, Instant createdAt) {
    return products.saveAndFlush(
        Product.builder()
            .seller(seller)
            .category(category)
            .name(name)
            .description("Frontend integration product")
            .price(BigDecimal.TEN)
            .stockQuantity(5)
            .active(active)
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .build());
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
