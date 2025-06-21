package cl.marco.eli.ms_sales_bs.models.dto;

import java.math.BigDecimal;

public record ProductDTO(
        Long id,
        BigDecimal price,
        Integer stock,
        String name
) {
}
