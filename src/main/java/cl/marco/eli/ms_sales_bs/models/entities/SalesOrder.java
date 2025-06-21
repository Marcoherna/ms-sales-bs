package cl.marco.eli.ms_sales_bs.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sales_orders")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SalesOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long customerId;
    private BigDecimal totalAmount;
    private String currency;
    private String paymentStatus;
    private String webpayTransactionId; // Token de Flow
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;
}
