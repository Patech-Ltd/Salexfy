package org.PSmart.POS.Repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Generic base repository for common CRUD operations.
 * Thread-safe implementation using instance-based EntityManager.
 *
 * @param <T> The entity type this repository manages
 */
public abstract class BaseRepository<T> {

    protected final Class<T> entityClass;
    protected final Supplier<EntityManager> entityManagerSupplier;

    /**
     * Constructor that automatically detects the entity class from generic type.
     *
     * @param entityManagerSupplier Supplier that provides EntityManager instances
     */
    @SuppressWarnings("unchecked")
    protected BaseRepository(Supplier<EntityManager> entityManagerSupplier) {
        this.entityManagerSupplier = entityManagerSupplier;
        // Get the actual class of T through reflection
        this.entityClass = (Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
    }

    /**
     * Constructor with explicit entity class (useful when reflection doesn't work).
     */
    protected BaseRepository(Class<T> entityClass, Supplier<EntityManager> entityManagerSupplier) {
        this.entityClass = entityClass;
        this.entityManagerSupplier = entityManagerSupplier;
    }

    /**
     * Get the current EntityManager instance.
     */
    protected EntityManager getEntityManager() {
        return entityManagerSupplier.get();
    }

    /**
     * Find entity by name (assumes entity has a 'name' field and 'deleted' field).
     */
    public Optional<T> findByName(String name) {
        try {
            CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
            CriteriaQuery<T> cq = cb.createQuery(entityClass);
            Root<T> root = cq.from(entityClass);

            cq.select(root).where(
                    cb.and(
                            cb.equal(root.get("deleted"), false),
                            cb.equal(root.get("name"), name)
                    )
            );

            return Optional.ofNullable(getEntityManager().createQuery(cq).getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } catch (Exception e) {
            throw new RepositoryException("Error finding entity by name: " + name, e);
        }
    }

    /**
     * Find all non-deleted entities.
     */
    public List<T> findAll() {
        try {
            CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
            CriteriaQuery<T> cq = cb.createQuery(entityClass);
            Root<T> root = cq.from(entityClass);

            cq.select(root).where(cb.equal(root.get("deleted"), false));

            return getEntityManager().createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error finding all entities", e);
        }
    }

    /**
     * Find all non-deleted entities ordered by name ascending.
     */
    public List<T> findAllAsc() {
        try {
            CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
            CriteriaQuery<T> cq = cb.createQuery(entityClass);
            Root<T> root = cq.from(entityClass);

            cq.select(root)
                    .where(cb.equal(root.get("deleted"), false))
                    .orderBy(cb.asc(root.get("name")));

            return getEntityManager().createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error finding all entities ordered by name", e);
        }
    }

    /**
     * Find entity by ID.
     */
    public Optional<T> findById(UUID id) {
        try {
            T entity = getEntityManager().find(entityClass, id);
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            throw new RepositoryException("Error finding entity by ID: " + id, e);
        }
    }

    /**
     * Search entities by name or description (partial match).
     */
    public List<T> searchByNameOrDescription(String searchTerm) {
        try {
            CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
            CriteriaQuery<T> cq = cb.createQuery(entityClass);
            Root<T> root = cq.from(entityClass);

            String likePattern = "%" + searchTerm + "%";

            Predicate nameLike = cb.like(cb.lower(root.get("name")), likePattern.toLowerCase());
            Predicate descLike = cb.like(cb.lower(root.get("description")), likePattern.toLowerCase());
            Predicate notDeleted = cb.equal(root.get("deleted"), false);

            cq.select(root).where(
                    cb.and(notDeleted, cb.or(nameLike, descLike))
            );

            return getEntityManager().createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error searching entities by: " + searchTerm, e);
        }
    }

    /**
     * Save a new entity.
     */
    public T save(T entity) {
        EntityTransaction transaction = null;
        try {
            EntityManager em = getEntityManager();
            transaction = em.getTransaction();
            transaction.begin();
            em.persist(entity);
            transaction.commit();
            return entity;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RepositoryException("Error saving entity", e);
        }
    }

    /**
     * Update an existing entity.
     */
    public T update(T entity) {
        EntityTransaction transaction = null;
        try {
            EntityManager em = getEntityManager();
            transaction = em.getTransaction();
            transaction.begin();
            T merged = em.merge(entity);
            transaction.commit();
            return merged;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RepositoryException("Error updating entity", e);
        }
    }

    /**
     * Soft delete an entity (sets deleted flag to true).
     */
    public void delete(T entity) {
        EntityTransaction transaction = null;
        try {
            EntityManager em = getEntityManager();
            transaction = em.getTransaction();
            transaction.begin();

            // Use reflection to set the deleted field
            java.lang.reflect.Method setDeleted = entityClass.getMethod("setDeleted", boolean.class);
            setDeleted.invoke(entity, true);

            em.merge(entity);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RepositoryException("Error deleting entity", e);
        }
    }

    /**
     * Hard delete an entity (permanently removes from database).
     */
    public void hardDelete(T entity) {
        EntityTransaction transaction = null;
        try {
            EntityManager em = getEntityManager();
            transaction = em.getTransaction();
            transaction.begin();

            // Ensure entity is managed
            if (!em.contains(entity)) {
                entity = em.merge(entity);
            }

            em.remove(entity);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RepositoryException("Error hard deleting entity", e);
        }
    }

    /**
     * Count all non-deleted entities.
     */
    public long count() {
        try {
            CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<T> root = cq.from(entityClass);

            cq.select(cb.count(root)).where(cb.equal(root.get("deleted"), false));

            return getEntityManager().createQuery(cq).getSingleResult();
        } catch (Exception e) {
            throw new RepositoryException("Error counting entities", e);
        }
    }

    /**
     * Check if entity exists by ID.
     */
    public boolean existsById(UUID id) {
        return findById(id).isPresent();
    }

    /**
     * Custom exception for repository errors.
     */
    public static class RepositoryException extends RuntimeException {
        public RepositoryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}