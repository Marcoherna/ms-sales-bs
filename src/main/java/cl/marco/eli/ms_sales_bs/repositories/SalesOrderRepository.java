package cl.marco.eli.ms_sales_bs.repositories;

import cl.marco.eli.ms_sales_bs.models.entities.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {}
