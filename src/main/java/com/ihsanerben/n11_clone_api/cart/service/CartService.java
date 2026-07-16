package com.ihsanerben.n11_clone_api.cart.service;

import com.ihsanerben.n11_clone_api.cart.dto.AddCartItemRequest;
import com.ihsanerben.n11_clone_api.cart.dto.CartResponse;
import com.ihsanerben.n11_clone_api.cart.dto.UpdateCartItemRequest;
import com.ihsanerben.n11_clone_api.cart.entity.Cart;
import com.ihsanerben.n11_clone_api.cart.entity.CartItem;
import com.ihsanerben.n11_clone_api.cart.exception.CartItemNotFoundException;
import com.ihsanerben.n11_clone_api.cart.exception.CartStockException;
import com.ihsanerben.n11_clone_api.cart.mapper.CartMapper;
import com.ihsanerben.n11_clone_api.cart.repository.CartItemRepository;
import com.ihsanerben.n11_clone_api.cart.repository.CartRepository;
import com.ihsanerben.n11_clone_api.common.security.AuthenticatedUserProvider;
import com.ihsanerben.n11_clone_api.product.entity.Product;
import com.ihsanerben.n11_clone_api.product.exception.ProductNotFoundException;
import com.ihsanerben.n11_clone_api.product.repository.ProductRepository;
import com.ihsanerben.n11_clone_api.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {
  private final CartRepository carts;
  private final CartItemRepository cartItems;
  private final ProductRepository products;
  private final UserRepository users;
  private final CartMapper mapper;
  private final AuthenticatedUserProvider authenticatedUser;

  @Transactional(readOnly = true)
  public CartResponse get() {
    Long userId = userId();
    Long cartId = carts.findByUserId(userId).map(Cart::getId).orElse(null);
    if (cartId == null) {
      return new CartResponse(null, List.of(), BigDecimal.ZERO);
    }
    return mapper.toResponse(cartId, cartItems.findAllByCartUserIdOrderById(userId));
  }

  @Transactional
  public CartResponse add(AddCartItemRequest request) {
    Long userId = userId();
    Product product =
        products.findById(request.productId()).orElseThrow(ProductNotFoundException::new);
    ensureAvailable(product);
    Cart cart = findOrCreateCart(userId);
    CartItem item =
        cartItems
            .findByCartIdAndProductId(cart.getId(), product.getId())
            .orElseGet(() -> CartItem.builder().cart(cart).product(product).quantity(0).build());
    int newQuantity = item.getQuantity() + request.quantity();
    ensureStock(product, newQuantity);
    item.setQuantity(newQuantity);
    cartItems.save(item);
    return response(userId, cart.getId());
  }

  @Transactional
  public CartResponse update(Long itemId, UpdateCartItemRequest request) {
    Long userId = userId();
    CartItem item = ownedItem(itemId, userId);
    ensureAvailable(item.getProduct());
    ensureStock(item.getProduct(), request.quantity());
    item.setQuantity(request.quantity());
    return response(userId, item.getCart().getId());
  }

  @Transactional
  public void removeItem(Long itemId) {
    cartItems.delete(ownedItem(itemId, userId()));
  }

  @Transactional
  public void clear() {
    cartItems.deleteAllByCartUserId(userId());
  }

  private Cart findOrCreateCart(Long userId) {
    return carts
        .findByUserId(userId)
        .orElseGet(() -> carts.save(Cart.builder().user(users.getReferenceById(userId)).build()));
  }

  private CartItem ownedItem(Long itemId, Long userId) {
    return cartItems
        .findByIdAndCartUserId(itemId, userId)
        .orElseThrow(CartItemNotFoundException::new);
  }

  private void ensureAvailable(Product product) {
    if (!product.isActive()) {
      throw new CartStockException("Product is not available");
    }
  }

  private void ensureStock(Product product, int quantity) {
    if (quantity > product.getStockQuantity()) {
      throw new CartStockException("Requested quantity exceeds available stock");
    }
  }

  private CartResponse response(Long userId, Long cartId) {
    return mapper.toResponse(cartId, cartItems.findAllByCartUserIdOrderById(userId));
  }

  private Long userId() {
    return authenticatedUser.userId();
  }
}
