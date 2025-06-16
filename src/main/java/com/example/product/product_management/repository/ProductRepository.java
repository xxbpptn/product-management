package com.example.product.product_management.repository;

import com.example.product.product_management.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable

public interface ProductRepository extends JpaRepository<Product, Long> {
    // เมธอดนี้จะถูก implements โดย Spring Data JPA โดยอัตโนมัติ
    // มันจะค้นหาสินค้าที่ชื่อมี String ที่กำหนดอยู่ โดยไม่คำนึงถึงตัวพิมพ์เล็ก-ใหญ่
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
}