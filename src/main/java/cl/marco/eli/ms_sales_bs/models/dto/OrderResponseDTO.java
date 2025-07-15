package cl.marco.eli.ms_sales_bs.models.dto;

public record OrderResponseDTO(
        Long orderId, 
        String status, 
        String redirectUrl
) {
}
