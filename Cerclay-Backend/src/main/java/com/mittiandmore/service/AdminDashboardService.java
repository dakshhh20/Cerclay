package com.mittiandmore.service;

import com.mittiandmore.dto.AdminDashboardResponse;
import com.mittiandmore.entity.*;
import com.mittiandmore.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final ReturnRequestRepository returns;
    private final RefundRepository refunds;
    private final ProductReviewRepository reviews;

    public AdminDashboardService(
        OrderRepository orders,
        OrderItemRepository orderItems,
        CustomerRepository customers,
        ProductRepository products,
        ReturnRequestRepository returns,
        RefundRepository refunds,
        ProductReviewRepository reviews
    ) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.customers = customers;
        this.products = products;
        this.returns = returns;
        this.refunds = refunds;
        this.reviews = reviews;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard(int days) {
        int safeDays = Math.max(7, Math.min(days, 90));
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(safeDays - 1L);
        LocalDateTime startAt = start.atStartOfDay();
        List<Order> allOrders = orders.findAll();
        List<Customer> allCustomers = customers.findAll();
        List<Product> allProducts = products.findAll();
        List<Refund> allRefunds = refunds.findAll();
        List<ReturnRequest> allReturns = returns.findAll();
        List<ProductReview> allReviews = reviews.findAll();

        List<Order> revenueOrders = allOrders.stream().filter(this::countsAsSale).toList();
        List<Order> periodOrders = revenueOrders
            .stream()
            .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(startAt))
            .toList();
        BigDecimal gross = revenueOrders
            .stream()
            .map(Order::getTotal)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal periodGross = periodOrders
            .stream()
            .map(Order::getTotal)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal processedRefunds = sumRefunds(allRefunds, "PROCESSED");
        BigDecimal pendingRefunds = sumRefunds(allRefunds, "PENDING");

        AdminDashboardResponse r = new AdminDashboardResponse();
        AdminDashboardResponse.Summary s = new AdminDashboardResponse.Summary();
        s.setTotalOrders(allOrders.size());
        s.setPeriodOrders(periodOrders.size());
        s.setTotalCustomers(allCustomers.size());
        s.setActiveCustomers(
            allCustomers
                .stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .count()
        );
        s.setTotalProducts(allProducts.size());
        s.setActiveProducts(
            allProducts
                .stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .count()
        );
        s.setLowStock(
            allProducts
                .stream()
                .filter(p -> p.getStock() != null && p.getStock() > 0 && p.getStock() <= 5)
                .count()
        );
        s.setOutOfStock(
            allProducts
                .stream()
                .filter(p -> p.getStock() == null || p.getStock() <= 0)
                .count()
        );
        s.setGrossSales(gross);
        s.setPeriodSales(periodGross);
        s.setRefunded(processedRefunds);
        s.setPendingRefunds(pendingRefunds);
        s.setNetSales(gross.subtract(processedRefunds).max(BigDecimal.ZERO));
        s.setAverageOrderValue(
            revenueOrders.isEmpty()
                ? BigDecimal.ZERO
                : gross.divide(BigDecimal.valueOf(revenueOrders.size()), 2, RoundingMode.HALF_UP)
        );
        r.setSummary(s);

        Map<LocalDate, AdminDashboardResponse.DailyPoint> daily = new LinkedHashMap<>();
        for (int i = 0; i < safeDays; i++) {
            LocalDate d = start.plusDays(i);
            var p = new AdminDashboardResponse.DailyPoint();
            p.setDate(d.toString());
            daily.put(d, p);
        }
        for (Order o : periodOrders) {
            LocalDate d = o.getCreatedAt().toLocalDate();
            var p = daily.get(d);
            if (p != null) {
                p.setOrders(p.getOrders() + 1);
                p.setRevenue(p.getRevenue().add(nvl(o.getTotal())));
            }
        }
        r.setDailySales(new ArrayList<>(daily.values()));

        Map<String, Long> statuses = allOrders
            .stream()
            .collect(Collectors.groupingBy(o -> normal(o.getOrderStatus()), TreeMap::new, Collectors.counting()));
        r.setOrderStatuses(statuses);
        Map<String, BigDecimal> payments = new TreeMap<>();
        for (Order o : revenueOrders) {
            String method = normal(o.getPaymentMethod());
            payments.merge(method, nvl(o.getTotal()), BigDecimal::add);
        }
        r.setPaymentMethods(payments);

        Map<Long, AdminDashboardResponse.TopProduct> top = new HashMap<>();
        for (OrderItem item : orderItems.findAll()) {
            if (item.getOrder() == null || !countsAsSale(item.getOrder())) continue;
            Long id = item.getProduct() != null ? item.getProduct().getId() : null;
            if (id == null) continue;
            var p = top.computeIfAbsent(id, k -> {
                var x = new AdminDashboardResponse.TopProduct();
                x.setProductId(k);
                x.setName(item.getProductName());
                return x;
            });
            p.setQuantity(p.getQuantity() + Optional.ofNullable(item.getQuantity()).orElse(0));
            p.setRevenue(p.getRevenue().add(nvl(item.getTotal())));
        }
        List<AdminDashboardResponse.TopProduct> topList = top
            .values()
            .stream()
            .sorted(Comparator.comparing(AdminDashboardResponse.TopProduct::getRevenue).reversed())
            .limit(5)
            .toList();
        r.setTopProducts(topList);

        var rs = new AdminDashboardResponse.ReviewSummary();
        rs.setTotal(allReviews.size());
        rs.setPending(countStatus(allReviews, "PENDING"));
        rs.setApproved(countStatus(allReviews, "APPROVED"));
        rs.setRejected(countStatus(allReviews, "REJECTED"));
        double avg = allReviews
            .stream()
            .filter(x -> "APPROVED".equalsIgnoreCase(x.getStatus()) && x.getRating() != null)
            .mapToInt(ProductReview::getRating)
            .average()
            .orElse(0);
        rs.setAverageRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
        r.setReviews(rs);

        var rr = new AdminDashboardResponse.ReturnSummary();
        rr.setTotal(allReturns.size());
        rr.setPending(countReturnPending(allReturns));
        rr.setApproved(countStatusReturn(allReturns, "APPROVED"));
        rr.setRejected(countStatusReturn(allReturns, "REJECTED"));
        rr.setClosed(countStatusReturn(allReturns, "CLOSED"));
        rr.setRefunded(processedRefunds);
        rr.setPendingRefunds(pendingRefunds);
        r.setReturns(rr);
        return r;
    }

    private boolean countsAsSale(Order o) {
        String status = normal(o.getOrderStatus());
        String method = normal(o.getPaymentMethod());
        String payment = normal(o.getPaymentStatus());
        boolean validPayment = "COD".equals(method) || "PAID".equals(payment);
        return !status.contains("CANCEL") && validPayment && nvl(o.getTotal()).compareTo(BigDecimal.ZERO) > 0;
    }

    private long countStatus(List<ProductReview> list, String status) {
        return list
            .stream()
            .filter(x -> status.equalsIgnoreCase(normal(x.getStatus())))
            .count();
    }

    private long countStatusReturn(List<ReturnRequest> list, String status) {
        return list
            .stream()
            .filter(x -> status.equalsIgnoreCase(normal(x.getStatus())))
            .count();
    }

    private long countReturnPending(List<ReturnRequest> list) {
        Set<String> done = Set.of("REJECTED", "CLOSED", "COMPLETED", "REFUNDED");
        return list
            .stream()
            .filter(x -> !done.contains(normal(x.getStatus())))
            .count();
    }

    private BigDecimal sumRefunds(List<Refund> list, String status) {
        return list
            .stream()
            .filter(x -> status.equalsIgnoreCase(normal(x.getStatus())))
            .map(Refund::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String normal(String v) {
        return v == null || v.isBlank() ? "UNKNOWN" : v.toUpperCase(Locale.ROOT);
    }
}
