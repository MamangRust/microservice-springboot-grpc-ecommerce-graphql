package com.sanedge.ecommerce.role.repository;

import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.role.models.Role;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
public class RoleCommandRepositoryImpl implements RoleCommandRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    private static final String TRASHED_QUERY = """
            UPDATE roles
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE role_id = :roleId
              AND deleted_at IS NULL
            """;

    private static final String RESTORE_QUERY = """
            UPDATE roles
            SET deleted_at = NULL
            WHERE role_id = :roleId
              AND deleted_at IS NOT NULL
            """;

    private static final String DELETE_PERMANENT_QUERY = """
            DELETE FROM roles
            WHERE role_id = :roleId
              AND deleted_at IS NOT NULL
            """;

    private static final String RESTORE_ALL_QUERY = """
            UPDATE roles
            SET deleted_at = NULL
            WHERE deleted_at IS NOT NULL
            """;

    private static final String DELETE_ALL_QUERY = """
            DELETE FROM roles
            WHERE deleted_at IS NOT NULL
            """;

    @Override
    @Transactional
    public Role trashed(Long roleId) {
        Role role = em.find(Role.class, roleId);

        if (role != null) {
            em.createNativeQuery(TRASHED_QUERY)
                    .setParameter("roleId", roleId)
                    .executeUpdate();

            em.refresh(role);
        }

        return role;
    }

    @Override
    @Transactional
    public Role restore(Long roleId) {
        Role role = em.find(Role.class, roleId);

        if (role != null) {
            em.createNativeQuery(RESTORE_QUERY)
                    .setParameter("roleId", roleId)
                    .executeUpdate();

            em.refresh(role);
        }

        return role;
    }

    @Override
    @Transactional
    public Role deletePermanent(Long roleId) {
        Role role = em.find(Role.class, roleId);

        if (role != null) {
            em.createNativeQuery(DELETE_PERMANENT_QUERY)
                    .setParameter("roleId", roleId)
                    .executeUpdate();

            em.detach(role);
        }

        return role;
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
