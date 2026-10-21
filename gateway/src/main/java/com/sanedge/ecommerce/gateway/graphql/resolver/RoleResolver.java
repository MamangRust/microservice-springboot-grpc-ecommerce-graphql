package com.sanedge.ecommerce.gateway.graphql.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.protobuf.Empty;
import com.sanedge.ecommerce.common.grpc.ProtoMappers;
import com.sanedge.ecommerce.gateway.dto.responses.role.RoleResponse;
import com.sanedge.ecommerce.gateway.dto.responses.role.RoleResponseDeleteAt;
import com.sanedge.ecommerce.gateway.graphql.Gql;
import com.sanedge.ecommerce.gateway.graphql.dto.Page;
import com.sanedge.ecommerce.gateway.mapper.ProtoMapper;
import com.sanedge.ecommerce.pb.role.RoleCommandServiceGrpc;
import com.sanedge.ecommerce.pb.role.RoleQueryServiceGrpc;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RoleResolver {

    private final RoleQueryServiceGrpc.RoleQueryServiceBlockingStub roleQueryStub;
    private final RoleCommandServiceGrpc.RoleCommandServiceBlockingStub roleCommandStub;

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<RoleResponse> roles(@Argument Integer page, @Argument Integer pageSize, @Argument String search) {
        var resp = roleQueryStub.findAllRole(com.sanedge.ecommerce.pb.role.FindAllRoleRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.roles(resp.getDataList()), ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public RoleResponse role(@Argument Long id) {
        var resp = roleQueryStub.findByIdRole(com.sanedge.ecommerce.pb.role.FindByIdRoleRequest.newBuilder()
                .setRoleId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.role(resp.getData());
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<RoleResponseDeleteAt> activeRoles(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = roleQueryStub.findByActive(com.sanedge.ecommerce.pb.role.FindAllRoleRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.rolesDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<RoleResponseDeleteAt> trashedRoles(@Argument Integer page, @Argument Integer pageSize,
            @Argument String search) {
        var resp = roleQueryStub.findByTrashed(com.sanedge.ecommerce.pb.role.FindAllRoleRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.rolesDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public java.util.List<RoleResponse> rolesByUser(@Argument Long userId) {
        var resp = roleQueryStub.findByUserId(com.sanedge.ecommerce.pb.role.FindByIdUserRoleRequest.newBuilder()
                .setUserId(userId == null ? 0 : userId.intValue()).build());
        return ProtoMapper.roles(resp.getDataList());
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public RoleResponse roleByName(@Argument String name) {
        var resp = roleQueryStub.findByNameRole(com.sanedge.ecommerce.pb.role.FindByNameRoleRequest.newBuilder()
                .setName(Gql.s(name)).build());
        return ProtoMapper.role(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public RoleResponse createRole(@Argument("input") CreateRole input) {
        var resp = roleCommandStub.createRole(com.sanedge.ecommerce.pb.role.CreateRoleRequest.newBuilder()
                .setName(Gql.s(input.name())).build());
        return ProtoMapper.role(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public RoleResponse updateRole(@Argument("input") UpdateRole input) {
        var resp = roleCommandStub.updateRole(com.sanedge.ecommerce.pb.role.UpdateRoleRequest.newBuilder()
                .setId(input.id() == null ? 0 : input.id().intValue())
                .setName(Gql.s(input.name())).build());
        return ProtoMapper.role(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public RoleResponseDeleteAt trashRole(@Argument Long id) {
        var resp = roleCommandStub.trashedRole(com.sanedge.ecommerce.pb.role.FindByIdRoleRequest.newBuilder()
                .setRoleId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.roleDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public RoleResponseDeleteAt restoreRole(@Argument Long id) {
        var resp = roleCommandStub.restoreRole(com.sanedge.ecommerce.pb.role.FindByIdRoleRequest.newBuilder()
                .setRoleId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.roleDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteRolePermanent(@Argument Long id) {
        var resp = roleCommandStub.deleteRolePermanent(com.sanedge.ecommerce.pb.role.FindByIdRoleRequest.newBuilder()
                .setRoleId(id == null ? 0 : id.intValue()).build());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean restoreAllRoles() {
        var resp = roleCommandStub.restoreAllRole(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteAllRolesPermanent() {
        var resp = roleCommandStub.deleteAllRolePermanent(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    public record CreateRole(String name) {
    }

    public record UpdateRole(Long id, String name) {
    }
}
