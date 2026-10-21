package com.sanedge.ecommerce.gateway.graphql.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.protobuf.Empty;
import com.sanedge.ecommerce.common.grpc.ProtoMappers;
import com.sanedge.ecommerce.gateway.dto.responses.merchant.MerchantResponse;
import com.sanedge.ecommerce.gateway.dto.responses.merchant.MerchantResponseDeleteAt;
import com.sanedge.ecommerce.gateway.dto.responses.merchantaward.MerchantAwardResponse;
import com.sanedge.ecommerce.gateway.dto.responses.merchantaward.MerchantAwardResponseDeleteAt;
import com.sanedge.ecommerce.gateway.dto.responses.merchantbusiness.MerchantBusinessResponse;
import com.sanedge.ecommerce.gateway.dto.responses.merchantbusiness.MerchantBusinessResponseDeleteAt;
import com.sanedge.ecommerce.gateway.dto.responses.merchantdetail.MerchantDetailRelationResponse;
import com.sanedge.ecommerce.gateway.dto.responses.merchantdetail.MerchantDetailRelationResponseDeleteAt;
import com.sanedge.ecommerce.gateway.dto.responses.merchantdetail.MerchantDetailResponse;
import com.sanedge.ecommerce.gateway.dto.responses.merchantdetail.MerchantDetailResponseDeleteAt;
import com.sanedge.ecommerce.gateway.dto.responses.merchantpolicy.MerchantPoliciesResponse;
import com.sanedge.ecommerce.gateway.dto.responses.merchantpolicy.MerchantPoliciesResponseDeleteAt;
import com.sanedge.ecommerce.gateway.dto.responses.merchantsociallink.MerchantSocialMediaLinkResponse;
import com.sanedge.ecommerce.gateway.graphql.Gql;
import com.sanedge.ecommerce.gateway.graphql.dto.Page;
import com.sanedge.ecommerce.gateway.mapper.ProtoMapper;
import com.sanedge.ecommerce.pb.merchant.MerchantCommandServiceGrpc;
import com.sanedge.ecommerce.pb.merchant.MerchantQueryServiceGrpc;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MerchantResolver {

    private final MerchantQueryServiceGrpc.MerchantQueryServiceBlockingStub merchantQueryStub;
    private final MerchantCommandServiceGrpc.MerchantCommandServiceBlockingStub merchantCommandStub;
    private final com.sanedge.ecommerce.pb.merchant_award.MerchantAwardQueryServiceGrpc.MerchantAwardQueryServiceBlockingStub awardQueryStub;
    private final com.sanedge.ecommerce.pb.merchant_award.MerchantAwardCommandServiceGrpc.MerchantAwardCommandServiceBlockingStub awardCommandStub;
    private final com.sanedge.ecommerce.pb.merchant_business.MerchantBusinessQueryServiceGrpc.MerchantBusinessQueryServiceBlockingStub businessQueryStub;
    private final com.sanedge.ecommerce.pb.merchant_business.MerchantBusinessCommandServiceGrpc.MerchantBusinessCommandServiceBlockingStub businessCommandStub;
    private final com.sanedge.ecommerce.pb.merchant_policy.MerchantPolicyQueryServiceGrpc.MerchantPolicyQueryServiceBlockingStub policyQueryStub;
    private final com.sanedge.ecommerce.pb.merchant_policy.MerchantPolicyCommandServiceGrpc.MerchantPolicyCommandServiceBlockingStub policyCommandStub;
    private final com.sanedge.ecommerce.pb.merchant_detail.MerchantDetailQueryServiceGrpc.MerchantDetailQueryServiceBlockingStub detailQueryStub;
    private final com.sanedge.ecommerce.pb.merchant_detail.MerchantDetailCommandServiceGrpc.MerchantDetailCommandServiceBlockingStub detailCommandStub;
    private final com.sanedge.ecommerce.pb.merchant_social_link.MerchantSocialCommandServiceGrpc.MerchantSocialCommandServiceBlockingStub socialCommandStub;

    private static com.sanedge.ecommerce.pb.merchant.FindAllMerchantRequest listReq(Integer page, Integer pageSize,
            String search) {
        return com.sanedge.ecommerce.pb.merchant.FindAllMerchantRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build();
    }

    // ------------------------------------------------------------------ merchant

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<MerchantResponse> merchants(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = merchantQueryStub.findAll(listReq(page, pageSize, search));
        return Page.of(ProtoMapper.merchants(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public MerchantResponse merchant(@Argument Long id) {
        var resp = merchantQueryStub.findById(com.sanedge.ecommerce.pb.merchant.FindByIdMerchantRequest.newBuilder()
                .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.merchant(resp.getData());
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Page<MerchantResponseDeleteAt> activeMerchants(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = merchantQueryStub.findByActive(listReq(page, pageSize, search));
        return Page.of(ProtoMapper.merchantsDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Page<MerchantResponseDeleteAt> trashedMerchants(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = merchantQueryStub.findByTrashed(listReq(page, pageSize, search));
        return Page.of(ProtoMapper.merchantsDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantResponse createMerchant(@Argument("input") CreateMerchant input) {
        var resp = merchantCommandStub.create(com.sanedge.ecommerce.pb.merchant.CreateMerchantRequest.newBuilder()
                .setUserId(Gql.i(input.userId() == null ? null : input.userId().intValue()))
                .setName(Gql.s(input.name()))
                .setDescription(Gql.s(input.description()))
                .setAddress(Gql.s(input.address()))
                .setContactEmail(Gql.s(input.contactEmail()))
                .setContactPhone(Gql.s(input.contactPhone()))
                .setStatus(Gql.s(input.status()))
                .build());
        return ProtoMapper.merchant(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantResponse updateMerchant(@Argument("input") UpdateMerchant input) {
        var resp = merchantCommandStub.update(com.sanedge.ecommerce.pb.merchant.UpdateMerchantRequest.newBuilder()
                .setMerchantId(Gql.i(input.merchantId() == null ? null : input.merchantId().intValue()))
                .setUserId(Gql.i(input.userId() == null ? null : input.userId().intValue()))
                .setName(Gql.s(input.name()))
                .setDescription(Gql.s(input.description()))
                .setAddress(Gql.s(input.address()))
                .setContactEmail(Gql.s(input.contactEmail()))
                .setContactPhone(Gql.s(input.contactPhone()))
                .setStatus(Gql.s(input.status()))
                .build());
        return ProtoMapper.merchant(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantResponseDeleteAt trashMerchant(@Argument Long id) {
        var resp = merchantCommandStub.trashedMerchant(com.sanedge.ecommerce.pb.merchant.FindByIdMerchantRequest
                .newBuilder().setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.merchantDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantResponseDeleteAt restoreMerchant(@Argument Long id) {
        var resp = merchantCommandStub.restoreMerchant(com.sanedge.ecommerce.pb.merchant.FindByIdMerchantRequest
                .newBuilder().setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.merchantDeleteAtFromResponse(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteMerchantPermanent(@Argument Long id) {
        var resp = merchantCommandStub.deleteMerchantPermanent(com.sanedge.ecommerce.pb.merchant.FindByIdMerchantRequest
                .newBuilder().setId(id == null ? 0 : id.intValue()).build());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean restoreAllMerchants() {
        var resp = merchantCommandStub.restoreAllMerchant(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteAllMerchantsPermanent() {
        var resp = merchantCommandStub.deleteAllMerchantPermanent(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    // ------------------------------------------------------------- merchant award

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<MerchantAwardResponse> merchantAwards(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = awardQueryStub.findAll(listReq(page, pageSize, search));
        return Page.of(ProtoMapper.merchantAwards(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public MerchantAwardResponse merchantAward(@Argument Long id) {
        var resp = awardQueryStub.findById(com.sanedge.ecommerce.pb.merchant_award.FindByIdMerchantAwardRequest
                .newBuilder().setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.merchantAward(resp.getData());
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Page<MerchantAwardResponseDeleteAt> activeMerchantAwards(@Argument Integer page,
            @Argument Integer pageSize, @Argument String search) {
        var resp = awardQueryStub.findByActive(listReq(page, pageSize, search));
        return Page.of(ProtoMapper.merchantAwardsDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Page<MerchantAwardResponseDeleteAt> trashedMerchantAwards(@Argument Integer page,
            @Argument Integer pageSize, @Argument String search) {
        var resp = awardQueryStub.findByTrashed(listReq(page, pageSize, search));
        return Page.of(ProtoMapper.merchantAwardsDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantAwardResponse createMerchantAward(@Argument("input") CreateMerchantAward input) {
        var resp = awardCommandStub.create(com.sanedge.ecommerce.pb.merchant_award.CreateMerchantAwardRequest
                .newBuilder()
                .setMerchantId(Gql.i(input.merchantId() == null ? null : input.merchantId().intValue()))
                .setTitle(Gql.s(input.title()))
                .setDescription(Gql.s(input.description()))
                .setIssuedBy(Gql.s(input.issuedBy()))
                .setIssueDate(Gql.s(input.issueDate()))
                .setExpiryDate(Gql.s(input.expiryDate()))
                .setCertificateUrl(Gql.s(input.certificateUrl()))
                .build());
        return ProtoMapper.merchantAward(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantAwardResponse updateMerchantAward(@Argument("input") UpdateMerchantAward input) {
        var resp = awardCommandStub.update(com.sanedge.ecommerce.pb.merchant_award.UpdateMerchantAwardRequest
                .newBuilder()
                .setMerchantCertificationId(
                        Gql.i(input.merchantCertificationId() == null ? null : input.merchantCertificationId().intValue()))
                .setTitle(Gql.s(input.title()))
                .setDescription(Gql.s(input.description()))
                .setIssuedBy(Gql.s(input.issuedBy()))
                .setIssueDate(Gql.s(input.issueDate()))
                .setExpiryDate(Gql.s(input.expiryDate()))
                .setCertificateUrl(Gql.s(input.certificateUrl()))
                .build());
        return ProtoMapper.merchantAward(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantAwardResponseDeleteAt trashMerchantAward(@Argument Long id) {
        var resp = awardCommandStub.trashedMerchantAward(
                com.sanedge.ecommerce.pb.merchant_award.FindByIdMerchantAwardRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.merchantAwardDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantAwardResponseDeleteAt restoreMerchantAward(@Argument Long id) {
        var resp = awardCommandStub.restoreMerchantAward(
                com.sanedge.ecommerce.pb.merchant_award.FindByIdMerchantAwardRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.merchantAwardDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteMerchantAwardPermanent(@Argument Long id) {
        var resp = awardCommandStub.deleteMerchantAwardPermanent(
                com.sanedge.ecommerce.pb.merchant_award.FindByIdMerchantAwardRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean restoreAllMerchantAwards() {
        var resp = awardCommandStub.restoreAllMerchantAward(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteAllMerchantAwardsPermanent() {
        var resp = awardCommandStub.deleteAllMerchantAwardPermanent(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    // ---------------------------------------------------------- merchant business

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<MerchantBusinessResponse> merchantBusinesses(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = businessQueryStub.findAll(listReq(page, pageSize, search));
        return Page.of(ProtoMapper.merchantBusinesses(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public MerchantBusinessResponse merchantBusiness(@Argument Long id) {
        var resp = businessQueryStub.findById(
                com.sanedge.ecommerce.pb.merchant_business.FindByIdMerchantBusinessRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.merchantBusiness(resp.getData());
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Page<MerchantBusinessResponseDeleteAt> activeMerchantBusinesses(@Argument Integer page,
            @Argument Integer pageSize, @Argument String search) {
        var resp = businessQueryStub.findByActive(listReq(page, pageSize, search));
        return Page.of(ProtoMapper.merchantBusinessesDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Page<MerchantBusinessResponseDeleteAt> trashedMerchantBusinesses(@Argument Integer page,
            @Argument Integer pageSize, @Argument String search) {
        var resp = businessQueryStub.findByTrashed(listReq(page, pageSize, search));
        return Page.of(ProtoMapper.merchantBusinessesDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantBusinessResponse createMerchantBusiness(@Argument("input") CreateMerchantBusiness input) {
        var resp = businessCommandStub.create(
                com.sanedge.ecommerce.pb.merchant_business.CreateMerchantBusinessRequest.newBuilder()
                        .setMerchantId(Gql.i(input.merchantId() == null ? null : input.merchantId().intValue()))
                        .setBusinessType(Gql.s(input.businessType()))
                        .setTaxId(Gql.s(input.taxId()))
                        .setEstablishedYear(Gql.i(input.establishedYear()))
                        .setNumberOfEmployees(Gql.i(input.numberOfEmployees()))
                        .setWebsiteUrl(Gql.s(input.websiteUrl()))
                        .build());
        return ProtoMapper.merchantBusiness(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantBusinessResponse updateMerchantBusiness(@Argument("input") UpdateMerchantBusiness input) {
        var resp = businessCommandStub.update(
                com.sanedge.ecommerce.pb.merchant_business.UpdateMerchantBusinessRequest.newBuilder()
                        .setMerchantBusinessInfoId(
                                Gql.i(input.merchantBusinessInfoId() == null ? null
                                        : input.merchantBusinessInfoId().intValue()))
                        .setBusinessType(Gql.s(input.businessType()))
                        .setTaxId(Gql.s(input.taxId()))
                        .setEstablishedYear(Gql.i(input.establishedYear()))
                        .setNumberOfEmployees(Gql.i(input.numberOfEmployees()))
                        .setWebsiteUrl(Gql.s(input.websiteUrl()))
                        .build());
        return ProtoMapper.merchantBusiness(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantBusinessResponseDeleteAt trashMerchantBusiness(@Argument Long id) {
        var resp = businessCommandStub.trashedMerchantBusiness(
                com.sanedge.ecommerce.pb.merchant_business.FindByIdMerchantBusinessRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.merchantBusinessDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantBusinessResponseDeleteAt restoreMerchantBusiness(@Argument Long id) {
        var resp = businessCommandStub.restoreMerchantBusiness(
                com.sanedge.ecommerce.pb.merchant_business.FindByIdMerchantBusinessRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.merchantBusinessDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteMerchantBusinessPermanent(@Argument Long id) {
        var resp = businessCommandStub.deleteMerchantBusinessPermanent(
                com.sanedge.ecommerce.pb.merchant_business.FindByIdMerchantBusinessRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean restoreAllMerchantBusinesses() {
        var resp = businessCommandStub.restoreAllMerchantBusiness(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteAllMerchantBusinessesPermanent() {
        var resp = businessCommandStub.deleteAllMerchantBusinessPermanent(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    // ------------------------------------------------------------ merchant policy

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<MerchantPoliciesResponse> merchantPolicies(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = policyQueryStub.findAll(listReq(page, pageSize, search));
        return Page.of(ProtoMapper.merchantPoliciesList(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public MerchantPoliciesResponse merchantPolicy(@Argument Long id) {
        var resp = policyQueryStub.findById(
                com.sanedge.ecommerce.pb.merchant_policy.FindByIdMerchantPoliciesRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.merchantPolicies(resp.getData());
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<MerchantPoliciesResponseDeleteAt> activeMerchantPolicies(@Argument Integer page,
            @Argument Integer pageSize, @Argument String search) {
        var resp = policyQueryStub.findByActive(listReq(page, pageSize, search));
        return Page.of(ProtoMapper.merchantPoliciesDeleteAtList(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<MerchantPoliciesResponseDeleteAt> trashedMerchantPolicies(@Argument Integer page,
            @Argument Integer pageSize, @Argument String search) {
        var resp = policyQueryStub.findByTrashed(listReq(page, pageSize, search));
        return Page.of(ProtoMapper.merchantPoliciesDeleteAtList(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantPoliciesResponse createMerchantPolicy(@Argument("input") CreateMerchantPolicy input) {
        var resp = policyCommandStub.create(
                com.sanedge.ecommerce.pb.merchant_policy.CreateMerchantPoliciesRequest.newBuilder()
                        .setMerchantId(Gql.i(input.merchantId() == null ? null : input.merchantId().intValue()))
                        .setPolicyType(Gql.s(input.policyType()))
                        .setTitle(Gql.s(input.title()))
                        .setDescription(Gql.s(input.description()))
                        .build());
        return ProtoMapper.merchantPolicies(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantPoliciesResponse updateMerchantPolicy(@Argument("input") UpdateMerchantPolicy input) {
        var resp = policyCommandStub.update(
                com.sanedge.ecommerce.pb.merchant_policy.UpdateMerchantPoliciesRequest.newBuilder()
                        .setMerchantPolicyId(
                                Gql.i(input.merchantPolicyId() == null ? null : input.merchantPolicyId().intValue()))
                        .setPolicyType(Gql.s(input.policyType()))
                        .setTitle(Gql.s(input.title()))
                        .setDescription(Gql.s(input.description()))
                        .build());
        return ProtoMapper.merchantPolicies(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantPoliciesResponseDeleteAt trashMerchantPolicy(@Argument Long id) {
        var resp = policyCommandStub.trashedMerchantPolicies(
                com.sanedge.ecommerce.pb.merchant_policy.FindByIdMerchantPoliciesRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.merchantPoliciesDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantPoliciesResponseDeleteAt restoreMerchantPolicy(@Argument Long id) {
        var resp = policyCommandStub.restoreMerchantPolicies(
                com.sanedge.ecommerce.pb.merchant_policy.FindByIdMerchantPoliciesRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.merchantPoliciesDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteMerchantPolicyPermanent(@Argument Long id) {
        var resp = policyCommandStub.deleteMerchantPoliciesPermanent(
                com.sanedge.ecommerce.pb.merchant_policy.FindByIdMerchantPoliciesRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean restoreAllMerchantPolicies() {
        var resp = policyCommandStub.restoreAllMerchantPolicies(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteAllMerchantPoliciesPermanent() {
        var resp = policyCommandStub.deleteAllMerchantPoliciesPermanent(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    // ------------------------------------------------------------ merchant detail

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Page<MerchantDetailRelationResponse> merchantDetails(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = detailQueryStub.findAll(listReq(page, pageSize, search));
        return Page.of(ProtoMapper.merchantDetailRelations(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public MerchantDetailRelationResponse merchantDetail(@Argument Long id) {
        var resp = detailQueryStub.findById(
                com.sanedge.ecommerce.pb.merchant_detail.FindByIdMerchantDetailRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.merchantDetailRelation(resp.getData());
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<MerchantDetailRelationResponseDeleteAt> activeMerchantDetails(@Argument Integer page,
            @Argument Integer pageSize, @Argument String search) {
        var resp = detailQueryStub.findByActive(listReq(page, pageSize, search));
        return Page.of(ProtoMapper.merchantDetailRelationsDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<MerchantDetailRelationResponseDeleteAt> trashedMerchantDetails(@Argument Integer page,
            @Argument Integer pageSize, @Argument String search) {
        var resp = detailQueryStub.findByTrashed(listReq(page, pageSize, search));
        return Page.of(ProtoMapper.merchantDetailRelationsDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantDetailResponse createMerchantDetail(@Argument("input") CreateMerchantDetail input) {
        var resp = detailCommandStub.create(
                com.sanedge.ecommerce.pb.merchant_detail.CreateMerchantDetailRequest.newBuilder()
                        .setMerchantId(Gql.i(input.merchantId() == null ? null : input.merchantId().intValue()))
                        .setDisplayName(Gql.s(input.displayName()))
                        .setCoverImageUrl(Gql.s(input.coverImageUrl()))
                        .setLogoUrl(Gql.s(input.logoUrl()))
                        .setShortDescription(Gql.s(input.shortDescription()))
                        .setWebsiteUrl(Gql.s(input.websiteUrl()))
                        .build());
        return ProtoMapper.merchantDetail(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantDetailResponse updateMerchantDetail(@Argument("input") UpdateMerchantDetail input) {
        var resp = detailCommandStub.update(
                com.sanedge.ecommerce.pb.merchant_detail.UpdateMerchantDetailRequest.newBuilder()
                        .setMerchantDetailId(
                                Gql.i(input.merchantDetailId() == null ? null : input.merchantDetailId().intValue()))
                        .setDisplayName(Gql.s(input.displayName()))
                        .setCoverImageUrl(Gql.s(input.coverImageUrl()))
                        .setLogoUrl(Gql.s(input.logoUrl()))
                        .setShortDescription(Gql.s(input.shortDescription()))
                        .setWebsiteUrl(Gql.s(input.websiteUrl()))
                        .build());
        return ProtoMapper.merchantDetail(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantDetailResponseDeleteAt trashMerchantDetail(@Argument Long id) {
        var resp = detailCommandStub.trashedMerchantDetail(
                com.sanedge.ecommerce.pb.merchant_detail.FindByIdMerchantDetailRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.merchantDetailDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantDetailResponseDeleteAt restoreMerchantDetail(@Argument Long id) {
        var resp = detailCommandStub.restoreMerchantDetail(
                com.sanedge.ecommerce.pb.merchant_detail.FindByIdMerchantDetailRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.merchantDetailDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteMerchantDetailPermanent(@Argument Long id) {
        var resp = detailCommandStub.deleteMerchantDetailPermanent(
                com.sanedge.ecommerce.pb.merchant_detail.FindByIdMerchantDetailRequest.newBuilder()
                        .setId(id == null ? 0 : id.intValue()).build());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean restoreAllMerchantDetails() {
        var resp = detailCommandStub.restoreAllMerchantDetail(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteAllMerchantDetailsPermanent() {
        var resp = detailCommandStub.deleteAllMerchantDetailPermanent(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    // -------------------------------------------------------- merchant social link

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantSocialMediaLinkResponse createMerchantSocial(@Argument("input") CreateMerchantSocial input) {
        var resp = socialCommandStub.create(
                com.sanedge.ecommerce.pb.merchant_social_link.CreateMerchantSocialRequest.newBuilder()
                        .setMerchantDetailId(
                                Gql.i(input.merchantDetailId() == null ? null : input.merchantDetailId().intValue()))
                        .setPlatform(Gql.s(input.platform()))
                        .setUrl(Gql.s(input.url()))
                        .build());
        return ProtoMapper.socialLink(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public MerchantSocialMediaLinkResponse updateMerchantSocial(@Argument("input") UpdateMerchantSocial input) {
        var resp = socialCommandStub.update(
                com.sanedge.ecommerce.pb.merchant_social_link.UpdateMerchantSocialRequest.newBuilder()
                        .setId(Gql.i(input.id() == null ? null : input.id().intValue()))
                        .setMerchantDetailId(
                                Gql.i(input.merchantDetailId() == null ? null : input.merchantDetailId().intValue()))
                        .setPlatform(Gql.s(input.platform()))
                        .setUrl(Gql.s(input.url()))
                        .build());
        return ProtoMapper.socialLink(resp.getData());
    }

    // ------------------------------------------------------------------- inputs

    public record CreateMerchant(Long userId, String name, String description, String address,
            String contactEmail, String contactPhone, String status) {
    }

    public record UpdateMerchant(Long merchantId, Long userId, String name, String description, String address,
            String contactEmail, String contactPhone, String status) {
    }

    public record CreateMerchantAward(Long merchantId, String title, String description, String issuedBy,
            String issueDate, String expiryDate, String certificateUrl) {
    }

    public record UpdateMerchantAward(Long merchantCertificationId, String title, String description,
            String issuedBy, String issueDate, String expiryDate, String certificateUrl) {
    }

    public record CreateMerchantBusiness(Long merchantId, String businessType, String taxId,
            Integer establishedYear, Integer numberOfEmployees, String websiteUrl) {
    }

    public record UpdateMerchantBusiness(Long merchantBusinessInfoId, String businessType, String taxId,
            Integer establishedYear, Integer numberOfEmployees, String websiteUrl) {
    }

    public record CreateMerchantPolicy(Long merchantId, String policyType, String title, String description) {
    }

    public record UpdateMerchantPolicy(Long merchantPolicyId, String policyType, String title, String description) {
    }

    public record CreateMerchantDetail(Long merchantId, String displayName, String coverImageUrl, String logoUrl,
            String shortDescription, String websiteUrl) {
    }

    public record UpdateMerchantDetail(Long merchantDetailId, String displayName, String coverImageUrl,
            String logoUrl, String shortDescription, String websiteUrl) {
    }

    public record CreateMerchantSocial(Long merchantDetailId, String platform, String url) {
    }

    public record UpdateMerchantSocial(Long id, Long merchantDetailId, String platform, String url) {
    }
}
