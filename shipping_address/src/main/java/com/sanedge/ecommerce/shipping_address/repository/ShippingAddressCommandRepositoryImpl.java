package com.sanedge.ecommerce.shipping_address.repository;

import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.shipping_address.models.ShippingAddress;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
public class ShippingAddressCommandRepositoryImpl implements ShippingAddressCommandRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    private static final String TRASHED_QUERY = """
            UPDATE shipping_addresses
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE shipping_address_id = :shippingAddressId
              AND deleted_at IS NULL
            """;

    private static final String RESTORE_QUERY = """
            UPDATE shipping_addresses
            SET deleted_at = NULL
            WHERE shipping_address_id = :shippingAddressId
              AND deleted_at IS NOT NULL
            """;

    private static final String DELETE_PERMANENT_QUERY = """
            DELETE FROM shipping_addresses
            WHERE shipping_address_id = :shippingAddressId
              AND deleted_at IS NOT NULL
            """;

    private static final String RESTORE_ALL_QUERY = """
            UPDATE shipping_addresses
            SET deleted_at = NULL
            WHERE deleted_at IS NOT NULL
            """;

    private static final String DELETE_ALL_QUERY = """
            DELETE FROM shipping_addresses
            WHERE deleted_at IS NOT NULL
            """;

    @Override
    @Transactional
    public ShippingAddress trashed(Long shippingAddressId) {
        ShippingAddress address = em.find(ShippingAddress.class, shippingAddressId);

        if (address != null) {
            em.createNativeQuery(TRASHED_QUERY)
                    .setParameter("shippingAddressId", shippingAddressId)
                    .executeUpdate();

            em.refresh(address);
        }

        return address;
    }

    @Override
    @Transactional
    public ShippingAddress restore(Long shippingAddressId) {
        ShippingAddress address = em.find(ShippingAddress.class, shippingAddressId);

        if (address != null) {
            em.createNativeQuery(RESTORE_QUERY)
                    .setParameter("shippingAddressId", shippingAddressId)
                    .executeUpdate();

            em.refresh(address);
        }

        return address;
    }

    @Override
    @Transactional
    public boolean deletePermanent(Long shippingAddressId) {
        ShippingAddress address = em.find(ShippingAddress.class, shippingAddressId);

        if (address != null) {
            int deleted = em.createNativeQuery(DELETE_PERMANENT_QUERY)
                    .setParameter("shippingAddressId", shippingAddressId)
                    .executeUpdate();

            em.detach(address);

            return deleted > 0;
        }

        return false;
    }

    @Override
    @Transactional
    public boolean restoreAllDeleted() {
        int updated = em.createNativeQuery(RESTORE_ALL_QUERY)
                .executeUpdate();

        return updated > 0;
    }

    @Override
    @Transactional
    public boolean deleteAllDeleted() {
        int deleted = em.createNativeQuery(DELETE_ALL_QUERY)
                .executeUpdate();

        return deleted > 0;
    }
}
