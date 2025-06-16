package com.example.product.product_management.service;

import com.example.product.product_management.model.Product;
import com.example.product.product_management.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID; // สำหรับสร้างชื่อไฟล์ที่ไม่ซ้ำกัน

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final String uploadDir = "uploads"; // โฟลเดอร์สำหรับเก็บไฟล์

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
        // ตรวจสอบและสร้างโฟลเดอร์ uploads ถ้ายังไม่มี
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    public Page<Product> getAllProductsPaged(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Page<Product> searchProductsByNamePaged(String name, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product, MultipartFile image) throws IOException {
        if (image != null && !image.isEmpty()) {
            String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
            Path filePath = Paths.get(uploadDir, fileName);
            Files.copy(image.getInputStream(), filePath);
            product.setImageUrl("/uploads/" + fileName);
        }
        return productRepository.save(product);
    }

    public Optional<Product> updateProduct(Long id, Product productDetails, MultipartFile image) throws IOException {
        return productRepository.findById(id)
                .map(existingProduct -> {
                    existingProduct.setName(productDetails.getName());
                    existingProduct.setDescription(productDetails.getDescription());
                    existingProduct.setPrice(productDetails.getPrice());

                    // ตรวจสอบว่ามีไฟล์ใหม่ถูกอัปโหลดหรือไม่
                    if (image != null && !image.isEmpty()) {
                        // ลบรูปภาพเก่าถ้ามี
                        if (existingProduct.getImageUrl() != null && !existingProduct.getImageUrl().isEmpty()) {
                            try {
                                Path oldImagePath = Paths.get(uploadDir, existingProduct.getImageUrl().replace("/uploads/", ""));
                                Files.deleteIfExists(oldImagePath);
                            } catch (IOException e) {
                                System.err.println("Error deleting old image: " + e.getMessage());
                            }
                        }
                        // บันทึกรูปภาพใหม่
                        String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                        Path filePath = Paths.get(uploadDir, fileName);
                        try {
                            Files.copy(image.getInputStream(), filePath);
                            existingProduct.setImageUrl("/uploads/" + fileName);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to store new image file!", e);
                        }
                    } else if (productDetails.getImageUrl() != null && productDetails.getImageUrl().isEmpty()) {
                        // ถ้าไม่มีไฟล์ใหม่ และ imageUrl จาก productDetails เป็นค่าว่าง (frontend ต้องการลบรูปเดิม)
                        if (existingProduct.getImageUrl() != null && !existingProduct.getImageUrl().isEmpty()) {
                            try {
                                Path oldImagePath = Paths.get(uploadDir, existingProduct.getImageUrl().replace("/uploads/", ""));
                                Files.deleteIfExists(oldImagePath);
                            } catch (IOException e) {
                                System.err.println("Error deleting old image on clear: " + e.getMessage());
                            }
                        }
                        existingProduct.setImageUrl(null); // ตั้งค่าเป็น null ใน database
                    }
                    
                    
                    return productRepository.save(existingProduct);
                });
    }

    public void deleteProduct(Long id) {
        productRepository.findById(id).ifPresent(product -> {
            // ลบรูปภาพที่เกี่ยวข้องด้วย
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                try {
                    Path imagePath = Paths.get(uploadDir, product.getImageUrl().replace("/uploads/", ""));
                    Files.deleteIfExists(imagePath);
                } catch (IOException e) {
                    System.err.println("Error deleting product image: " + e.getMessage());
                }
            }
            productRepository.delete(product);
        });
    }
}