package cl.marco.eli.ms_sales_bs.models.dto;

public record OrderItemRequestDTO(
        Long productId,
        Integer quantity
) {
}
