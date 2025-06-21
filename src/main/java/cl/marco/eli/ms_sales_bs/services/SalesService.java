package cl.marco.eli.ms_sales_bs.services;


import cl.marco.eli.ms_sales_bs.clients.ProductClient;
import cl.marco.eli.ms_sales_bs.models.dto.*;
import cl.marco.eli.ms_sales_bs.models.entities.OrderItem;
import cl.marco.eli.ms_sales_bs.models.entities.SalesOrder;
import cl.marco.eli.ms_sales_bs.repositories.SalesOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesService {

    private static final Logger logger = LoggerFactory.getLogger(SalesService.class);

    @Autowired
    private SalesOrderRepository orderRepository;
    @Autowired
    private ProductClient productClient;
    @Autowired
    private FlowSignatureService signatureService;
    @Autowired
    private RestTemplate restTemplate;

    @Value("${flow.api.key}") private String flowApiKey;
    @Value("${flow.api.secret}") private String flowApiSecret;
    @Value("${flow.api.url}") private String flowApiUrl;

    @Transactional
    public OrderResponseDTO createOrder(CreateOrderRequestDTO request) {
        // 1. Validar stock y calcular total
        logger.info("PASO 1: Iniciando creación de orden para cliente ID {}", request.customerId());
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequestDTO itemDto : request.items()) {
            logger.info(" -> Verificando producto ID: {}", itemDto.productId());
            ProductDTO product = productClient.getProductById(itemDto.productId());
            if (product == null) {
                logger.error("ERROR: Producto no encontrado con ID: {}", itemDto.productId());
                throw new RuntimeException("Producto no encontrado con ID: " + itemDto.productId());
            }
            logger.info(" -> Producto ID {} encontrado. Stock disponible: {}", itemDto.productId(), product.stock());

            if (product.stock() < itemDto.quantity()) {
                logger.error("ERROR: Stock insuficiente para producto ID: {}", itemDto.productId());
                throw new RuntimeException("Stock insuficiente para el producto ID: " + itemDto.productId());
            }
            totalAmount = totalAmount.add(product.price().multiply(new BigDecimal(itemDto.quantity())));

            OrderItem item = new OrderItem();
            item.setProductId(product.id());
            item.setQuantity(itemDto.quantity());
            item.setUnitPrice(product.price());
            orderItems.add(item);
        }
        logger.info("PASO 2: Verificación de productos y cálculo de total (${}) completado.", totalAmount);

        // 2. Preparar los parámetros para la API de Flow
        String commerceOrder = "FERREMAS-" + System.currentTimeMillis();
        Map<String, String> flowParams = new HashMap<>();
        flowParams.put("apiKey", flowApiKey);
        flowParams.put("commerceOrder", commerceOrder);
        flowParams.put("subject", "Compra Ferremas");
        flowParams.put("amount", totalAmount.toPlainString());
        flowParams.put("email", request.customerEmail());
        flowParams.put("urlConfirmation", "https://ferremas.com/api/sales/flow/confirm");
        flowParams.put("urlReturn", "https://ferremas.com/order/result");

        try {
            // 3. Generar la firma
            logger.info("PASO 3: Generando firma digital para Flow.");
            String signature = signatureService.generateSignature(flowParams, flowApiSecret);
            logger.info(" -> Firma generada con éxito.");

            // 4. Preparar y ejecutar la llamada a Flow
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            flowParams.forEach(body::add);
            body.add("s", signature);

            HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(body, headers);
            String flowEndpoint = flowApiUrl + "/payment/create";
            logger.info("PASO 4: Llamando a la API de Flow en {}.", flowEndpoint);
            FlowPaymentResponseDTO flowResponse = restTemplate.postForObject(flowEndpoint, httpEntity, FlowPaymentResponseDTO.class);
            logger.info(" -> Respuesta de Flow recibida con éxito. Token: {}", flowResponse.token());

            if (flowResponse == null || flowResponse.url() == null) {
                logger.error("ERROR: La respuesta de Flow es inválida o no contiene URL.");
                throw new RuntimeException("Error al crear la orden de pago con Flow.");
            }

            // 5. Guardar la orden en nuestra BD
            SalesOrder order = new SalesOrder();
            order.setCustomerId(request.customerId());
            order.setTotalAmount(totalAmount);
            order.setCurrency("CLP");
            order.setPaymentStatus("PENDING");
            order.setWebpayTransactionId(flowResponse.token()); // Guardamos el token de Flow
            order.setItems(orderItems);
            for (OrderItem item : orderItems) {
                item.setSalesOrder(order);
            }
            logger.info("PASO 5: Guardando la orden en la base de datos.");
            SalesOrder savedOrder = orderRepository.save(order);
            logger.info(" -> Orden ID {} guardada con éxito.", savedOrder.getId());

            // 6. Preparar la respuesta para el BFF
            String redirectUrl = flowResponse.url() + "?token=" + flowResponse.token();
            logger.info("PASO 6: Proceso completado. Enviando URL de redirección.");
            return new OrderResponseDTO(savedOrder.getId(), "PENDIENTE DE PAGO", redirectUrl);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("ERROR CRÍTICO: No se pudo generar la firma para Flow.", e);
            throw new RuntimeException("Error al generar la firma para Flow", e);
        }catch (Exception e) {
            logger.error("ERROR INESPERADO durante la creación de la orden.", e);
            throw e; // Relanzamos la excepción para que Spring genere el 500
        }
    }

}
