package com.sanedge.ecommerce.merchant.repository;

import com.sanedge.ecommerce.merchant.models.MerchantDocument;

/** Soft-delete operations for {@code merchant_documents}. */
public interface MerchantDocumentCommandRepositoryCustom {

    MerchantDocument trashed(Long documentId);

    MerchantDocument restore(Long documentId);

    MerchantDocument deletePermanent(Long documentId);

    boolean restoreAllDeleted();

    boolean deleteAllDeleted();
}
