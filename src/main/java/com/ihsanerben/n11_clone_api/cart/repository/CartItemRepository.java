package com.ihsanerben.n11_clone_api.cart.repository;

import com.ihsanerben.n11_clone_api.cart.entity.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
  @EntityGraph(attributePaths = {"product", "product.seller"})
  List<CartItem> findAllByCartUserIdOrderById(Long userId);

  @EntityGraph(attributePaths = {"product", "product.seller"})
  Optional<CartItem> findByIdAndCartUserId(Long id, Long userId);

  Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

  void deleteAllByCartUserId(Long userId);
}
