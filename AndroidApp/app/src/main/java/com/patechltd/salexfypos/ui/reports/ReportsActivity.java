package com.patechltd.salexfypos.ui.reports;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.KeyValueAdapter;
import com.patechltd.salexfypos.db.CashierReportRow;
import com.patechltd.salexfypos.db.DayReportRow;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.TopProductRow;
import com.patechltd.salexfypos.db.entity.Expense;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.ExcelUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;
import com.patechltd.salexfypos.util.StorageUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReportsActivity extends AppCompatActivity {

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<View> rangeChips = new ArrayList<>();

    private TextView rangeLabel, totalSales, totalCount, totalProfit, totalCost, creditSales, paymentsReceived, purchasesTotal, itemsSold;
    private TextView taxTotal, expensesTotal, netProfit, turnoverTotal;
    private KeyValueAdapter dailyAdapter, cashierAdapter, topAdapter, paymentsAdapter, expensesAdapter;

    private long from = DateUtil.startOfDay(System.currentTimeMillis());
    private long to = DateUtil.endOfDay(System.currentTimeMillis());
    private boolean exportToday;
    private final List<Expense> currentExpenses = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        repo = Repository.get(this);
        exportToday = getIntent().getBooleanExtra("exportToday", false);

        ((com.google.android.material.appbar.MaterialToolbar) findViewById(R.id.toolbar))
                .setNavigationOnClickListener(v -> finish());

        bind();
        buildRangeChips();

        if (exportToday) {
            from = DateUtil.startOfDay(System.currentTimeMillis());
            to = DateUtil.endOfDay(System.currentTimeMillis());
            selectChip(0);
            load();
            exportNow();
        } else {
            selectChip(0);
            load();
        }
    }

    private void bind() {
        rangeLabel = findViewById(R.id.range_label);
        totalSales = findViewById(R.id.total_sales);
        totalCount = findViewById(R.id.total_count);
        totalProfit = findViewById(R.id.total_profit);
        totalCost = findViewById(R.id.total_cost);
        creditSales = findViewById(R.id.credit_sales);
        paymentsReceived = findViewById(R.id.payments_received);
        purchasesTotal = findViewById(R.id.purchases_total);
        itemsSold = findViewById(R.id.items_sold);
        taxTotal = findViewById(R.id.tax_total);
        expensesTotal = findViewById(R.id.expenses_total);
        netProfit = findViewById(R.id.net_profit);
        turnoverTotal = findViewById(R.id.turnover_total);

        dailyAdapter = new KeyValueAdapter();
        cashierAdapter = new KeyValueAdapter();
        topAdapter = new KeyValueAdapter();
        paymentsAdapter = new KeyValueAdapter();
        expensesAdapter = new KeyValueAdapter();

        RecyclerView dailyList = findViewById(R.id.daily_list);
        dailyList.setLayoutManager(new LinearLayoutManager(this));
        dailyList.setAdapter(dailyAdapter);

        RecyclerView cashierList = findViewById(R.id.cashier_list);
        cashierList.setLayoutManager(new LinearLayoutManager(this));
        cashierList.setAdapter(cashierAdapter);

        RecyclerView topList = findViewById(R.id.top_list);
        topList.setLayoutManager(new LinearLayoutManager(this));
        topList.setAdapter(topAdapter);

        RecyclerView paymentsList = findViewById(R.id.payments_list);
        paymentsList.setLayoutManager(new LinearLayoutManager(this));
        paymentsList.setAdapter(paymentsAdapter);

        RecyclerView expensesList = findViewById(R.id.expenses_list);
        expensesList.setLayoutManager(new LinearLayoutManager(this));
        expensesList.setAdapter(expensesAdapter);
        expensesAdapter.setListener(this::onExpenseRowClicked);

        findViewById(R.id.btn_export).setOnClickListener(v -> exportNow());
        findViewById(R.id.btn_add_expense).setOnClickListener(v -> showAddExpenseDialog());
        findViewById(R.id.btn_view_all_days).setOnClickListener(v -> {
            Intent i = new Intent(this, TotDaysActivity.class);
            i.putExtra(TotDaysActivity.EXTRA_FROM, from);
            i.putExtra(TotDaysActivity.EXTRA_TO, to);
            i.putExtra(TotDaysActivity.EXTRA_TITLE,
                    DateUtil.formatDate(from) + " — " + DateUtil.formatDate(to)
                            + " · TOT (1.5% of sales)");
            startActivity(i);
        });
    }

    private void buildRangeChips() {
        String[] names = {"Today", "Yesterday", "Last 7 Days", "Last 30 Days", "This Month", "Previous Month", "Custom"};
        android.widget.LinearLayout host = findViewById(R.id.range_chips);
        for (int i = 0; i < names.length; i++) {
            TextView chip = new TextView(this);
            chip.setText(names[i]);
            chip.setTextSize(13);
            chip.setTextColor(0xFF2F3E46);
            chip.setBackgroundResource(R.drawable.bg_chip);
            chip.setPadding(dp(14), dp(6), dp(14), dp(6));
            final int idx = i;
            chip.setOnClickListener(v -> {
                selectChip(idx);
                applyRange(idx);
                load();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            host.addView(chip, lp);
            rangeChips.add(chip);
        }
    }

    private void selectChip(int idx) {
        for (int i = 0; i < rangeChips.size(); i++) {
            rangeChips.get(i).setBackgroundResource(i == idx ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
            ((TextView) rangeChips.get(i)).setTextColor(i == idx ? 0xFFFFFFFF : 0xFF2F3E46);
        }
    }

    private void applyRange(int idx) {
        long now = System.currentTimeMillis();
        switch (idx) {
            case 0:
                from = DateUtil.startOfDay(now);
                to = DateUtil.endOfDay(now);
                rangeLabel.setText("Today");
                break;
            case 1:
                from = DateUtil.startOfDay(now - 86400000L);
                to = DateUtil.endOfDay(now - 86400000L);
                rangeLabel.setText("Yesterday");
                break;
            case 2:
                from = DateUtil.startOfDay(now - 6 * 86400000L);
                to = DateUtil.endOfDay(now);
                rangeLabel.setText("Last 7 days");
                break;
            case 3:
                from = DateUtil.startOfDay(now - 29 * 86400000L);
                to = DateUtil.endOfDay(now);
                rangeLabel.setText("Last 30 days");
                break;
            case 4:
                from = DateUtil.startOfMonth(now);
                to = DateUtil.endOfDay(now);
                rangeLabel.setText("This month");
                break;
            case 5:
                Calendar prev = Calendar.getInstance();
                prev.set(Calendar.DAY_OF_MONTH, 1);
                prev.add(Calendar.MONTH, -1);
                from = DateUtil.startOfDay(prev.getTimeInMillis());
                prev.set(Calendar.DAY_OF_MONTH, prev.getActualMaximum(Calendar.DAY_OF_MONTH));
                prev.set(Calendar.HOUR_OF_DAY, 23);
                prev.set(Calendar.MINUTE, 59);
                prev.set(Calendar.SECOND, 59);
                to = prev.getTimeInMillis();
                rangeLabel.setText("Previous month");
                break;
            case 6:
                pickCustomRange();
                break;
        }
    }

    private void pickCustomRange() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(this, (v, y, m, d) -> {
            Calendar start = Calendar.getInstance();
            start.clear();
            start.set(y, m, d);
            from = start.getTimeInMillis();
            DatePickerDialog dp2 = new DatePickerDialog(this, (v2, y2, m2, d2) -> {
                Calendar end = Calendar.getInstance();
                end.clear();
                end.set(y2, m2, d2);
                end.set(Calendar.HOUR_OF_DAY, 23);
                end.set(Calendar.MINUTE, 59);
                end.set(Calendar.SECOND, 59);
                to = end.getTimeInMillis();
                rangeLabel.setText(DateUtil.formatDate(from) + " — " + DateUtil.formatDate(to));
                load();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dp2.show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dp.show();
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    private void load() {
        String currency = Prefs.currency(this);
        repo.run(() -> {
            double sales = repo.sales.salesTotal(from, to);
            int count = repo.sales.saleCountBetween(from, to);
            double cost = repo.sales.costOfSalesBetween(from, to);
            double credit = repo.sales.creditSalesBetween(from, to);
            double payments = repo.suppliers.paymentsBetween(from, to);
            double purchases = repo.purchases.purchasesTotal(from, to);
            double tax = com.patechltd.salexfypos.util.TaxUtil.tot(repo.sales.subtotalTotal(from, to));
            double expenses = repo.expenses.totalBetween(from, to);
            List<Expense> expenseList =
                    repo.expenses.getBetween(from, to);
            currentExpenses.clear();
            currentExpenses.addAll(expenseList);
            List<DayReportRow> days = repo.sales.getDailyReport(from, to);
            List<CashierReportRow> cashiers = repo.sales.getCashierReport(from, to);
            List<TopProductRow> tops = repo.sales.getTopProducts(from, to, 10);
            List<com.patechltd.salexfypos.db.PaymentMethodTotalRow> pmts =
                    repo.sales.paymentTotalsBetween(from, to);
            handler.post(() -> {
                double profit = sales - cost;
                totalSales.setText(currency + " " + NumberUtil.money(sales));
                totalCount.setText(count + (count == 1 ? " sale" : " sales"));
                totalProfit.setText(currency + " " + NumberUtil.money(profit));
                totalCost.setText("Cost: " + currency + " " + NumberUtil.money(cost));
                creditSales.setText(currency + " " + NumberUtil.money(credit));
                paymentsReceived.setText(currency + " " + NumberUtil.money(payments));
                purchasesTotal.setText(currency + " " + NumberUtil.money(purchases));
                taxTotal.setText(currency + " " + NumberUtil.money(tax));
                expensesTotal.setText(currency + " " + NumberUtil.money(expenses));
                netProfit.setText(currency + " " + NumberUtil.money(profit - expenses));
                turnoverTotal.setText(currency + " " + NumberUtil.money(sales));
                int itemCount = 0;
                for (DayReportRow d : days) itemCount += d.itemCount;
                itemsSold.setText(String.valueOf(itemCount));

                List<KeyValueAdapter.Row> dailyRows = new ArrayList<>();
                for (DayReportRow d : days) {
                    dailyRows.add(new KeyValueAdapter.Row(
                            DateUtil.formatDate(DateUtil.startOfDay(d.dayStart)),
                            d.saleCount + " sales • " + d.itemCount + " items • Profit "
                                    + currency + " " + NumberUtil.money(d.profit),
                            currency + " " + NumberUtil.money(d.totalSales),
                            d.profit >= 0 ? 0xFF1565C0 : 0xFFB00020));
                }
                if (dailyRows.isEmpty()) dailyRows.add(new KeyValueAdapter.Row("No sales", "in this period", "", 0));
                dailyAdapter.submit(dailyRows);

                List<KeyValueAdapter.Row> cashierRows = new ArrayList<>();
                for (CashierReportRow c : cashiers) {
                    cashierRows.add(new KeyValueAdapter.Row(
                            c.cashierName,
                            c.saleCount + " sales",
                            currency + " " + NumberUtil.money(c.totalSales),
                            0xFF1565C0));
                }
                if (cashierRows.isEmpty()) cashierRows.add(new KeyValueAdapter.Row("No activity", "", "", 0));
                cashierAdapter.submit(cashierRows);

                List<KeyValueAdapter.Row> topRows = new ArrayList<>();
                for (TopProductRow t : tops) {
                    topRows.add(new KeyValueAdapter.Row(
                            t.name,
                            NumberUtil.qty(t.totalQty) + " sold",
                            currency + " " + NumberUtil.money(t.totalSales),
                            0xFF1565C0));
                }
                if (topRows.isEmpty()) topRows.add(new KeyValueAdapter.Row("No products sold", "", "", 0));
                topAdapter.submit(topRows);

                List<KeyValueAdapter.Row> paymentRows = new ArrayList<>();
                for (com.patechltd.salexfypos.db.PaymentMethodTotalRow pm : pmts) {
                    paymentRows.add(new KeyValueAdapter.Row(
                            com.patechltd.salexfypos.model.PaymentMethod.labelOf(pm.method),
                            "",
                            currency + " " + NumberUtil.money(pm.total),
                            0xFF1565C0));
                }
                if (paymentRows.isEmpty()) {
                    paymentRows.add(new KeyValueAdapter.Row("No payments", "in this period", "", 0));
                }
                paymentsAdapter.submit(paymentRows);

                List<KeyValueAdapter.Row> expenseRows = new ArrayList<>();
                for (com.patechltd.salexfypos.db.entity.Expense e : expenseList) {
                    String sub = DateUtil.formatDate(e.expenseDate);
                    if (e.category != null && !e.category.isEmpty()) sub += " • " + e.category;
                    expenseRows.add(new KeyValueAdapter.Row(
                            e.description == null || e.description.isEmpty() ? "Expense" : e.description,
                            sub,
                            currency + " " + NumberUtil.money(e.amount),
                            0xFFB00020));
                }
                if (expenseRows.isEmpty()) {
                    expenseRows.add(new KeyValueAdapter.Row("No expenses", "in this period", "", 0));
                }
                expensesAdapter.submit(expenseRows);
            });
        });
    }

    private void exportNow() {
        StorageUtil.createReportUri(this, DateUtil.formatDate(from) + "_" + DateUtil.formatDate(to));
    }

    private void showAddExpenseDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);
        TextInputEditText desc = view.findViewById(R.id.expense_desc);
        TextInputEditText amount = view.findViewById(R.id.expense_amount);
        TextInputEditText category = view.findViewById(R.id.expense_category);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Add expense")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String d = desc.getText() == null ? "" : desc.getText().toString().trim();
                    double amt = NumberUtil.parse(amount.getText() == null ? "" : amount.getText().toString(), 0);
                    if (d.isEmpty()) {
                        DialogUtil.toast(this, "Enter a description");
                        return;
                    }
                    if (amt <= 0) {
                        DialogUtil.toast(this, "Enter a valid amount");
                        return;
                    }
                    String cat = category.getText() == null ? "" : category.getText().toString().trim();
                    final String fDesc = d;
                    final String fCat = cat;
                    final double fAmt = amt;
                    repo.run(() -> {
                        repo.addExpense(fDesc, fCat, fAmt, System.currentTimeMillis());
                        handler.post(() -> {
                            DialogUtil.toast(this, "Expense added");
                            load();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onExpenseRowClicked(int position) {
        if (position < 0 || position >= currentExpenses.size()) return;
        Expense expense = currentExpenses.get(position);
        String[] actions = {"Edit", "Delete"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(expense.description == null || expense.description.isEmpty() ? "Expense" : expense.description)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) showEditExpenseDialog(expense);
                    else confirmDeleteExpense(expense);
                })
                .show();
    }

    private void showEditExpenseDialog(Expense expense) {
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_expense, null);
        TextInputEditText dateInput = view.findViewById(R.id.expense_date);
        TextInputEditText desc = view.findViewById(R.id.expense_desc);
        TextInputEditText amount = view.findViewById(R.id.expense_amount);
        TextInputEditText category = view.findViewById(R.id.expense_category);

        final long[] expenseDate = {expense.expenseDate};
        dateInput.setText(DateUtil.formatDate(expenseDate[0]));
        desc.setText(expense.description);
        amount.setText(NumberUtil.money(expense.amount));
        category.setText(expense.category == null ? "" : expense.category);
        dateInput.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(expenseDate[0]);
            new DatePickerDialog(this, (d, y, m, day) -> {
                Calendar c = Calendar.getInstance();
                c.clear();
                c.set(y, m, day);
                expenseDate[0] = c.getTimeInMillis();
                dateInput.setText(DateUtil.formatDate(expenseDate[0]));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit expense")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String d = desc.getText() == null ? "" : desc.getText().toString().trim();
                    double amt = NumberUtil.parse(amount.getText() == null ? "" : amount.getText().toString(), 0);
                    if (d.isEmpty()) {
                        DialogUtil.toast(this, "Enter a description");
                        return;
                    }
                    if (amt <= 0) {
                        DialogUtil.toast(this, "Enter a valid amount");
                        return;
                    }
                    String cat = category.getText() == null ? "" : category.getText().toString().trim();
                    final Expense copy = new Expense();
                    copy.uid = expense.uid;
                    copy.description = d;
                    copy.category = cat;
                    copy.amount = amt;
                    copy.expenseDate = expenseDate[0];
                    copy.createdBy = expense.createdBy;
                    copy.createdAt = expense.createdAt;
                    repo.run(() -> {
                        repo.updateExpense(copy);
                        handler.post(() -> {
                            DialogUtil.toast(this, "Expense updated");
                            load();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteExpense(Expense expense) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete expense")
                .setMessage("Delete \"" + (expense.description == null || expense.description.isEmpty()
                        ? "Expense" : expense.description) + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> repo.run(() -> {
                    repo.deleteExpense(expense.uid);
                    handler.post(() -> {
                        DialogUtil.toast(this, "Expense deleted");
                        load();
                    });
                }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == StorageUtil.REQ_CREATE_REPORT && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            repo.run(() -> {
                List<DayReportRow> days = repo.sales.getDailyReport(from, to);
                List<CashierReportRow> cashiers = repo.sales.getCashierReport(from, to);
                List<TopProductRow> tops = repo.sales.getTopProducts(from, to, 50);
                List<com.patechltd.salexfypos.db.PaymentMethodTotalRow> pmts =
                        repo.sales.paymentTotalsBetween(from, to);
                List<com.patechltd.salexfypos.db.entity.Expense> expenses = repo.expenses.getBetween(from, to);
                List<ExcelUtil.Section> sections = new ArrayList<>();
                sections.add(new ExcelUtil.Section("Daily", days,
                        new ExcelUtil.RowWriter() {
                            @Override public Object[] header() {
                                return new Object[]{"Date", "Sales", "Cost", "Profit", "Items", "Transactions"};
                            }
                            @Override public Object[] row(Object item, int index) {
                                DayReportRow d = (DayReportRow) item;
                                return new Object[]{DateUtil.formatDate(DateUtil.startOfDay(d.dayStart)), d.totalSales, d.totalCost, d.profit, d.itemCount, d.saleCount};
                            }
                        }));
                sections.add(new ExcelUtil.Section("Cashiers", cashiers,
                        new ExcelUtil.RowWriter() {
                            @Override public Object[] header() {
                                return new Object[]{"Cashier", "Sales", "Value"};
                            }
                            @Override public Object[] row(Object item, int index) {
                                CashierReportRow c = (CashierReportRow) item;
                                return new Object[]{c.cashierName, c.saleCount, c.totalSales};
                            }
                        }));
                sections.add(new ExcelUtil.Section("Payments", pmts,
                        new ExcelUtil.RowWriter() {
                            @Override public Object[] header() {
                                return new Object[]{"Method", "Amount"};
                            }
                            @Override public Object[] row(Object item, int index) {
                                com.patechltd.salexfypos.db.PaymentMethodTotalRow pm =
                                        (com.patechltd.salexfypos.db.PaymentMethodTotalRow) item;
                                return new Object[]{com.patechltd.salexfypos.model.PaymentMethod.labelOf(pm.method), pm.total};
                            }
                        }));
                sections.add(new ExcelUtil.Section("Top Products", tops,
                        new ExcelUtil.RowWriter() {
                            @Override public Object[] header() {
                                return new Object[]{"Product", "Qty Sold", "Value"};
                            }
                            @Override public Object[] row(Object item, int index) {
                                TopProductRow t = (TopProductRow) item;
                                return new Object[]{t.name, t.totalQty, t.totalSales};
                            }
                        }));
                sections.add(new ExcelUtil.Section("Expenses", expenses,
                        new ExcelUtil.RowWriter() {
                            @Override public Object[] header() {
                                return new Object[]{"Date", "Description", "Category", "Amount"};
                            }
                            @Override public Object[] row(Object item, int index) {
                                com.patechltd.salexfypos.db.entity.Expense e = (com.patechltd.salexfypos.db.entity.Expense) item;
                                return new Object[]{DateUtil.formatDate(e.expenseDate), e.description, e.category, e.amount};
                            }
                        }));
                boolean ok = ExcelUtil.exportMulti(this, uri, sections);
                handler.post(() -> DialogUtil.toast(this, ok ? "Report exported" : "Export failed"));
            });
        }
    }
}
