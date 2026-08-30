package com.jdclone.mall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jdclone.mall.common.GlobalExceptionHandler;
import com.jdclone.mall.entity.Category;
import com.jdclone.mall.entity.Product;
import com.jdclone.mall.mapper.CategoryMapper;
import com.jdclone.mall.service.OrderService;
import com.jdclone.mall.service.ProductService;
import com.jdclone.mall.service.RefundService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MerchantControllerIntegrationTest {
    private MockMvc mockMvc;

    private ProductService productService;
    private OrderService orderService;
    private RefundService refundService;
    private CategoryMapper categoryMapper;

    @BeforeEach
    void setUp() {
        productService = org.mockito.Mockito.mock(ProductService.class);
        orderService = org.mockito.Mockito.mock(OrderService.class);
        refundService = org.mockito.Mockito.mock(RefundService.class);
        categoryMapper = org.mockito.Mockito.mock(CategoryMapper.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new MerchantProductController(productService, categoryMapper),
                        new MerchantOrderController(orderService),
                        new MerchantRefundController(refundService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean())
                .build();
    }

    @Test
    void categoriesEndpointReturnsCategoryList() throws Exception {
        Category c = new Category();
        c.setId(1L);
        c.setName("手机数码");
        when(categoryMapper.selectList(any())).thenReturn(List.of(c));

        mockMvc.perform(get("/api/merchant/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("手机数码"));
    }

    @Test
    void createProductReturnsOkWithData() throws Exception {
        Product p = new Product();
        p.setId(1L);
        p.setName("测试手机");
        p.setAuditStatus("PENDING");
        when(productService.create(any())).thenReturn(p);

        mockMvc.perform(post("/api/merchant/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试手机\",\"price\":99.00,\"stock\":10,\"categoryId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("测试手机"));
    }

    @Test
    void createProductRejectsNameBlankViaValidation() throws Exception {
        mockMvc.perform(post("/api/merchant/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":99.00,\"stock\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(422));
    }

    @Test
    void onShelfProductForwardsToService() throws Exception {
        Product p = new Product();
        p.setId(7L);
        when(productService.onShelf(eq(7L))).thenReturn(p);

        mockMvc.perform(post("/api/merchant/products/7/on-shelf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(productService).onShelf(7L);
    }

    @Test
    void offShelfProductForwardsToService() throws Exception {
        Product p = new Product();
        p.setId(8L);
        when(productService.offShelf(eq(8L))).thenReturn(p);

        mockMvc.perform(post("/api/merchant/products/8/off-shelf"))
                .andExpect(status().isOk());
        verify(productService).offShelf(8L);
    }

    @Test
    void shipOrderForwardsPayloadToService() throws Exception {
        mockMvc.perform(post("/api/merchant/orders/9/ship")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logisticsCompany\":\"京东快递\",\"logisticsNo\":\"JDV123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(orderService).ship(eq(9L), any());
    }

    @Test
    void shipOrderRejectsBlankLogisticsViaValidation() throws Exception {
        mockMvc.perform(post("/api/merchant/orders/9/ship")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logisticsCompany\":\"\",\"logisticsNo\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(422));
    }

    @Test
    void approveRefundRejectsMissingRemarkViaValidation() throws Exception {
        mockMvc.perform(post("/api/merchant/refunds/3/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(422));
    }

    @Test
    void rejectRefundForwardsRemarkToService() throws Exception {
        mockMvc.perform(post("/api/merchant/refunds/3/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remark\":\"已发货不支持退货\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(refundService).reject(eq(3L), eq("已发货不支持退货"));
    }

    @Test
    void confirmReturnForwardsRemarkToService() throws Exception {
        when(refundService.confirmReturn(eq(5L), eq("收到退货"))).thenReturn(null);
        mockMvc.perform(post("/api/merchant/refunds/5/confirm-return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remark\":\"收到退货\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(refundService).confirmReturn(eq(5L), eq("收到退货"));
    }
}
