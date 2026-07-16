package com.ihsanerben.n11_clone_api.address.controller;

import com.ihsanerben.n11_clone_api.address.dto.AddressRequest;
import com.ihsanerben.n11_clone_api.address.dto.AddressResponse;
import com.ihsanerben.n11_clone_api.address.service.AddressService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {
  private final AddressService service;

  @GetMapping
  public List<AddressResponse> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  public AddressResponse get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AddressResponse create(@Valid @RequestBody AddressRequest request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public AddressResponse update(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
