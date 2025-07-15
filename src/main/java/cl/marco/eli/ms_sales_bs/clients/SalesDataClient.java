package cl.marco.eli.ms_sales_bs.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import cl.marco.eli.ms_sales_bs.models.dto.SalesOrderDTO;

@FeignClient(name = "sales-data-service", url = "${ferremas.ms-sales-data.url}")
public interface SalesDataClient {

    @PostMapping("/data/orders")
    SalesOrderDTO createOrder(@RequestBody SalesOrderDTO order);
}