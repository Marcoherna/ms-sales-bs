package cl.marco.eli.ms_sales_bs.clients;

import cl.marco.eli.ms_sales_bs.models.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "${ferremas.bs-products.url}")
public interface ProductClient {
    @GetMapping("/api/products/{id}")
    ProductDTO getProductById(@PathVariable("id") Long id);
}
