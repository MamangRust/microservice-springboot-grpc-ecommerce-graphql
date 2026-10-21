package com.sanedge.ecommerce.gateway.graphql.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.protobuf.Empty;
import com.sanedge.ecommerce.common.grpc.ProtoMappers;
import com.sanedge.ecommerce.gateway.graphql.Gql;
import com.sanedge.ecommerce.gateway.graphql.dto.Page;
import com.sanedge.ecommerce.gateway.mapper.ProtoMapper;
import com.sanedge.ecommerce.pb.user.UserCommandServiceGrpc;
import com.sanedge.ecommerce.pb.user.UserQueryServiceGrpc;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserResolver {

    private final UserQueryServiceGrpc.UserQueryServiceBlockingStub userQueryStub;
    private final UserCommandServiceGrpc.UserCommandServiceBlockingStub userCommandStub;

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<com.sanedge.ecommerce.gateway.dto.responses.user.UserResponse> users(
            @Argument Integer page, @Argument Integer pageSize, @Argument String search) {
        var resp = userQueryStub.findAll(com.sanedge.ecommerce.pb.user.FindAllUserRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.users(resp.getDataList()), ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public com.sanedge.ecommerce.gateway.dto.responses.user.UserResponse user(@Argument Long id) {
        var resp = userQueryStub.findById(com.sanedge.ecommerce.pb.user.FindByIdUserRequest.newBuilder()
                .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.user(resp.getData());
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<com.sanedge.ecommerce.gateway.dto.responses.user.UserResponseDeleteAt> activeUsers(
            @Argument Integer page, @Argument Integer pageSize, @Argument String search) {
        var resp = userQueryStub.findByActive(com.sanedge.ecommerce.pb.user.FindAllUserRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.usersDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Page<com.sanedge.ecommerce.gateway.dto.responses.user.UserResponseDeleteAt> trashedUsers(
            @Argument Integer page, @Argument Integer pageSize, @Argument String search) {
        var resp = userQueryStub.findByTrashed(com.sanedge.ecommerce.pb.user.FindAllUserRequest.newBuilder()
                .setPage(Gql.i(page)).setPageSize(Gql.i(pageSize)).setSearch(Gql.s(search)).build());
        return Page.of(ProtoMapper.usersDeleteAt(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public com.sanedge.ecommerce.gateway.dto.responses.user.UserResponse createUser(@Argument("input") CreateUser input) {
        var resp = userCommandStub.create(com.sanedge.ecommerce.pb.user.CreateUserRequest.newBuilder()
                .setFirstname(Gql.s(input.firstname()))
                .setLastname(Gql.s(input.lastname()))
                .setEmail(Gql.s(input.email()))
                .setPassword(Gql.s(input.password()))
                .setConfirmPassword(Gql.s(input.confirmPassword()))
                .build());
        return ProtoMapper.user(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public com.sanedge.ecommerce.gateway.dto.responses.user.UserResponse updateUser(@Argument("input") UpdateUser input) {
        var resp = userCommandStub.update(com.sanedge.ecommerce.pb.user.UpdateUserRequest.newBuilder()
                .setId(input.id() == null ? 0 : input.id().intValue())
                .setFirstname(Gql.s(input.firstname()))
                .setLastname(Gql.s(input.lastname()))
                .setEmail(Gql.s(input.email()))
                .setPassword(Gql.s(input.password()))
                .setConfirmPassword(Gql.s(input.confirmPassword()))
                .build());
        return ProtoMapper.user(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public com.sanedge.ecommerce.gateway.dto.responses.user.UserResponseDeleteAt trashUser(@Argument Long id) {
        var resp = userCommandStub.trashedUser(com.sanedge.ecommerce.pb.user.FindByIdUserRequest.newBuilder()
                .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.userDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public com.sanedge.ecommerce.gateway.dto.responses.user.UserResponseDeleteAt restoreUser(@Argument Long id) {
        var resp = userCommandStub.restoreUser(com.sanedge.ecommerce.pb.user.FindByIdUserRequest.newBuilder()
                .setId(id == null ? 0 : id.intValue()).build());
        return ProtoMapper.userDeleteAt(resp.getData());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteUserPermanent(@Argument Long id) {
        var resp = userCommandStub.deleteUserPermanent(com.sanedge.ecommerce.pb.user.FindByIdUserRequest.newBuilder()
                .setId(id == null ? 0 : id.intValue()).build());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean restoreAllUsers() {
        var resp = userCommandStub.restoreAllUser(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Boolean deleteAllUsersPermanent() {
        var resp = userCommandStub.deleteAllUserPermanent(Empty.getDefaultInstance());
        return Gql.ok(resp.getStatus());
    }

    public record CreateUser(String username, String firstname, String lastname, String email,
            String password, String confirmPassword) {
    }

    public record UpdateUser(Long id, String username, String firstname, String lastname, String email,
            String password, String confirmPassword) {
    }
}
