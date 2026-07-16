package com.ihsanerben.n11_clone_api.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ihsanerben.n11_clone_api.address.entity.Address;
import com.ihsanerben.n11_clone_api.address.repository.AddressRepository;
import com.ihsanerben.n11_clone_api.auth.service.ResendEmailService;
import com.ihsanerben.n11_clone_api.auth.service.TokenService;
import com.ihsanerben.n11_clone_api.cart.entity.Cart;
import com.ihsanerben.n11_clone_api.cart.entity.CartItem;
import com.ihsanerben.n11_clone_api.cart.repository.CartItemRepository;
import com.ihsanerben.n11_clone_api.cart.repository.CartRepository;
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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
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
class CheckoutControllerIT {
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
  @Autowired CartRepository carts;
  @Autowired CartItemRepository cartItems;
  @Autowired OrderRepository orders;
  @Autowired OrderItemRepository orderItems;
  @Autowired TokenService tokens;
  @MockitoBean ResendEmailService email;

  @Test
  void shouldCreateOneOrderPerSellerWithSnapshots() throws Exception {
    User buyer = user("checkout-buyer", UserRole.USER);
    Address address = address(buyer, "Snapshot Recipient");
    Category category = category("Checkout Category");
    Product first = product("Checkout First", "Checkout Store A", category, 5, "10.50");
    Product second = product("Checkout Second", "Checkout Store B", category, 4, "20.00");
    cart(buyer, List.of(item(first, 2), item(second, 1)));

    mvc.perform(
            post("/api/orders")
                .header("Authorization", bearer(tokens.createAccessToken(buyer)))
                .contentType(APPLICATION_JSON)
                .content("{\"addressId\":" + address.getId() + "}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orders.length()").value(2))
        .andExpect(jsonPath("$.orders[0].status").value("PENDING"))
        .andExpect(jsonPath("$.orders[0].totalAmount").value(21.0))
        .andExpect(jsonPath("$.orders[1].totalAmount").value(20.0));

    List<Order> buyerOrders =
        orders.findAll().stream()
            .filter(order -> order.getBuyer().getId().equals(buyer.getId()))
            .toList();
    assertThat(buyerOrders).hasSize(2);
    assertThat(buyerOrders).allMatch(order -> order.getStatus() == OrderStatus.PENDING);
    assertThat(buyerOrders)
        .allMatch(order -> order.getRecipientName().equals("Snapshot Recipient"));
    assertThat(cartItems.findAllByCartUserIdOrderById(buyer.getId())).isEmpty();
    assertThat(products.findById(first.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);
    assertThat(products.findById(second.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);
    List<OrderItem> snapshots =
        buyerOrders.stream()
            .flatMap(order -> orderItems.findAllByOrderIdOrderById(order.getId()).stream())
            .toList();
    assertThat(snapshots)
        .extracting(OrderItem::getUnitPrice)
        .containsExactlyInAnyOrder(new BigDecimal("10.50"), new BigDecimal("20.00"));
  }

  @Test
  void shouldRollbackCheckoutWhenAnyItemIsInvalid() throws Exception {
    User buyer = user("rollback-buyer", UserRole.USER);
    Address address = address(buyer, "Rollback Recipient");
    Category category = category("Rollback Category");
    Product available = product("Available Product", "Rollback Store A", category, 5, "15.00");
    Product insufficient =
        product("Insufficient Product", "Rollback Store B", category, 1, "30.00");
    cart(buyer, List.of(item(available, 2), item(insufficient, 2)));
    long orderCount = orders.count();

    mvc.perform(
            post("/api/orders")
                .header("Authorization", bearer(tokens.createAccessToken(buyer)))
                .contentType(APPLICATION_JSON)
                .content("{\"addressId\":" + address.getId() + "}"))
        .andExpect(status().isConflict());

    assertThat(orders.count()).isEqualTo(orderCount);
    assertThat(products.findById(available.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);
    assertThat(products.findById(insufficient.getId()).orElseThrow().getStockQuantity())
        .isEqualTo(1);
    assertThat(cartItems.findAllByCartUserIdOrderById(buyer.getId())).hasSize(2);
  }

  @Test
  void shouldPreventOversellingDuringConcurrentCheckout() throws Exception {
    Category category = category("Concurrent Category");
    Product product = product("Last Product", "Concurrent Store", category, 1, "40.00");
    User firstBuyer = user("concurrent-buyer-a", UserRole.USER);
    User secondBuyer = user("concurrent-buyer-b", UserRole.USER);
    Address firstAddress = address(firstBuyer, "First Concurrent");
    Address secondAddress = address(secondBuyer, "Second Concurrent");
    cart(firstBuyer, List.of(item(product, 1)));
    cart(secondBuyer, List.of(item(product, 1)));
    String firstToken = tokens.createAccessToken(firstBuyer);
    String secondToken = tokens.createAccessToken(secondBuyer);
    CountDownLatch start = new CountDownLatch(1);

    List<Integer> statuses;
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(() -> checkoutStatus(start, firstToken, firstAddress.getId()));
      var second = executor.submit(() -> checkoutStatus(start, secondToken, secondAddress.getId()));
      start.countDown();
      statuses = List.of(first.get(), second.get());
    }

    assertThat(statuses).containsExactlyInAnyOrder(201, 409);
    assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isZero();
    long created =
        orders.findAll().stream()
            .filter(order -> order.getSeller().getId().equals(product.getSeller().getId()))
            .count();
    assertThat(created).isEqualTo(1);
  }

  private int checkoutStatus(CountDownLatch start, String token, Long addressId) throws Exception {
    start.await();
    return mvc.perform(
            post("/api/orders")
                .header("Authorization", bearer(token))
                .contentType(APPLICATION_JSON)
                .content("{\"addressId\":" + addressId + "}"))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private void cart(User buyer, List<CartItem> unsavedItems) {
    Cart cart = carts.saveAndFlush(Cart.builder().user(buyer).build());
    unsavedItems.forEach(item -> item.setCart(cart));
    cartItems.saveAllAndFlush(unsavedItems);
  }

  private CartItem item(Product product, int quantity) {
    return CartItem.builder().product(product).quantity(quantity).build();
  }

  private Address address(User buyer, String recipient) {
    return addresses.saveAndFlush(
        Address.builder()
            .user(buyer)
            .label("Home")
            .recipientName(recipient)
            .phone("+90 555 111 22 33")
            .addressLine("Snapshot Street 1")
            .city("Istanbul")
            .postalCode("34000")
            .defaultAddress(true)
            .build());
  }

  private Category category(String name) {
    return categories.saveAndFlush(Category.builder().name(name).build());
  }

  private Product product(
      String name, String storeName, Category category, int stock, String price) {
    String username = storeName.toLowerCase().replace(" ", "-");
    User sellerUser = user(username, UserRole.SELLER);
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
            .price(new BigDecimal(price))
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
