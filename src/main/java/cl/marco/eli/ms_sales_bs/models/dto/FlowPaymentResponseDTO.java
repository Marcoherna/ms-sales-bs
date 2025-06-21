package cl.marco.eli.ms_sales_bs.models.dto;

public record FlowPaymentResponseDTO(
        String url,
        String token,
        int flowOrder
) {
}
