package cl.marco.eli.ms_sales_bs.models.dto;

import java.math.BigDecimal;

public record OrderItemDTO(
        Long productId,
        Integer quantity,
        BigDecimal unitPrice
) {
    
}
