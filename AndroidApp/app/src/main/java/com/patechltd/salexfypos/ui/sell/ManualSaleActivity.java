package com.patechltd.salexfypos.ui.sell;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Category;
import com.patechltd.salexfypos.db.entity.Customer;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.db.entity.SaleItem;
import com.patechltd.salexfypos.db.entity.SalePayment;
import com.patechltd.salexfypos.db.entity.User;
import com.patechltd.salexfypos.model.PaymentMethod;
import com.patechltd.salexfypos.security.Session;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class ManualSaleActivity extends AppCompatActivity {

    private Repository repo;
    private final List<Line> lines = new ArrayList<>();
    private ManualSaleAdapter adapter;

    private long saleDate = System.currentTimeMillis();
    private String customerId = null;
    private String customerName = null;
    private PaymentMethod method = PaymentMethod.CASH;
    private String currency;

    private TextView dateInput;
    private TextView customerLabel;
    private TextView methodLabel;
    private TextView balanceLabel;
    private TextView subtotalLabel;
    private TextView taxLabel;
    private TextView totalLabel;
    private EditText paidInput;

    static class Line {
        Product product;
        double qty = 1;
        double price = 0;
        String unitLabel = "Pcs";
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_sale);
        repo = Repository.get(this);
        currency = Prefs.currency(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        dateInput = findViewById(R.id.date_input);
        customerLabel = findViewById(R.id.customer_label);
        methodLabel = findViewById(R.id.method_label);
        balanceLabel = findViewById(R.id.balance_label);
        subtotalLabel = findViewById(R.id.subtotal_label);
        taxLabel = findViewById(R.id.tax_label);
        totalLabel = findViewById(R.id.total_label);
        paidInput = findViewById(R.id.paid_input);

        dateInput.setText("Date: " + DateUtil.format(saleDate));
        dateInput.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(saleDate);
            DatePickerDialog dialog = new DatePickerDialog(this,
                    (view, year, month, day) -> {
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.YEAR, year);
                        c.set(Calendar.MONTH, month);
                        c.set(Calendar.DAY_OF_MONTH, day);
                        c.set(Calendar.HOUR_OF_DAY, 12);
                        c.set(Calendar.MINUTE, 0);
                        c.set(Calendar.SECOND, 0);
                        c.set(Calendar.MILLISECOND, 0);
                        saleDate = c.getTimeInMillis();
                        dateInput.setText("Date: " + DateUtil.format(saleDate));
                    },
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        customerLabel.setOnClickListener(v -> pickCustomer());
        methodLabel.setOnClickListener(v -> pickMethod());

        RecyclerView itemList = findViewById(R.id.item_list);
        itemList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ManualSaleAdapter(lines, this::recalc, position -> {
            lines.remove(position);
            adapter.notifyDataSetChanged();
            recalc();
        });
        itemList.setAdapter(adapter);

        findViewById(R.id.btn_add_item).setOnClickListener(v -> pickProduct());
        paidInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                recalc();
            }
        });
        findViewById(R.id.btn_save).setOnClickListener(v -> save());

        recalc();
    }

    private void pickProduct() {
        repo.thenOnMain(repo.io(() -> {
            Object[] result = new Object[2];
            result[0] = repo.products.getAllActive();
            result[1] = repo.directory.getCategories();
            return result;
        }), result -> {
            @SuppressWarnings("unchecked")
            List<Product> products = (List<Product>) result[0];
            @SuppressWarnings("unchecked")
            List<Category> categories = (List<Category>) result[1];
            new ProductSearchDialog(this, products, categories, false, this::addLine).show();
        });
    }

    private void addLine(Product product) {
        for (Line l : lines) {
            if (l.product.uid.equals(product.uid)) {
                l.qty += 1;
                adapter.notifyDataSetChanged();
                recalc();
                return;
            }
        }
        Line line = new Line();
        line.product = product;
        line.qty = 1;
        line.price = product.retailPrice > 0 ? product.retailPrice : product.wholesalePrice;
        line.unitLabel = product.retailUnit == null || product.retailUnit.isEmpty() ? "Pcs" : product.retailUnit;
        lines.add(line);
        adapter.notifyDataSetChanged();
        recalc();
    }

    private void pickCustomer() {
        repo.thenOnMain(repo.io((java.util.concurrent.Callable<Object>) () -> repo.suppliers.getCustomers()),
                result -> {
                    @SuppressWarnings("unchecked")
                    List<Customer> customers = (List<Customer>) result;
                    String[] names = new String[customers.size() + 1];
                    names[0] = "+ New customer";
                    for (int i = 0; i < customers.size(); i++) names[i + 1] = customers.get(i).name;
                    DialogUtil.pick(this, "Customer", names, 0, which -> {
                        if (which == 0) {
                            DialogUtil.inputText(this, "New customer", "Customer name", "", "Add", value -> {
                                if (value.trim().isEmpty()) return;
                                Customer customer = new Customer();
                                customer.uid = UUID.randomUUID().toString();
                                customer.name = value.trim();
                                customer.createdAt = System.currentTimeMillis();
                                repo.run(() -> repo.suppliers.insertCustomer(customer));
                                setCustomer(customer.uid, customer.name);
                            });
                        } else {
                            Customer customer = customers.get(which - 1);
                            setCustomer(customer.uid, customer.name);
                        }
                    });
                });
    }

    private void setCustomer(String id, String name) {
        customerId = id;
        customerName = name;
        customerLabel.setText("Customer: " + name);
        customerLabel.setTextColor(getColor(R.color.text_primary));
        recalc();
    }

    private void pickMethod() {
        PaymentMethod[] methods = PaymentMethod.values();
        String[] labels = new String[methods.length];
        for (int i = 0; i < methods.length; i++) labels[i] = methods[i].getLabel();
        DialogUtil.pick(this, "Payment method", labels, method.ordinal(), which -> {
            method = methods[which];
            methodLabel.setText("Method: " + method.getLabel());
            recalc();
        });
    }

    private void recalc() {
        double subtotal = 0;
        double tax = 0;
        for (Line l : lines) {
            double lineTotal = l.qty * l.price;
            subtotal += lineTotal;
            tax += lineTotal * l.product.taxPercent / 100.0;
        }
        double total = subtotal + tax;
        double paid;
        if (method == PaymentMethod.CREDIT) {
            paid = 0;
        } else {
            paid = NumberUtil.parse(paidInput.getText() == null ? "" : paidInput.getText().toString(), total);
            if (paid < 0) paid = 0;
            if (paid > total) paid = total;
        }
        double balance = NumberUtil.round2(total - paid);

        subtotalLabel.setText(currency + " " + NumberUtil.money(subtotal));
        taxLabel.setText(currency + " " + NumberUtil.money(tax));
        totalLabel.setText(currency + " " + NumberUtil.money(total));
        if (balance > 0.01) {
            balanceLabel.setText("Balance: " + currency + " " + NumberUtil.money(balance)
                    + (customerId == null ? "  (add a customer)" : ""));
            balanceLabel.setVisibility(View.VISIBLE);
        } else {
            balanceLabel.setVisibility(View.GONE);
        }
    }

    private void save() {
        if (lines.isEmpty()) {
            DialogUtil.toast(this, "Add at least one product");
            return;
        }
        String notes = ((EditText) findViewById(R.id.notes_input)).getText() == null
                ? "" : ((EditText) findViewById(R.id.notes_input)).getText().toString().trim();

        double subtotal = 0;
        double tax = 0;
        for (Line l : lines) {
            double lineTotal = l.qty * l.price;
            subtotal += lineTotal;
            tax += lineTotal * l.product.taxPercent / 100.0;
        }
        final double total = subtotal + tax;
        double paid;
        if (method == PaymentMethod.CREDIT) {
            paid = 0;
        } else {
            paid = NumberUtil.parse(paidInput.getText() == null ? "" : paidInput.getText().toString(), total);
            if (paid < 0) paid = 0;
            if (paid > total) paid = total;
        }
        final double paidFinal = paid;
        final double balance = NumberUtil.round2(total - paid);
        final double subtotalFinal = NumberUtil.round2(subtotal);
        final double taxFinal = NumberUtil.round2(tax);
        if (balance > 0.01 && customerId == null) {
            DialogUtil.toast(this, "Select a customer to record the unpaid balance");
            return;
        }

        final String cashierId = Session.userId(this);
        repo.thenOnMain(repo.io(() -> {
            Sale sale = new Sale();
            sale.uid = UUID.randomUUID().toString();
            sale.saleNo = repo.nextSaleNo();
            sale.saleDate = saleDate;
            User user = cashierId != null ? repo.admin.getUser(cashierId) : null;
            sale.cashierId = cashierId;
            sale.cashierName = user != null ? user.fullName : "";
            sale.customerId = customerId;
            sale.customerName = customerName;
            sale.subtotal = subtotalFinal;
            sale.discount = 0;
            sale.taxAmount = taxFinal;
            sale.total = NumberUtil.round2(total);
            sale.paidAmount = NumberUtil.round2(paidFinal);
            sale.changeAmount = 0;
            sale.paymentMethod = method.name();
            sale.status = "COMPLETE";
            sale.notes = notes;
            sale.createdBy = cashierId;
            sale.createdAt = System.currentTimeMillis();

            List<SaleItem> items = new ArrayList<>();
            for (Line l : lines) {
                SaleItem item = new SaleItem();
                item.uid = UUID.randomUUID().toString();
                item.saleId = sale.uid;
                item.productId = l.product.uid;
                item.productName = l.product.name;
                item.barcode = l.product.barcode;
                item.qty = l.qty;
                item.stockQty = l.qty;
                item.unitLabel = l.unitLabel;
                item.isWholesale = false;
                item.unitPrice = NumberUtil.round2(l.price);
                item.costPrice = l.product.costPrice;
                item.lineTotal = NumberUtil.round2(l.qty * l.price);
                items.add(item);
            }

            List<SalePayment> payments = new ArrayList<>();
            if (method == PaymentMethod.CREDIT) {
                SalePayment credit = new SalePayment();
                credit.uid = UUID.randomUUID().toString();
                credit.method = PaymentMethod.CREDIT.name();
                credit.amount = NumberUtil.round2(total);
                credit.customerId = customerId;
                credit.customerName = customerName;
                payments.add(credit);
            } else {
                SalePayment main = new SalePayment();
                main.uid = UUID.randomUUID().toString();
                main.method = method.name();
                main.amount = NumberUtil.round2(paidFinal);
                main.customerId = customerId;
                main.customerName = customerName;
                payments.add(main);
                if (balance > 0.01) {
                    SalePayment credit = new SalePayment();
                    credit.uid = UUID.randomUUID().toString();
                    credit.method = PaymentMethod.CREDIT.name();
                    credit.amount = balance;
                    credit.customerId = customerId;
                    credit.customerName = customerName;
                    payments.add(credit);
                }
            }

            repo.completeSale(sale, items, payments);
            return sale.saleNo;
        }), saleNo -> {
            DialogUtil.toast(this, "Sale #" + saleNo + " saved");
            setResult(RESULT_OK);
            finish();
        });
    }

    interface OnRemove {
        void onRemove(int position);
    }

    private class ManualSaleAdapter extends RecyclerView.Adapter<ManualSaleAdapter.Holder> {

        private final List<Line> data;
        private final Runnable onChanged;
        private final OnRemove onRemove;

        ManualSaleAdapter(List<Line> data, Runnable onChanged, OnRemove onRemove) {
            this.data = data;
            this.onChanged = onChanged;
            this.onRemove = onRemove;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_manual_sale_line, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Line line = data.get(position);
            holder.name.setText(line.product.name);
            holder.qty.setText(NumberUtil.qty(line.qty));
            holder.price.setText(line.price == 0 ? "" : String.valueOf(line.price));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView name;
            final EditText qty;
            final EditText price;
            final ImageView remove;

            Holder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.line_name);
                qty = itemView.findViewById(R.id.line_qty);
                price = itemView.findViewById(R.id.line_price);
                remove = itemView.findViewById(R.id.line_remove);
                qty.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        int pos = getBindingAdapterPosition();
                        if (pos < 0 || pos >= data.size()) return;
                        data.get(pos).qty = NumberUtil.parse(s == null ? "" : s.toString(), 1);
                        onChanged.run();
                    }
                });
                price.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        int pos = getBindingAdapterPosition();
                        if (pos < 0 || pos >= data.size()) return;
                        data.get(pos).price = NumberUtil.parse(s == null ? "" : s.toString(), 0);
                        onChanged.run();
                    }
                });
                remove.setOnClickListener(v -> onRemove.onRemove(getBindingAdapterPosition()));
            }
        }
    }
}
