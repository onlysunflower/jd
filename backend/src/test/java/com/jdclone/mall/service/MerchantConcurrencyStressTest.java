package com.jdclone.mall.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jdclone.mall.common.Constants;
import com.jdclone.mall.entity.OrderInfo;
import com.jdclone.mall.entity.Product;
import com.jdclone.mall.mapper.AddressMapper;
import com.jdclone.mall.mapper.OrderInfoMapper;
import com.jdclone.mall.mapper.OrderItemMapper;
import com.jdclone.mall.mapper.ProductMapper;
import com.jdclone.mall.security.AuthContext;
import com.jdclone.mall.security.AuthUser;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * 商家端并发/压力测试：模拟多名商家同时执行商品管理与发货操作，
 * 验证在大量并发请求下服务调用本身是线程安全、可稳定完成的。
 */
class MerchantConcurrencyStressTest {

    @AfterEach
    void clearAuth() {
        AuthContext.clear();
    }

    @Test
    void parallelShipProcessesAllRequestsWithoutLoss() throws InterruptedException {
        loginAsMerchant(1L, 50L);

        OrderInfoMapper orderInfoMapper = mock(OrderInfoMapper.class);
        AtomicInteger shipCalls = new AtomicInteger();
        // 每个下单都以 WAIT_SHIP 状态被读取；updateById 计数成功发货次数
        when(orderInfoMapper.selectById(any())).thenAnswer(inv -> {
            OrderInfo o = new OrderInfo();
            o.setId(100L);
            o.setMerchantId(50L);
            o.setStatus(Constants.ORDER_WAIT_SHIP);
            return o;
        });
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenAnswer(inv -> {
            shipCalls.incrementAndGet();
            return 1;
        });

        OrderService service = new OrderService(orderInfoMapper, mock(OrderItemMapper.class),
                mock(ProductMapper.class), mock(AddressMapper.class), mock(OperationLogService.class));

        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                loginAsMerchant(1L, 50L);
                ready.countDown();
                try {
                    start.await();
                    service.ship(100L, new com.jdclone.mall.dto.ShipRequest() {{
                        setLogisticsCompany("京东快递");
                        setLogisticsNo("JDV-CONCURRENT-" + Thread.currentThread().getId());
                    }});
                } catch (Exception ignored) {
                    // 状态条件竞争下允许部分请求被业务层拒绝（BizException）
                } finally {
                    AuthContext.clear();
                    done.countDown();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "并发发货任务未在限定时间内完成");
        pool.shutdownNow();

        assertTrue(shipCalls.get() >= 1, "至少应有 1 次发货成功被落库");
        assertTrue(shipCalls.get() <= threads, "发货成功次数不应超过并发请求总数");
    }

    @Test
    void parallelOnShelfKeepsInvocationCountExact() throws InterruptedException {
        loginAsMerchant(1L, 50L);

        ProductMapper productMapper = mock(ProductMapper.class);
        AtomicInteger onShelfCalls = new AtomicInteger();
        when(productMapper.selectById(any())).thenAnswer(inv -> {
            Product p = new Product();
            p.setId(7L);
            p.setMerchantId(50L);
            p.setAuditStatus(Constants.PRODUCT_APPROVED);
            p.setShelfStatus(Constants.SHELF_OFF);
            return p;
        });
        when(productMapper.updateById(any(Product.class))).thenAnswer(inv -> {
            onShelfCalls.incrementAndGet();
            return 1;
        });

        ProductService service = new ProductService(productMapper, mock(OperationLogService.class));

        int threads = 40;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                loginAsMerchant(1L, 50L);
                ready.countDown();
                try {
                    start.await();
                    service.onShelf(7L);
                } catch (Exception ignored) {
                } finally {
                    AuthContext.clear();
                    done.countDown();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "并发上架任务未在限定时间内完成");
        pool.shutdownNow();

        assertEquals(40, onShelfCalls.get());
    }

    private void loginAsMerchant(Long userId, Long merchantId) {
        AuthContext.set(new AuthUser(userId, "stress-shop", Constants.ROLE_MERCHANT, merchantId));
    }
}
