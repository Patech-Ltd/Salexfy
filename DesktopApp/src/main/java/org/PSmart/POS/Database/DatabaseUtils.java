package org.PSmart.POS.Database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.PSmart.POS.Repositories.*;

public class DatabaseUtils {

    private EntityManagerFactory emf = Persistence.createEntityManagerFactory("PSmartPosPU");

    private static DatabaseUtils instance;

    public static DatabaseUtils getInstance() {
        if(instance==null)
            instance = new DatabaseUtils();
        return instance;
    }

    public EntityManagerFactory getEmf() {
        return emf;
    }

    private EntityManager entityManager;


    public EntityManager createEntityManager()
    {
        entityManager = emf.createEntityManager();
        return entityManager;
    }

    private UsersRepository usersRepository;

    private RoleRepository roleRepository;

    private SaleRepository saleRepository;

    private CustomerRepository customerRepository;

    private StoreConfigRepository storeConfigRepository;

    private PurchaseRepository purchaseRepository;

    private ExpenseRepository expenseRepository;

    private CreditRepository creditRepository;

    private HeldOrderRepository heldOrderRepository;

    private CartRepository cartRepository;

    public CreditRepository getCreditRepository() {
        if (creditRepository == null) {
            creditRepository = new CreditRepository(() -> emf.createEntityManager());
        }
        return creditRepository;
    }

    public HeldOrderRepository getHeldOrderRepository() {
        if (heldOrderRepository == null) {
            heldOrderRepository = new HeldOrderRepository(() -> emf.createEntityManager());
        }
        return heldOrderRepository;
    }

    public CartRepository getCartRepository() {
        if (cartRepository == null) {
            cartRepository = new CartRepository(() -> emf.createEntityManager());
        }
        return cartRepository;
    }

    public UsersRepository getUsersRepository() {
        if(usersRepository==null)
        {
            usersRepository =
                    new UsersRepository(() -> emf.createEntityManager());
        }
        return usersRepository;
    }

    public RoleRepository getRoleRepository() {
        if(roleRepository==null)
        {
            roleRepository = new RoleRepository(() -> emf.createEntityManager());
        }
        return roleRepository;
    }

    public SaleRepository getSaleRepository() {
        if(saleRepository==null)
        {
            saleRepository = new SaleRepository(() -> emf.createEntityManager());
        }
        return saleRepository;
    }

    public CustomerRepository getCustomerRepository() {
        if(customerRepository==null)
        {
            customerRepository = new CustomerRepository(() -> emf.createEntityManager());
        }
        return customerRepository;
    }

    public StoreConfigRepository getStoreConfigRepository() {
        if(storeConfigRepository==null)
        {
            storeConfigRepository = new StoreConfigRepository(() -> emf.createEntityManager());
        }
        return storeConfigRepository;
    }

    public PurchaseRepository getPurchaseRepository() {
        if(purchaseRepository==null)
        {
            purchaseRepository = new PurchaseRepository(() -> emf.createEntityManager());
        }
        return purchaseRepository;
    }

    public ExpenseRepository getExpenseRepository() {
        if(expenseRepository==null)
        {
            expenseRepository = new ExpenseRepository(() -> emf.createEntityManager());
        }
        return expenseRepository;
    }
}
