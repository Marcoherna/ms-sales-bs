package cl.marco.eli.ms_sales_bs.models.dto;

import java.util.List;

public record CreateOrderRequestDTO(
        Long customerId,
        String customerEmail,
        List<OrderItemRequestDTO> items
        ) {

}
