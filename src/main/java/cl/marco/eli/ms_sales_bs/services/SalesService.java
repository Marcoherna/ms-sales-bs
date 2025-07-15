package cl.marco.eli.ms_sales_bs.services;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import cl.marco.eli.ms_sales_bs.clients.ProductClient;
import cl.marco.eli.ms_sales_bs.clients.SalesDataClient;
import cl.marco.eli.ms_sales_bs.models.dto.CreateOrderRequestDTO;
import cl.marco.eli.ms_sales_bs.models.dto.FlowPaymentResponseDTO;
import cl.marco.eli.ms_sales_bs.models.dto.OrderItemDTO;
import cl.marco.eli.ms_sales_bs.models.dto.OrderItemRequestDTO;
import cl.marco.eli.ms_sales_bs.models.dto.OrderResponseDTO;
import cl.marco.eli.ms_sales_bs.models.dto.ProductDTO;
import cl.marco.eli.ms_sales_bs.models.dto.SalesOrderDTO;

@Service
public class SalesService {

    private static final Logger logger = LoggerFactory.getLogger(SalesService.class);

    
    @Autowired private SalesDataClient salesDataClient; 
    @Autowired private ProductClient productClient;
    @Autowired private FlowSignatureService signatureService;
    @Autowired private RestTemplate restTemplate;

    // --- Propiedades cargadas desde application.properties ---
    @Value("${flow.api.key}")
    private String flowApiKey;

    @Value("${flow.api.secret}")
    private String flowApiSecret;

    @Value("${flow.api.url}")
    private String flowApiUrl;

    
    @SuppressWarnings("UseSpecificCatch")
    public OrderResponseDTO createOrder(CreateOrderRequestDTO request) {
        logger.info("PASO 1: Iniciando creación de orden para cliente ID {}", request.customerId());

        try {
            // -----------------------------------------------------------------
            // ETAPA A: VERIFICACIÓN DE PRODUCTOS Y CÁLCULO DEL TOTAL
            // -----------------------------------------------------------------
            BigDecimal totalAmount = BigDecimal.ZERO;
            List<OrderItemDTO> orderItemsForPersistence = new ArrayList<>();

            for (OrderItemRequestDTO itemRequest : request.items()) {
                logger.info(" -> Verificando producto ID: {}", itemRequest.productId());
                ProductDTO product = productClient.getProductById(itemRequest.productId());

                if (product == null) {
                    throw new RuntimeException("Producto no encontrado con ID: " + itemRequest.productId());
                }
                logger.info(" -> Producto ID {} encontrado. Stock disponible: {}", itemRequest.productId(), product.stock());

                if (product.stock() < itemRequest.quantity()) {
                    throw new RuntimeException("Stock insuficiente para el producto ID: " + itemRequest.productId());
                }

                // Acumulamos el monto total
                totalAmount = totalAmount.add(product.price().multiply(new BigDecimal(itemRequest.quantity())));

                // Instanciamos el record de OrderItemDTO directamente con su constructor
                orderItemsForPersistence.add(
                    new OrderItemDTO(
                        itemRequest.productId(),
                        itemRequest.quantity(),
                        product.price()
                    )
                );
            }
            logger.info("PASO 2: Verificación de productos y cálculo de total (${}) completado.", totalAmount);

            // -----------------------------------------------------------------
            // ETAPA B: INTERACCIÓN CON LA PASARELA DE PAGO (FLOW)
            // -----------------------------------------------------------------
            String commerceOrder = "FERREMAS-" + System.currentTimeMillis();
            Map<String, String> flowParams = new HashMap<>();
            flowParams.put("apiKey", flowApiKey);
            flowParams.put("commerceOrder", commerceOrder);
            flowParams.put("subject", "Compra Ferremas");
            flowParams.put("amount", totalAmount.toPlainString());
            flowParams.put("email", request.customerEmail());
            flowParams.put("urlConfirmation", "https://ferremas.com/api/sales/flow/confirm");
            flowParams.put("urlReturn", "https://ferremas.com/order/result");

            logger.info("PASO 3: Generando firma digital para Flow.");
            String signature = signatureService.generateSignature(flowParams, flowApiSecret);
            logger.info(" -> Firma generada con éxito.");

            MultiValueMap<String, String> body = new  LinkedMultiValueMap<>();
            flowParams.forEach(body::add);
            body.add("s", signature);

            HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(body, new HttpHeaders() {{
                setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            }});

            String flowEndpoint = flowApiUrl + "/payment/create";
            logger.info("PASO 4: Llamando a la API de Flow en {}.", flowEndpoint);
            FlowPaymentResponseDTO flowResponse = restTemplate.postForObject(flowEndpoint, httpEntity, FlowPaymentResponseDTO.class);

            if (flowResponse == null || flowResponse.url() == null) {
                throw new RuntimeException("La respuesta de Flow es inválida.");
            }
            logger.info(" -> Respuesta de Flow recibida con éxito. Token: {}", flowResponse.token());
            logger.info("PASO 5: Preparando datos para enviar al servicio de persistencia.");
            
            // Instanciamos el SalesOrderDTO de una vez con todos sus datos, incluyendo la lista de items.
            SalesOrderDTO orderToSave = new SalesOrderDTO(
                request.customerId(),
                totalAmount,
                "CLP",
                "PENDING",
                flowResponse.token(),
                orderItemsForPersistence // Le pasamos la lista que ya creamos
            );

            // Llamamos al microservicio de datos para que guarde la orden
            salesDataClient.createOrder(orderToSave);
            logger.info(" -> Orden persistida con éxito en el servicio de datos.");

            // ETAPA D: CONSTRUCCIÓN DE LA RESPUESTA FINAL
            String redirectUrl = flowResponse.url() + "?token=" + flowResponse.token();
            
            // Instanciamos la respuesta final directamente con su constructor
            OrderResponseDTO response = new OrderResponseDTO(
                null, // The order ID is not returned by salesDataClient.createOrder, so it's set to null.
                "PENDIENTE DE PAGO",
                redirectUrl
            );

            logger.info("PASO 6: Proceso completado. Enviando URL de redirección.");
            return response;

        } catch (Exception e) {
            logger.error("ERROR INESPERADO durante la creación de la orden.", e);
            throw new RuntimeException("Error inesperado en el flujo de ventas.", e);
        }
    }
}