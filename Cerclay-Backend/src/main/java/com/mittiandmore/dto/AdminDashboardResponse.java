package com.mittiandmore.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AdminDashboardResponse {
    private Summary summary;
    private List<DailyPoint> dailySales;
    private List<TopProduct> topProducts;
    private Map<String, Long> orderStatuses;
    private Map<String, BigDecimal> paymentMethods;
    private ReviewSummary reviews;
    private ReturnSummary returns;

    public Summary getSummary(){return summary;} public void setSummary(Summary v){summary=v;}
    public List<DailyPoint> getDailySales(){return dailySales;} public void setDailySales(List<DailyPoint> v){dailySales=v;}
    public List<TopProduct> getTopProducts(){return topProducts;} public void setTopProducts(List<TopProduct> v){topProducts=v;}
    public Map<String,Long> getOrderStatuses(){return orderStatuses;} public void setOrderStatuses(Map<String,Long> v){orderStatuses=v;}
    public Map<String,BigDecimal> getPaymentMethods(){return paymentMethods;} public void setPaymentMethods(Map<String,BigDecimal> v){paymentMethods=v;}
    public ReviewSummary getReviews(){return reviews;} public void setReviews(ReviewSummary v){reviews=v;}
    public ReturnSummary getReturns(){return returns;} public void setReturns(ReturnSummary v){returns=v;}

    public static class Summary {
        private long totalOrders, periodOrders, totalCustomers, activeCustomers, totalProducts, activeProducts, lowStock, outOfStock;
        private BigDecimal grossSales=BigDecimal.ZERO, periodSales=BigDecimal.ZERO, refunded=BigDecimal.ZERO, pendingRefunds=BigDecimal.ZERO, netSales=BigDecimal.ZERO, averageOrderValue=BigDecimal.ZERO;
        public long getTotalOrders(){return totalOrders;} public void setTotalOrders(long v){totalOrders=v;}
        public long getPeriodOrders(){return periodOrders;} public void setPeriodOrders(long v){periodOrders=v;}
        public long getTotalCustomers(){return totalCustomers;} public void setTotalCustomers(long v){totalCustomers=v;}
        public long getActiveCustomers(){return activeCustomers;} public void setActiveCustomers(long v){activeCustomers=v;}
        public long getTotalProducts(){return totalProducts;} public void setTotalProducts(long v){totalProducts=v;}
        public long getActiveProducts(){return activeProducts;} public void setActiveProducts(long v){activeProducts=v;}
        public long getLowStock(){return lowStock;} public void setLowStock(long v){lowStock=v;}
        public long getOutOfStock(){return outOfStock;} public void setOutOfStock(long v){outOfStock=v;}
        public BigDecimal getGrossSales(){return grossSales;} public void setGrossSales(BigDecimal v){grossSales=v;}
        public BigDecimal getPeriodSales(){return periodSales;} public void setPeriodSales(BigDecimal v){periodSales=v;}
        public BigDecimal getRefunded(){return refunded;} public void setRefunded(BigDecimal v){refunded=v;}
        public BigDecimal getPendingRefunds(){return pendingRefunds;} public void setPendingRefunds(BigDecimal v){pendingRefunds=v;}
        public BigDecimal getNetSales(){return netSales;} public void setNetSales(BigDecimal v){netSales=v;}
        public BigDecimal getAverageOrderValue(){return averageOrderValue;} public void setAverageOrderValue(BigDecimal v){averageOrderValue=v;}
    }
    public static class DailyPoint { private String date; private long orders; private BigDecimal revenue=BigDecimal.ZERO; public String getDate(){return date;} public void setDate(String v){date=v;} public long getOrders(){return orders;} public void setOrders(long v){orders=v;} public BigDecimal getRevenue(){return revenue;} public void setRevenue(BigDecimal v){revenue=v;} }
    public static class TopProduct { private Long productId; private String name; private long quantity; private BigDecimal revenue=BigDecimal.ZERO; public Long getProductId(){return productId;} public void setProductId(Long v){productId=v;} public String getName(){return name;} public void setName(String v){name=v;} public long getQuantity(){return quantity;} public void setQuantity(long v){quantity=v;} public BigDecimal getRevenue(){return revenue;} public void setRevenue(BigDecimal v){revenue=v;} }
    public static class ReviewSummary { private long total,pending,approved,rejected; private BigDecimal averageRating=BigDecimal.ZERO; public long getTotal(){return total;} public void setTotal(long v){total=v;} public long getPending(){return pending;} public void setPending(long v){pending=v;} public long getApproved(){return approved;} public void setApproved(long v){approved=v;} public long getRejected(){return rejected;} public void setRejected(long v){rejected=v;} public BigDecimal getAverageRating(){return averageRating;} public void setAverageRating(BigDecimal v){averageRating=v;} }
    public static class ReturnSummary { private long total,pending,approved,rejected,closed; private BigDecimal refunded=BigDecimal.ZERO,pendingRefunds=BigDecimal.ZERO; public long getTotal(){return total;} public void setTotal(long v){total=v;} public long getPending(){return pending;} public void setPending(long v){pending=v;} public long getApproved(){return approved;} public void setApproved(long v){approved=v;} public long getRejected(){return rejected;} public void setRejected(long v){rejected=v;} public long getClosed(){return closed;} public void setClosed(long v){closed=v;} public BigDecimal getRefunded(){return refunded;} public void setRefunded(BigDecimal v){refunded=v;} public BigDecimal getPendingRefunds(){return pendingRefunds;} public void setPendingRefunds(BigDecimal v){pendingRefunds=v;} }
}
