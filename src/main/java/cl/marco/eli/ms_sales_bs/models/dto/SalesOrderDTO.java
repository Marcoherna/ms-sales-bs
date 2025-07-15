package cl.marco.eli.ms_sales_bs.models.dto;

import java.math.BigDecimal;
import java.util.List;

public record SalesOrderDTO(
        Long customerId,
        BigDecimal totalAmount,
        String currency,
        String paymentStatus,
        String webpayTransactionId,
        List<OrderItemDTO> items
) {
    
}
