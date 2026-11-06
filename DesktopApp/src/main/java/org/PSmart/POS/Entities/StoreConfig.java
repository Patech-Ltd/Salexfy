package org.PSmart.POS.Entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "StoreConfigTable")
public class StoreConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String storeName;

    private String storeAddress;

    private String storePhone;

    private String storeEmail;

    private String storeLogo;

    private String taxName;

    private Double taxRate;

    private Boolean useTurnoverTax;

    private Double turnoverTaxRate;

    private Double adjustingPct;

    private Boolean useTax;

    private Boolean taxInclusiveInPrice;

    private String currency;

    private String receiptFooter;

    private Boolean useBarcodeScanner;

    private Integer lowStockAlertLevel;

    private String defaultPrinter;

    /** Business tax PIN / VAT number printed under the store name. */
    private String taxPin;
    /** Optional line printed under the address (e.g. "Pay via Till 123456"). */
    private String receiptHeader;
    /** "58", "80" (thermal roll width in mm) or "A4". */
    private String receiptPaper;
    /** Print the receipt automatically after each sale (default on). */
    private Boolean autoPrintReceipt;

    /** Staff commission: enabled flag, basis ("SALES" or "PROFIT") and default rate in percent. */
    private Boolean commissionEnabled;
    private String commissionBasis;
    private Double commissionRate;

    @ElementCollection
    @CollectionTable(name = "PaymentMethodTable", joinColumns = @JoinColumn(name = "config_id"))
    @Column(name = "method_name")
    private List<String> paymentMethods;

    @ElementCollection
    @CollectionTable(name = "PaymentMethodDetailTable", joinColumns = @JoinColumn(name = "config_id"))
    @MapKeyColumn(name = "method_name")
    @Column(name = "detail")
    private Map<String, String> paymentMethodDetails;

    // ------------------------------------------------------------------
    // Backup & Sync settings
    // ------------------------------------------------------------------
    private Boolean autoBackupEnabled;

    /** Auto-backup interval in minutes (e.g. 60 = hourly). */
    private Integer autoBackupMinutes;

    /** How many latest backup files to keep locally (rotation). */
    private Integer backupKeepCount;

    private Long lastBackupAt;

    private Boolean syncEnabled;

    private String syncServerUrl;

    private String syncUsername;

    private String syncPassword;

    private Long lastSyncAt;

    private String lastSyncStatus;

    /** Loyalty programme (off unless enabled). */
    private Boolean loyaltyEnabled;
    /** Points earned per 1.00 of money paid. */
    private Double loyaltyPointsPerMoney;
    /** Money value of one point when redeemed. */
    private Double loyaltyPointValue;

    /** When true the till refuses to sell more than is on hand. */
    private Boolean blockOutOfStockSales;

    public static StoreConfig defaultConfig() {
        return StoreConfig.builder()
                .storeName("PSMART POS")
                .storeAddress("")
                .storePhone("")
                .storeEmail("")
                .taxName("VAT")
                .taxRate(16.0)
                .useTax(true)
                .useTurnoverTax(false)
                .turnoverTaxRate(0.0)
                .adjustingPct(0.0)
                .taxInclusiveInPrice(false)
                .currency("KES")
                .receiptFooter("Thank you for shopping with us!")
                .useBarcodeScanner(true)
                .lowStockAlertLevel(10)
                .defaultPrinter("")
                .paymentMethods(new ArrayList<>(List.of("Cash", "Card", "Mobile Money")))
                .paymentMethodDetails(new HashMap<>())
                .autoBackupEnabled(false)
                .autoBackupMinutes(60)
                .backupKeepCount(10)
                .syncEnabled(false)
                .syncServerUrl("")
                .syncUsername("")
                .syncPassword("")
                .lastSyncStatus("Not configured")
                .build();
    }

    public Double calculateTax(Double subtotal, Double discount) {
        if (useTax == null || !useTax || taxRate == null) return 0.0;
        double base = subtotal - (discount != null ? discount : 0);
        return base * taxRate / 100.0;
    }

    public Double calculateTotal(Double subtotal, Double discount) {
        double base = subtotal - (discount != null ? discount : 0);
        double tax = calculateTax(subtotal, discount);
        return base + tax;
    }
}
