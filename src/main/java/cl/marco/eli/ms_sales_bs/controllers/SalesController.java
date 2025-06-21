package cl.marco.eli.ms_sales_bs.controllers;

import cl.marco.eli.ms_sales_bs.models.dto.CreateOrderRequestDTO;
import cl.marco.eli.ms_sales_bs.models.dto.OrderResponseDTO;
import cl.marco.eli.ms_sales_bs.services.SalesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales")
public class SalesController {

    @Autowired
    private SalesService salesService;

    @PostMapping("/orders")
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody CreateOrderRequestDTO request) {
        try {
            OrderResponseDTO response = salesService.createOrder(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // En un proyecto real, se manejarían excepciones específicas con estados HTTP adecuados
            return ResponseEntity.internalServerError().build();
        }
    }
}
