# Gateway REST → GraphQL — Rencana Migrasi

> Dokumen perencanaan + catatan implementasi. Scope: **hanya modul `gateway/`** — semua
> microservice domain (role, user, product, dst) tetap apa adanya (gRPC server + Flyway + Kafka).
> Gateway tetap **BFF tanpa database**: satu-satunya perubahan adalah lapisan edge
> dari REST/JSON menjadi GraphQL.
>
> **Status: SUDAH DIIMPLEMENTASIKAN.** Lihat §15 untuk ringkasan hasil dan deviasi dari rencana.

---

## 1. Tujuan & ruang lingkup

| | |
|---|---|
| **Masuk scope** | Modul `gateway` — ganti controller REST + DTO request/response menjadi schema GraphQL + resolver. |
| **Tidak masuk scope** | Proto (`common/src/main/proto`), server gRPC domain, DB, Kafka, ClickHouse, deployment. |
| **Yang dipertahankan** | `GrpcClientConfig` (channel + blocking stub per service), `security/*` (JWT filter, entry point), `service/FileService` & `FolderService`, `domain.storage` static file serving. |
| **Tujuan teknis** | 1 endpoint `POST /graphql`, client bebas memilih field (hilangkan over-fetching), skema sebagai kontrak, tetap pakai protobuf stub yang sudah ada. |

### Kondisi saat ini

- 21 controller REST `@RestController` di `gateway/.../controller/`.
- Envelope seragam: `ApiResponse<T> { status, message, data }` dan
  `ApiResponsePagination<T> { status, message, data, pagination }`.
- Otorisasi per-endpoint via `@PreAuthorize("hasAnyRole(...)")` (`@EnableMethodSecurity` aktif).
- Error gRPC → HTTP dipetakan di `RestExceptionHandler`.
- Upload file multipart (product, category, slider, merchant-detail, review-detail) disimpan
  lewat `FileService`/`FolderService` lalu path disimpan sebagai string.
- Dokumentasi API: springdoc/Swagger UI di `/api-docs`.

---

## 2. Keputusan desain (yang perlu dikonfirmasi)

| # | Topik | Rekomendasi | Alternatif |
|---|---|---|---|
| D1 | Gaya schema | **Schema-first** (`src/main/resources/graphql/*.graphqls`) + `@QueryMapping`/`@MutationMapping` | Code-first (`@SchemaMapping` manual) — tidak dipilih |
| D2 | Framework | **Spring for GraphQL** (`spring-boot-starter-graphql`, versi ikut Spring Boot 3.5.6) | Netflix DGS, graphql-java langsung |
| D3 | Transport | Spring GraphQL WebMVC di **`POST /graphql`** (+ GraphiQL di `/graphiql`, dev only) | `/graphql` + tetap ada REST |
| D4 | Envelope `{status,message,data}` | **Dibuang** di GraphQL — field mengembalikan payload langsung; error lewat `errors[]` + `extensions` | Pertahankan envelope demi kompatibilitas |
| D5 | REST eksisting | **Dihapus** setelah GraphQL stabil | Dijalankan berdampingan (deprecate) |
| D6 | Naming | GraphQL camelCase (`accessToken`, `totalItems`) | Pertahankan snake_case (`access_token`) |
| D7 | Upload multipart | **Endpoint REST kecil `/api/upload`** → balikan URL, mutation terima `String` URL | `Upload` scalar ala GraphQL multipart spec |
| D8 | Pagination | Tipe `<Entity>Page { data: [T!]!, pagination: PaginationMeta! }` | Connection/Relay cursor |

> Rekomendasi default: D1 schema-first, D2 Spring for GraphQL, D4 buang envelope,
> D7 upload tetap via REST. Kalau ada yang ingin dipertahankan (mis. envelope), ubah dokumen ini dulu.

---

## 3. Perubahan dependency & konfigurasi

### `gateway/pom.xml`

Tambah:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-graphql</artifactId>
</dependency>
```

Biarkan tetap ada:

- `spring-boot-starter-web` → transport HTTP GraphQL (`POST /graphql`), static file `/static/**`,
  dan endpoint upload multipart.
- `spring-boot-starter-security` → JWT filter + `@PreAuthorize`.
- `spring-boot-starter-validation` → validasi input (`@Valid` pada argumen resolver).

Hapus setelah migrasi selesai (opsional, tidak wajib langsung):

- `springdoc-openapi-starter-webmvc-ui` → digantikan GraphiQL / `graphql schema` sebagai kontrak.
  Kalau ingin tetap ada docs REST untuk endpoint upload, tahan dulu.

### `application.properties` (tambahan)

```properties
# GraphQL
spring.graphql.graphiql.enabled=true          # dev; false di prod
spring.graphql.graphiql.path=/graphiql
spring.graphql.path=/graphql
spring.graphql.schema.locations=classpath:graphql/
spring.graphql.schema.file-extensions=.graphqls
```

`GrpcClientConfig`, `server.port=8080`, blok `grpc.client.*`, JWT, dan `spring.mvc.static-path-pattern`
**tidak berubah**.

---

## 4. Struktur package target

```
gateway/src/main/
├── resources/
│   ├── application.properties
│   └── graphql/
│       ├── schema.graphqls        # root Query & Mutation (gabungan)
│       ├── common.graphqls        # PaginationMeta, scalar Upload
│       ├── auth.graphqls
│       ├── user.graphqls
│       ├── role.graphqls
│       ├── merchant.graphqls      # merchant + award/business/detail/policy/social
│       ├── category.graphqls
│       ├── product.graphqls
│       ├── banner.graphqls
│       ├── slider.graphqls
│       ├── cart.graphqls
│       ├── order.graphqls         # order + order_item
│       ├── review.graphqls        # review + review_detail
│       ├── shipping_address.graphqls
│       ├── transaction.graphqls
│       └── stats.graphqls
└── java/com/sanedge/ecommerce/gateway/
    ├── config/            # GrpcClientConfig (tetap), SecurityConfig (tambah /graphql, /graphiql public), GraphQlConfig baru
    ├── controller/        # DIHAPUS setelah migrasi (kecuali UploadController kecil)
    ├── graphql/
    │   ├── query/         # @QueryMapping per domain
    │   ├── mutation/      # @MutationMapping per domain
    │   └── GraphQlExceptionHandler.java   # StatusRuntimeException -> GraphQLError
    ├── mapper/ProtoMapper.java            # DIPAKAI ULANG apa adanya
    ├── dto/               # tetap (ProtoMapper butuh) + *Input untuk argumen GraphQL
    ├── security/          # tetap
    └── service/           # FileService/FolderService tetap
```

Keputusan penting: **`ProtoMapper` dan semua DTO response dipertahankan.** Resolver memanggil
stub gRPC, memetakan lewat `ProtoMapper`, lalu mengembalikan DTO apa adanya. Jadi tidak ada
duplikasi logika mapping.

---

## 5. Konvensi penamaan & bentuk response

- **Query** (read) → nama jamak untuk list, tunggal untuk by-id:
  `products(...)`, `product(id)`, `activeProducts(...)`, `trashedProducts(...)`.
- **Mutation** (write) → kata kerja: `createProduct`, `updateProduct`, `trashProduct`,
  `restoreProduct`, `deleteProductPermanent`, `restoreAllProducts`, `deleteAllProductsPermanent`.
- **Input** memakai suffix `Input`, mutation memakai satu argumen `input`:
  `createProduct(input: CreateProductInput!): Product!`.
- **Argumen list/filter** (page, pageSize, search, minPrice, …) tetap sebagai argumen field,
  bukan dibungkus input, agar query mudah dibaca.
- **`ID`** untuk identifier, `Int`/`Float`/`String`/`Boolean` sesuai DTO. `Long id` di DTO
  dipetakan ke `ID` (aman untuk nilai besar).
- **Response list** selalu `XxxPage`, `data` non-null element, `pagination` non-null.
- **`*DeleteAt`** → tipe GraphQL tanpa suffix (mis. `ProductDeleteAt`) untuk membedakan
  field `deletedAt` yang hanya ada di hasil trash.

Contoh bentuk response GraphQL (pengganti `ApiResponsePagination`):

```graphql
type ProductPage {
  data: [Product!]!
  pagination: PaginationMeta!
}
```

---

## 6. Schema GraphQL lengkap

### 6.1 common.graphqls

```graphql
type PaginationMeta {
  page: Int!
  pageSize: Int!
  totalItems: Int!
  totalPages: Int!
}
```

### 6.2 auth.graphqls

```graphql
type Token {
  accessToken: String!
  refreshToken: String!
}

input RegisterInput {
  firstname: String!
  username: String
  lastname: String!
  email: String!
  password: String!
  confirmPassword: String!
}

input LoginInput {
  username: String!   # di REST namanya username, isinya email
  password: String!
}

extend type Query {
  me: User
}

extend type Mutation {
  register(input: RegisterInput!): User!
  login(input: LoginInput!): Token!
  refreshToken(refreshToken: String!): Token!
}
```

### 6.3 user.graphqls

```graphql
type User {
  id: ID!
  username: String
  firstname: String
  lastname: String
  email: String
  createdAt: String
  updatedAt: String
}

type UserDeleteAt {
  id: ID!
  firstname: String
  lastname: String
  email: String
  createdAt: String
  updatedAt: String
  deletedAt: String
}

type UserPage { data: [User!]! pagination: PaginationMeta! }
type UserDeleteAtPage { data: [UserDeleteAt!]! pagination: PaginationMeta! }

input CreateUserInput {
  username: String!
  firstname: String!
  lastname: String!
  email: String!
  password: String!
  confirmPassword: String!
}

input UpdateUserInput {
  id: ID!
  username: String
  firstname: String
  lastname: String
  email: String
  password: String
  confirmPassword: String
}

extend type Query {
  users(page: Int = 1, pageSize: Int = 10, search: String = ""): UserPage!          # ADMIN, STAFF
  user(id: ID!): User!                                                              # ADMIN, STAFF, USER
  activeUsers(page: Int = 1, pageSize: Int = 10, search: String = ""): UserDeleteAtPage!  # ADMIN, STAFF
  trashedUsers(page: Int = 1, pageSize: Int = 10, search: String = ""): UserDeleteAtPage! # ADMIN, STAFF
}

extend type Mutation {
  createUser(input: CreateUserInput!): User!          # ADMIN
  updateUser(input: UpdateUserInput!): User!          # ADMIN
  trashUser(id: ID!): UserDeleteAt!                   # ADMIN
  restoreUser(id: ID!): UserDeleteAt!                 # ADMIN
  deleteUserPermanent(id: ID!): Boolean!              # ADMIN
  restoreAllUsers: Boolean!                           # ADMIN
  deleteAllUsersPermanent: Boolean!                   # ADMIN
}
```

### 6.4 role.graphqls

```graphql
type Role {
  id: ID!
  name: String
  createdAt: String
  updatedAt: String
}

type RoleDeleteAt {
  id: ID!
  name: String
  createdAt: String
  updatedAt: String
  deletedAt: String
}

type RolePage { data: [Role!]! pagination: PaginationMeta! }
type RoleDeleteAtPage { data: [RoleDeleteAt!]! pagination: PaginationMeta! }

input CreateRoleInput { name: String! }
input UpdateRoleInput { id: ID!, name: String! }

extend type Query {
  roles(page: Int = 1, pageSize: Int = 10, search: String = ""): RolePage!   # ADMIN, STAFF
  role(id: ID!): Role!                                                       # ADMIN, STAFF
  activeRoles(page: Int = 1, pageSize: Int = 10, search: String = ""): RoleDeleteAtPage!   # ADMIN, STAFF
  trashedRoles(page: Int = 1, pageSize: Int = 10, search: String = ""): RoleDeleteAtPage!  # ADMIN, STAFF
  rolesByUser(userId: ID!): [Role!]!                                         # ADMIN, STAFF, USER
  roleByName(name: String!): Role!                                           # ADMIN, STAFF
}

extend type Mutation {
  createRole(input: CreateRoleInput!): Role!        # ADMIN
  updateRole(input: UpdateRoleInput!): Role!        # ADMIN
  trashRole(id: ID!): RoleDeleteAt!                 # ADMIN
  restoreRole(id: ID!): RoleDeleteAt!               # ADMIN
  deleteRolePermanent(id: ID!): Boolean!            # ADMIN
  restoreAllRoles: Boolean!                         # ADMIN
  deleteAllRolesPermanent: Boolean!                 # ADMIN
}
```

### 6.5 merchant.graphqls (6 domain sekaligus)

```graphql
type Merchant {
  id: ID! userId: Int name: String description: String address: String
  contactEmail: String contactPhone: String status: String
  createdAt: String updatedAt: String
}
type MerchantDeleteAt {
  id: ID! userId: Int name: String description: String address: String
  contactEmail: String contactPhone: String status: String
  createdAt: String updatedAt: String deletedAt: String
}
type MerchantAward {
  id: ID! merchantId: Int title: String description: String issuedBy: String
  issueDate: String expiryDate: String certificateUrl: String
  createdAt: String updatedAt: String
}
type MerchantAwardDeleteAt { ...idem + deletedAt: String }
type MerchantBusiness {
  id: ID! merchantId: Int businessType: String taxId: String
  establishedYear: Int numberOfEmployees: Int websiteUrl: String
  createdAt: String updatedAt: String
}
type MerchantBusinessDeleteAt { ...idem + deletedAt: String }
type MerchantPolicy {
  id: ID! merchantId: Int policyType: String title: String description: String
  createdAt: String updatedAt: String
}
type MerchantPolicyDeleteAt { ...idem + deletedAt: String }
type MerchantSocialMediaLink {
  id: ID! platform: String url: String createdAt: String updatedAt: String
}
type MerchantSocialMediaLinkDeleteAt { ...idem + deletedAt: String }
type MerchantDetail {
  id: ID! merchantId: Int displayName: String coverImageUrl: String logoUrl: String
  shortDescription: String websiteUrl: String createdAt: String updatedAt: String
}
type MerchantDetailDeleteAt { ...idem + deletedAt: String }
type MerchantDetailRelation {
  id: ID! merchantId: Int displayName: String coverImageUrl: String logoUrl: String
  shortDescription: String websiteUrl: String
  socialMediaLinks: [MerchantSocialMediaLink!]!
  createdAt: String updatedAt: String
}
type MerchantDetailRelationDeleteAt { ...idem + deletedAt: String }

# ... (Page types untuk tiap entity di atas, pola sama seperti UserPage)

input CreateMerchantInput { userId: ID! name: String! description: String address: String contactEmail: String contactPhone: String status: String }
input UpdateMerchantInput { merchantId: ID! userId: ID name: String description: String address: String contactEmail: String contactPhone: String status: String }
input CreateMerchantAwardInput { merchantId: ID! title: String! description: String issuedBy: String issueDate: String expiryDate: String certificateUrl: String }
input UpdateMerchantAwardInput { merchantCertificationId: ID! title: String description: String issuedBy: String issueDate: String expiryDate: String certificateUrl: String }
input CreateMerchantBusinessInput { merchantId: ID! businessType: String taxId: String establishedYear: Int numberOfEmployees: Int websiteUrl: String }
input UpdateMerchantBusinessInput { merchantBusinessInfoId: ID! businessType: String taxId: String establishedYear: Int numberOfEmployees: Int websiteUrl: String }
input CreateMerchantPolicyInput { merchantId: ID! policyType: String title: String! description: String }
input UpdateMerchantPolicyInput { merchantPolicyId: ID! policyType: String title: String description: String }
input CreateMerchantSocialInput { merchantDetailId: ID! platform: String! url: String! }
input UpdateMerchantSocialInput { id: ID! merchantDetailId: ID platform: String url: String }
input CreateMerchantDetailInput { merchantId: ID! displayName: String! coverImageUrl: String logoUrl: String shortDescription: String websiteUrl: String }
input UpdateMerchantDetailInput { merchantDetailId: ID! displayName: String coverImageUrl: String logoUrl: String shortDescription: String websiteUrl: String }
```

Query/mutation merchant mengikuti template CRUD standar:

```graphql
extend type Query {
  merchants(page: Int = 1, pageSize: Int = 10, search: String = ""): MerchantPage!            # ADMIN, STAFF, USER
  merchant(id: ID!): Merchant!                                                                 # ADMIN, STAFF, USER
  activeMerchants(...): MerchantDeleteAtPage!                                                  # ADMIN
  trashedMerchants(...): MerchantDeleteAtPage!                                                 # ADMIN
  # idem: merchantAwards, merchantBusinesses, merchantPolicies, merchantDetails, merchantSocialLinks
}
extend type Mutation {
  createMerchant(input: CreateMerchantInput!): Merchant!                                       # ADMIN
  updateMerchant(input: UpdateMerchantInput!): Merchant!                                       # ADMIN
  trashMerchant(id: ID!): MerchantDeleteAt!                                                    # ADMIN
  restoreMerchant(id: ID!): MerchantDeleteAt!                                                  # ADMIN
  deleteMerchantPermanent(id: ID!): Boolean!                                                   # ADMIN
  restoreAllMerchants: Boolean!                                                                # ADMIN
  deleteAllMerchantsPermanent: Boolean!                                                        # ADMIN
  # idem untuk award/business/policy/detail/social
}
```

> Catatan: `merchant-social-link` dan `merchant-award` di REST memakai prefix path tanpa `/api`
> dan `trash`/`delete` (bukan `trashed`/`permanent`). Di GraphQL **diseragamkan** jadi
> `trashX`/`restoreX`/`deleteXPermanent` supaya konsisten.

### 6.6 category.graphqls

```graphql
type Category {
  id: ID! name: String description: String slugCategory: String imageCategory: String
  createdAt: String updatedAt: String
}
type CategoryDeleteAt { ...idem + deletedAt: String }
type CategoryPage { data: [Category!]! pagination: PaginationMeta! }
type CategoryDeleteAtPage { data: [CategoryDeleteAt!]! pagination: PaginationMeta! }

input CreateCategoryInput { name: String! description: String slugCategory: String imageCategory: String }
input UpdateCategoryInput { categoryId: ID! name: String description: String slugCategory: String imageCategory: String }

extend type Query {
  categories(page: Int = 1, pageSize: Int = 10, search: String = ""): CategoryPage!   # ADMIN, STAFF, USER
  category(id: ID!): Category!                                                        # ADMIN, STAFF, USER
  activeCategories(...): CategoryDeleteAtPage!                                        # ADMIN, STAFF, USER
  trashedCategories(...): CategoryDeleteAtPage!                                       # ADMIN, STAFF, USER
}
extend type Mutation {
  createCategory(input: CreateCategoryInput!): Category!     # ADMIN, STAFF
  updateCategory(input: UpdateCategoryInput!): Category!     # ADMIN, STAFF
  trashCategory(id: ID!): CategoryDeleteAt!                  # ADMIN
  restoreCategory(id: ID!): CategoryDeleteAt!                # ADMIN
  deleteCategoryPermanent(id: ID!): Boolean!                 # ADMIN
  restoreAllCategories: Boolean!                             # ADMIN
  deleteAllCategoriesPermanent: Boolean!                     # ADMIN
}
```

### 6.7 product.graphqls

```graphql
type Product {
  id: ID! merchantId: Int categoryId: Int name: String description: String
  price: Int countInStock: Int brand: String weight: Int rating: Float
  slugProduct: String imageProduct: String createdAt: String updatedAt: String
}
type ProductDeleteAt { ...idem + deletedAt: String }
type ProductPage { data: [Product!]! pagination: PaginationMeta! }
type ProductDeleteAtPage { data: [ProductDeleteAt!]! pagination: PaginationMeta! }

input CreateProductInput {
  merchantId: ID! categoryId: ID! name: String! description: String
  price: Int countInStock: Int brand: String weight: Int rating: Int
  slugProduct: String imageProduct: String         # URL hasil /api/upload
}
input UpdateProductInput {
  productId: ID! merchantId: ID categoryId: ID name: String description: String
  price: Int countInStock: Int brand: String weight: Int rating: Int
  slugProduct: String imageProduct: String
}

extend type Query {
  products(page: Int = 1, pageSize: Int = 10, search: String = ""): ProductPage!        # ADMIN, STAFF, USER
  product(id: ID!): Product!                                                             # ADMIN, STAFF, USER
  productsByMerchant(merchantId: ID!, categoryId: ID, minPrice: Int, maxPrice: Int,
                     page: Int = 1, pageSize: Int = 10, search: String = ""): ProductPage!  # ADMIN, STAFF, USER
  productsByCategory(categoryName: String!, minPrice: Int, maxPrice: Int,
                     page: Int = 1, pageSize: Int = 10, search: String = ""): ProductPage!  # ADMIN, STAFF, USER
  activeProducts(page: Int = 1, pageSize: Int = 10, search: String = ""): ProductDeleteAtPage!   # ADMIN, STAFF
  trashedProducts(page: Int = 1, pageSize: Int = 10, search: String = ""): ProductDeleteAtPage!  # ADMIN, STAFF
}
extend type Mutation {
  createProduct(input: CreateProductInput!): Product!        # ADMIN
  updateProduct(input: UpdateProductInput!): Product!        # ADMIN
  trashProduct(id: ID!): ProductDeleteAt!                    # ADMIN
  restoreProduct(id: ID!): ProductDeleteAt!                  # ADMIN
  deleteProductPermanent(id: ID!): Boolean!                  # ADMIN
  restoreAllProducts: Boolean!                               # ADMIN
  deleteAllProductsPermanent: Boolean!                       # ADMIN
}
```

### 6.8 banner.graphqls

```graphql
type Banner {
  id: ID! name: String startDate: String endDate: String startTime: String endTime: String
  isActive: Boolean createdAt: String updatedAt: String
}
type BannerDeleteAt { ...idem + deletedAt: String }
type BannerPage { data: [Banner!]! pagination: PaginationMeta! }
type BannerDeleteAtPage { data: [BannerDeleteAt!]! pagination: PaginationMeta! }

input CreateBannerInput { name: String! startDate: String endDate: String startTime: String endTime: String isActive: Boolean }
input UpdateBannerInput { bannerId: ID! name: String startDate: String endDate: String startTime: String endTime: String isActive: Boolean }

extend type Query {
  banners(page: Int = 1, pageSize: Int = 10, search: String = ""): BannerPage!          # ADMIN, STAFF
  banner(id: ID!): Banner!                                                               # ADMIN, STAFF, USER
  activeBanners(...): BannerDeleteAtPage!                                                # ADMIN, STAFF
  trashedBanners(...): BannerDeleteAtPage!                                               # ADMIN, STAFF
}
extend type Mutation {
  createBanner(input: CreateBannerInput!): Banner!      # ADMIN, STAFF
  updateBanner(input: UpdateBannerInput!): Banner!      # ADMIN, STAFF
  trashBanner(id: ID!): BannerDeleteAt!                 # ADMIN
  restoreBanner(id: ID!): BannerDeleteAt!               # ADMIN
  deleteBannerPermanent(id: ID!): Boolean!              # ADMIN
  restoreAllBanners: Boolean!                           # ADMIN
  deleteAllBannersPermanent: Boolean!                   # ADMIN
}
```

### 6.9 slider.graphqls

```graphql
type Slider { id: ID! name: String image: String createdAt: String updatedAt: String }
type SliderDeleteAt { ...idem + deletedAt: String }
type SliderPage { data: [Slider!]! pagination: PaginationMeta! }
type SliderDeleteAtPage { data: [SliderDeleteAt!]! pagination: PaginationMeta! }

input CreateSliderInput { name: String! image: String }
input UpdateSliderInput { sliderId: ID! name: String image: String }

extend type Query {
  sliders(page: Int = 1, pageSize: Int = 10, search: String = ""): SliderPage!   # ADMIN, STAFF, USER
  activeSliders(...): SliderDeleteAtPage!                                        # ADMIN, STAFF
  trashedSliders(...): SliderDeleteAtPage!                                       # ADMIN, STAFF
}
extend type Mutation {
  createSlider(input: CreateSliderInput!): Slider!     # ADMIN
  updateSlider(input: UpdateSliderInput!): Slider!     # ADMIN
  trashSlider(id: ID!): SliderDeleteAt!                # ADMIN
  restoreSlider(id: ID!): SliderDeleteAt!              # ADMIN
  deleteSliderPermanent(id: ID!): Boolean!             # ADMIN
  restoreAllSliders: Boolean!                          # ADMIN
  deleteAllSlidersPermanent: Boolean!                  # ADMIN
}
# Catatan: REST slider TIDAK punya query by-id; tambahkan `slider(id)`
# hanya bila diinginkan (opsi, bukan paritas).
```

### 6.10 cart.graphqls

```graphql
type Cart {
  id: ID! userId: Int productId: Int name: String price: Int image: String
  quantity: Int weight: Int createdAt: String updatedAt: String
}
type CartPage { data: [Cart!]! pagination: PaginationMeta! }

input CreateCartInput { quantity: Int! productId: ID! userId: ID! }
input DeleteCartInput { cartIds: [ID!]! }

extend type Query {
  carts(userId: ID, page: Int = 1, pageSize: Int = 10, search: String = ""): CartPage!  # ADMIN, STAFF, USER
}
extend type Mutation {
  createCart(input: CreateCartInput!): Cart!        # ADMIN, STAFF, USER
  deleteCart(id: ID!): Boolean!                     # ADMIN, STAFF, USER
  deleteAllCarts(input: DeleteCartInput!): Boolean! # ADMIN, STAFF, USER
}
```

### 6.11 order.graphqls (order + order_item)

```graphql
type Order { id: ID! merchantId: Int userId: Int totalPrice: Int createdAt: String updatedAt: String }
type OrderDeleteAt { ...idem + deletedAt: String }
type OrderItem { id: ID! orderId: Int productId: Int quantity: Int price: Int createdAt: String updatedAt: String }
type OrderItemDeleteAt { ...idem + deletedAt: String }
type OrderRelation { orderId: ID! userId: Int merchantId: Int totalPrice: Int orderItems: [OrderItem!]! }
type OrderPage { data: [Order!]! pagination: PaginationMeta! }
type OrderDeleteAtPage { data: [OrderDeleteAt!]! pagination: PaginationMeta! }
type OrderItemPage { data: [OrderItem!]! pagination: PaginationMeta! }
type OrderItemDeleteAtPage { data: [OrderItemDeleteAt!]! pagination: PaginationMeta! }

input OrderItemInput { productId: ID! quantity: Int price: Int }
input CreateOrderInput { merchantId: ID! userId: ID! items: [OrderItemInput!]! shippingAddress: CreateShippingAddressInput }
input UpdateOrderItemInput { orderItemId: ID! productId: ID quantity: Int price: Int }
input UpdateOrderInput { orderId: ID! userId: ID items: [UpdateOrderItemInput!] shippingAddress: UpdateShippingAddressInput }

extend type Query {
  orders(page: Int = 1, pageSize: Int = 10, search: String = ""): OrderPage!                    # ADMIN, STAFF, USER
  order(id: ID!): Order!                                                                         # ADMIN, STAFF, USER
  orderRelation(orderId: ID!): OrderRelation!                                                    # ADMIN, STAFF, USER
  activeOrders(...): OrderDeleteAtPage!                                                          # ADMIN
  trashedOrders(...): OrderDeleteAtPage!                                                         # ADMIN
  ordersByMerchant(merchantId: ID!, page: Int = 1, pageSize: Int = 10, search: String = ""): OrderPage!  # ADMIN, STAFF
  # order_item (bila controller terpisah dibuka): ordersItems/orderItem/... pola CRUD sama
}
extend type Mutation {
  createOrder(input: CreateOrderInput!): Order!      # ADMIN
  updateOrder(input: UpdateOrderInput!): Order!      # ADMIN
  trashOrder(id: ID!): OrderDeleteAt!                # ADMIN
  restoreOrder(id: ID!): OrderDeleteAt!              # ADMIN
  deleteOrderPermanent(id: ID!): Boolean!            # ADMIN
  restoreAllOrders: Boolean!                         # ADMIN
  deleteAllOrdersPermanent: Boolean!                 # ADMIN
}
```

### 6.12 review.graphqls (review + review_detail)

```graphql
type Review { id: ID! userId: Int productId: Int name: String comment: String rating: Int createdAt: String updatedAt: String }
type ReviewDeleteAt { ...idem + deletedAt: String }
type ReviewDetail { id: ID! reviewId: Int type: String url: String caption: String createdAt: String updatedAt: String }
type ReviewDetailDeleteAt { ...idem + deletedAt: String }
type ReviewRelationsDetail { id: ID! userId: Int productId: Int name: String comment: String rating: Int reviewDetail: [ReviewDetail!]! createdAt: String updatedAt: String deletedAt: String }
type ReviewPage { data: [Review!]! pagination: PaginationMeta! }
type ReviewDeleteAtPage { data: [ReviewDeleteAt!]! pagination: PaginationMeta! }

input CreateReviewInput { userId: ID! productId: ID! rating: Int! comment: String }
input UpdateReviewInput { reviewId: ID! name: String rating: Int! comment: String }
input CreateReviewDetailInput { reviewId: ID! type: String! file: String caption: String }
input UpdateReviewDetailInput { reviewDetailId: ID! type: String file: String caption: String }

extend type Query {
  reviews(page: Int = 1, pageSize: Int = 20, search: String = ""): ReviewPage!                       # ADMIN, STAFF, USER
  reviewsByProduct(productId: ID!, rating: Int, page: Int = 1, pageSize: Int = 20, search: String = ""): ReviewPage!   # ADMIN, STAFF, USER
  reviewsByMerchant(merchantId: ID!, rating: Int, page: Int = 1, pageSize: Int = 20, search: String = ""): ReviewPage! # ADMIN, STAFF, USER
  review(reviewId: ID!): Review!                                                                     # ADMIN, STAFF, USER
  activeReviews(...): ReviewDeleteAtPage!                                                            # ADMIN
  trashedReviews(...): ReviewDeleteAtPage!                                                           # ADMIN
}
extend type Mutation {
  createReview(input: CreateReviewInput!): Review!      # ADMIN
  updateReview(input: UpdateReviewInput!): Review!      # ADMIN
  trashReview(reviewId: ID!): ReviewDeleteAt!           # ADMIN
  restoreReview(reviewId: ID!): ReviewDeleteAt!         # ADMIN
  deleteReviewPermanent(reviewId: ID!): Boolean!        # ADMIN
  restoreAllReviews: Boolean!                           # ADMIN
  deleteAllReviewsPermanent: Boolean!                   # ADMIN
  createReviewDetail(input: CreateReviewDetailInput!): ReviewDetail!    # ADMIN
  updateReviewDetail(input: UpdateReviewDetailInput!): ReviewDetail!    # ADMIN
  trashReviewDetail(reviewDetailId: ID!): ReviewDetailDeleteAt!         # ADMIN
  restoreReviewDetail(reviewDetailId: ID!): ReviewDetailDeleteAt!       # ADMIN
  deleteReviewDetailPermanent(reviewDetailId: ID!): Boolean!            # ADMIN
  restoreAllReviewDetails: Boolean!                                     # ADMIN
  deleteAllReviewDetailsPermanent: Boolean!                             # ADMIN
}
```

### 6.13 shipping_address.graphqls

```graphql
type ShippingAddress {
  id: ID! orderId: Int alamat: String provinsi: String negara: String kota: String
  shippingMethod: String shippingCost: Int createdAt: String updatedAt: String
}
type ShippingAddressDeleteAt { ...idem + deletedAt: String }
type ShippingAddressPage { data: [ShippingAddress!]! pagination: PaginationMeta! }
type ShippingAddressDeleteAtPage { data: [ShippingAddressDeleteAt!]! pagination: PaginationMeta! }

input CreateShippingAddressInput { orderId: ID! alamat: String! provinsi: String kota: String courier: String shippingMethod: String shippingCost: Int negara: String }
input UpdateShippingAddressInput { shippingId: ID! orderId: ID alamat: String provinsi: String kota: String courier: String shippingMethod: String shippingCost: Int negara: String }

extend type Query {
  shippingAddresses(page: Int = 1, pageSize: Int = 10, search: String = ""): ShippingAddressPage!  # ADMIN, STAFF, USER
  shippingAddress(shippingId: ID!): ShippingAddress!                                               # ADMIN, STAFF, USER
  shippingAddressByOrder(orderId: ID!): ShippingAddress!                                           # ADMIN, STAFF, USER
  activeShippingAddresses(...): ShippingAddressDeleteAtPage!                                       # ADMIN, STAFF
  trashedShippingAddresses(...): ShippingAddressDeleteAtPage!                                      # ADMIN, STAFF
}
extend type Mutation {
  createShippingAddress(input: CreateShippingAddressInput!): ShippingAddress!   # (dipanggil via order, atau eksplisit)
  updateShippingAddress(input: UpdateShippingAddressInput!): ShippingAddress!
  trashShippingAddress(shippingId: ID!): ShippingAddressDeleteAt!               # ADMIN
  restoreShippingAddress(shippingId: ID!): ShippingAddressDeleteAt!             # ADMIN
  deleteShippingAddressPermanent(shippingId: ID!): Boolean!                     # ADMIN
  restoreAllShippingAddresses: Boolean!                                         # ADMIN
  deleteAllShippingAddressesPermanent: Boolean!                                 # ADMIN
}
# Catatan: REST shipping_address TIDAK punya endpoint create/update langsung
# (dibuat bersama order); di GraphQL create/update diekspos hanya bila diinginkan.
```

### 6.14 transaction.graphqls

```graphql
type Transaction {
  id: ID! orderId: Int merchantId: Int paymentMethod: String amount: Int paymentStatus: String
  createdAt: String updatedAt: String
}
type TransactionDeleteAt { ...idem + deletedAt: String }
type TransactionPage { data: [Transaction!]! pagination: PaginationMeta! }
type TransactionDeleteAtPage { data: [TransactionDeleteAt!]! pagination: PaginationMeta! }

input CreateTransactionInput { orderId: ID! merchantId: ID! paymentMethod: String! amount: Int paymentStatus: String }
input UpdateTransactionInput { transactionId: ID! orderId: ID merchantId: ID paymentMethod: String amount: Int paymentStatus: String }

extend type Query {
  transactions(page: Int = 1, pageSize: Int = 10, search: String = ""): TransactionPage!        # ADMIN, STAFF, USER
  transaction(id: ID!): Transaction!                                                            # ADMIN, STAFF, USER
  transactionsByMerchant(merchantId: ID!, page: Int = 1, pageSize: Int = 10, search: String = ""): TransactionPage!  # ADMIN, STAFF
  activeTransactions(...): TransactionDeleteAtPage!                                             # ADMIN, STAFF
  trashedTransactions(...): TransactionDeleteAtPage!                                            # ADMIN, STAFF
}
extend type Mutation {
  createTransaction(input: CreateTransactionInput!): Transaction!   # ADMIN, STAFF, USER
  updateTransaction(input: UpdateTransactionInput!): Transaction!   # ADMIN
  trashTransaction(id: ID!): TransactionDeleteAt!                   # ADMIN
  restoreTransaction(id: ID!): TransactionDeleteAt!                 # ADMIN
  deleteTransactionPermanent(id: ID!): Boolean!                     # ADMIN
  restoreAllTransactions: Boolean!                                  # ADMIN
  deleteAllTransactionsPermanent: Boolean!                          # ADMIN
}
```

### 6.15 stats.graphqls

```graphql
type StatItem { period: String dimensionId: ID value: Float }

extend type Query {
  monthlyRevenue(year: Int, month: Int): [StatItem!]!    # ADMIN, STAFF
  yearlyRevenue(year: Int): [StatItem!]!                 # ADMIN, STAFF
  merchantSales(year: Int): [StatItem!]!                 # ADMIN, STAFF
  productSales(year: Int): [StatItem!]!                  # ADMIN, STAFF
}
```

### 6.16 Root schema

`schema.graphqls` hanya mendefinisikan root; file domain memakai `extend type Query/Mutation`:

```graphql
type Query { _root: Boolean }
type Mutation { _root: Boolean }
```

Spring GraphQL otomatis menggabungkan `extend` dari semua file `.graphqls`.

---

## 7. Tabel pemetaan REST → GraphQL

Template CRUD seragam (berlaku untuk role, user, merchant, category, product, banner,
slider, order, review, transaction, dan semua sub-merchant):

| REST (sekarang) | GraphQL (target) | HTTP status lama |
|---|---|---|
| `GET /api/<x>?page&pageSize&search` | `query { xs(page, pageSize, search) }` | 200 |
| `GET /api/<x>/{id}` | `query { x(id) }` | 200 |
| `GET /api/<x>/active` | `query { activeXs(...) }` | 200 |
| `GET /api/<x>/trashed` | `query { trashedXs(...) }` | 200 |
| `POST /api/<x>/create` | `mutation { createX(input) }` | 201 |
| `POST /api/<x>/update/{id}` | `mutation { updateX(input) }` | 200 |
| `POST /api/<x>/trashed/{id}` | `mutation { trashX(id) }` | 200 |
| `POST /api/<x>/restore/{id}` | `mutation { restoreX(id) }` | 200 |
| `DELETE /api/<x>/permanent/{id}` | `mutation { deleteXPermanent(id) }` | 200 |
| `POST /api/<x>/restore/all` | `mutation { restoreAllXs }` | 200 |
| `POST /api/<x>/permanent/all` | `mutation { deleteAllXsPermanent }` | 200 |

Query/mutation khusus (di luar template):

| REST | GraphQL |
|---|---|
| `POST /api/auth/register` | `mutation register(input)` |
| `POST /api/auth/login` | `mutation login(input)` |
| `POST /api/auth/refresh-token` | `mutation refreshToken(refreshToken)` |
| `GET /api/auth/me` | `query me` |
| `GET /api/auth/healthchecker` | dipertahankan sebagai `GET /health` (Actuator) / endpoint REST kecil |
| `GET /api/roles/by-user/{userId}` | `query rolesByUser(userId)` |
| `GET /api/roles/by-name?name=` | `query roleByName(name)` |
| `GET /api/product/merchant/{merchant_id}` | `query productsByMerchant(merchantId, ...)` |
| `GET /api/product/category/{category_name}` | `query productsByCategory(categoryName, ...)` |
| `GET /api/order/relation/{id}` | `query orderRelation(orderId)` |
| `GET /api/order/merchant/{merchant_id}` | `query ordersByMerchant(merchantId, ...)` |
| `GET /api/review/product/{productId}` | `query reviewsByProduct(...)` |
| `GET /api/review/merchant/{merchantId}` | `query reviewsByMerchant(...)` |
| `GET /api/shipping-address/order/{orderId}` | `query shippingAddressByOrder(orderId)` |
| `GET /api/transaction/merchant/{merchantId}` | `query transactionsByMerchant(merchantId, ...)` |
| `GET /api/stats/monthly-revenue` | `query monthlyRevenue(year, month)` |
| `GET /api/stats/yearly-revenue` | `query yearlyRevenue(year)` |
| `GET /api/stats/merchant-sales` | `query merchantSales(year)` |
| `GET /api/stats/product-sales` | `query productSales(year)` |
| `POST /api/cart/delete-all` | `mutation deleteAllCarts(input)` |

---

## 8. Upload file (multipart)

REST saat ini menerima `MultipartFile` di `@ModelAttribute` (`CreateProductRequest`, dll.) dan
menyimpan via `FileService`. GraphQL tidak punya konsep multipart native.

**Rekomendasi:** pisahkan upload dari GraphQL.

1. Tambah/pertahankan satu endpoint REST kecil: `POST /api/upload` (multipart `file`, param
   `folder`/`slug`) → `{ "url": "/static/product/xxx.png" }`.
2. Mutation di GraphQL menerima **string URL** (`imageProduct`, `imageCategory`, `image`,
   `coverImageUrl`, `logoUrl`, `file`).
3. `FileService`/`FolderService` dipakai ulang oleh `UploadController`.

Alur client: `POST /api/upload` (dapat URL) → `mutation createProduct(input:{... imageProduct: url })`.

**Alternatif (bila wajib satu endpoint):** implementasi `Upload` scalar + dukungan
"GraphQL multipart request spec". Lebih rumit (butuh custom `HttpMessageConverter`/`HandlerMethodArgumentResolver`),
tidak direkomendasikan untuk iterasi pertama.

---

## 9. Keamanan & otorisasi

- `SecurityConfig`: tambah `/graphql` dan `/graphiql` ke path yang relevan.
  - `/graphiql` → `permitAll()` (dev) atau matikan di prod.
  - `/graphql` → `authenticated()` (semua operasi butuh JWT), dengan pengecualian
    `login`/`register`/`refreshToken` yang diotorisasi di level resolver (anonim).
- `AuthTokenFilter` yang ada **tetap dipakai** — JWT dari header `Authorization: Bearer` masih
  mengisi `SecurityContextHolder`, jadi resolver aman memakai `SecurityContextHolder`.
- Otorisasi per-field: `@PreAuthorize("hasAnyRole('ADMIN','STAFF')")` **langsung di method**
  `@QueryMapping`/`@MutationMapping`. `@EnableMethodSecurity` sudah aktif. Ini menggantikan
  anotasi per-endpoint REST dengan biaya nol.
- Untuk operasi publik (login/register/refreshToken), tambahkan `/graphql` ke public list
  **hanya jika** seluruh field publik tidak butuh auth; lebih aman: buat `AuthorizationManager`
  khusus atau tandai resolver publik dengan `@PreAuthorize("permitAll()")`.

Contoh resolver:

```java
@Controller
@RequiredArgsConstructor
public class ProductQueryResolver {

    private final ProductQueryServiceGrpc.ProductQueryServiceBlockingStub productQueryStub;

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    public ProductPage products(
            @Argument Integer page, @Argument Integer pageSize, @Argument String search) {
        var resp = productQueryStub.findAll(FindAllProductRequest.newBuilder()
                .setPage(nvl(page)).setPageSize(nvl(pageSize)).setSearch(nvl(search)).build());
        return new ProductPage(ProtoMapper.products(resp.getDataList()),
                ProtoMappers.fromProtoPagination(resp.getPagination()));
    }
}
```

> Catatan: Spring Security per-field (`@PreAuthorize`) dievaluasi **per field** yang di-request,
> sehingga klien bisa memilih field terlarang → error `FORBIDDEN` di `errors[]`. Ini expected.

---

## 10. Error handling

Ganti `RestExceptionHandler` (`@RestControllerAdvice`) dengan resolver GraphQL:

```java
@ControllerAdvice
public class GraphQlExceptionHandler {

    @GraphQlExceptionHandler(StatusRuntimeException.class)
    public GraphQLError handleGrpc(StatusRuntimeException ex) {
        var code = ex.getStatus().getCode();
        var errorType = ErrorType.INTERNAL_ERROR;   // atau BAD_REQUEST/NOT_FOUND
        return GraphQLError.newError()
                .errorType(errorType)
                .message(ex.getStatus().getDescription())
                .extensions(Map.of(
                        "grpcCode", code.name(),
                        "httpStatus", httpStatusFor(code)))
                .build();
    }

    @GraphQlExceptionHandler({ConstraintViolationException.class, IllegalArgumentException.class})
    public GraphQLError handleBadRequest(Exception ex) {
        return GraphQLError.newError().errorType(ErrorType.BAD_REQUEST).message(ex.getMessage()).build();
    }
}
```

Pemetaan status dipertahankan iden dengan `RestExceptionHandler`:

| gRPC code | `extensions.httpStatus` |
|---|---|
| `NOT_FOUND` | 404 |
| `INVALID_ARGUMENT`, `FAILED_PRECONDITION` | 400 |
| `ALREADY_EXISTS` | 409 |
| `UNAUTHENTICATED` | 401 |
| `PERMISSION_DENIED` | 403 |
| lainnya | 500 |

Validasi input: `@Valid` pada argumen `input` (`@Validated` di resolver) → error `BAD_REQUEST`.
Jakarta validation tetap dipakai, DTO input di-annotate sama seperti DTO request sekarang.

Perbedaan penting: GraphQL selalu balikan HTTP 200 di transport; status "HTTP" lama pindah ke
`errors[].extensions.httpStatus`. Klien harus membaca `errors`, bukan status code.

---

## 11. Testing

- `GatewayApplicationTests` (context load) tetap; tambah konfigurasi GraphQL.
- **Resolver unit test**: mock stub gRPC (Mockito), panggil resolver langsung.
- **Schema test**: `HttpGraphQlTester` (bawaan Spring GraphQL) untuk integrasi endpoint `/graphql`
  tanpa jaringan gRPC (mock bean stub).
- **File query** `.graphql` di `src/test/resources/graphql-test/` (mis. `products.graphql`,
  `create-product.graphql`) sebagai dokumen uji — pola resmi Spring GraphQL.
- Uji otorisasi: request tanpa token → `UNAUTHENTICATED`; role salah → `FORBIDDEN`.
- Uji upload: `POST /api/upload` mengembalikan URL, lalu mutation memakai URL.

---

## 12. Rencana eksekusi (fase)

**Fase 0 — Persiapan**
- [ ] Konfirmasi keputusan §2 (terutama D4 envelope, D7 upload, D5 hapus REST).
- [ ] Tambah dependency `spring-boot-starter-graphql` + properti GraphQL.
- [ ] Buat kerangka `resources/graphql/` dan `graphql/query|mutation` package.

**Fase 1 — Pilot 1 domain (product)**
- [ ] Tulis `product.graphqls` + input.
- [ ] `ProductQueryResolver` + `ProductMutationResolver`.
- [ ] `GraphQlExceptionHandler`.
- [ ] Update `SecurityConfig` untuk `/graphql`.
- [ ] Uji manual di GraphiQL, bandingkan hasil dengan REST lama.

**Fase 2 — Domain CRUD lain**
- [ ] role, user, category, banner, slider, cart, transaction, shipping_address.
- [ ] merchant + 5 sub-domain.
- [ ] order + order_item, review + review_detail.
- [ ] auth (register/login/refresh/me).

**Fase 3 — Upload & stats**
- [ ] `UploadController` (`POST /api/upload`) memakai `FileService`.
- [ ] Resolver stats (4 query).

**Fase 4 — Pembersihan**
- [ ] Hapus controller REST + `RestExceptionHandler` + DTO request (simpan DTO response untuk ProtoMapper).
- [ ] Hapus `springdoc-openapi` bila tak diperlukan.
- [ ] Update README/Planning.md & hurl collection ke contoh GraphQL.
- [ ] Content negotiation: pastikan hanya `/graphql` yang tersisa (dan `/api/upload`, `/static/**`).

**Fase 5 — Verifikasi**
- [ ] Jalankan semua service + gateway, smoke test tiap domain via `HttpGraphQlTester`/GraphiQL.
- [ ] Cek otorisasi per role (ADMIN/STAFF/USER) untuk operasi sensitif.
- [ ] `mvn -pl gateway -am test`.

---

## 13. Risiko & pertanyaan terbuka

1. **Envelope**: klien frontend yang sudah ada bergantung pada `{status,message,data}`.
   Apakah perlu masa transisi (jalankan REST & GraphQL berdampingan) atau langsung putus?
2. **Upload**: apakah `POST /api/upload` (REST kecil) dapat diterima, atau harus full GraphQL?
3. **Nama field**: `access_token`/`accessToken` — apakah frontend dilonggarkan ke camelCase?
4. **ID type**: DTO memakai `Long`; GraphQL `ID` (string) vs `Int`. Rekomendasi `ID`.
5. **Endpoint by-id yang tidak ada di REST** (mis. `slider(id)`, `cart(id)`) — tambah untuk
   kelengkapan atau pertahankan paritas persis?
6. **`/api/auth/healthchecker`**: pindah ke Actuator `/actuator/health` atau pertahankan endpoint?
7. **Otorisasi field-level**: bila klien meminta field yang tidak diizinkan, seluruh query gagal
   (partial data + error). Apakah ini dapat diterima?
8. **File terpisah vs root schema**: tiap domain `extend type Query` — Spring GraphQL mendukung;
   pastikan tidak ada konflik nama tipe antar file.

---

## 14. Referensi internal

- `gateway/pom.xml` — dependency gateway.
- `gateway/.../config/GrpcClientConfig.java` — stub gRPC per service (dipakai ulang resolver).
- `gateway/.../config/SecurityConfig.java` — filter chain + public endpoints.
- `gateway/.../security/AuthTokenFilter.java` — JWT → SecurityContext.
- `gateway/.../mapper/ProtoMapper.java` — proto → DTO (dipakai ulang).
- `gateway/.../exception/RestExceptionHandler.java` — sumber pemetaan error gRPC → HTTP
  (sudah dihapus; logikanya dipindah ke `graphql/GraphQlExceptionHandler.java`).
- `common/src/main/proto/**` — kontrak gRPC tiap domain.

---

## 15. Status implementasi (hasil aktual)

Implementasi selesai; `gateway` **compile** dan context Spring **load** dengan schema valid.
Verifikasi: `./mvnw -o -pl gateway test`
→ `Tests run: 5, Failures: 0, Errors: 0, BUILD SUCCESS`.

Cakupan test:
- `GatewayApplicationTests` — seluruh bean & schema GraphQL dapat di-wire.
- `GraphQlEndpointTests` — query publik `healthchecker` berhasil; field ber-role tanpa token
  mengembalikan GraphQL error (bukan 500).
- `GraphQlResolverTests` — dengan stub gRPC di-mock:
  1. output `*Page` + `PaginationMeta` terserialisasi benar,
  2. input object GraphQL ter-bind ke record Java (diverifikasi via `ArgumentCaptor`),
  3. otorisasi `ROLE_ADMIN` pada resolver bekerja.

### Yang ditambahkan
- `pom.xml`: `spring-boot-starter-graphql` (+ `spring-graphql-test` untuk test), deskripsi diubah
  jadi "GraphQL gateway".
- `application.properties`: `spring.graphql.path=/graphql`, GraphiQL di `/graphiql`,
  `schema.locations=classpath:graphql/`, inspection aktif.
- `src/main/resources/graphql/*.graphqls` (16 file): `schema`, `common`, `auth`, `user`, `role`,
  `merchant`, `category`, `product`, `banner`, `slider`, `cart`, `order`, `review`, `shipping`,
  `transaction`, `stats`.
- `graphql/dto/Page.java` — payload paginasi generik (`data` + `pagination`); satu kelas Java
  melayani semua tipe `*Page` karena graphql-java membaca properti berdasarkan nama.
- `graphql/dto/Token.java` — token camelCase (`accessToken`/`refreshToken`) untuk GraphQL.
- `graphql/Gql.java` — helper `nvl` + `ok(status)` + `page(...)`.
- `graphql/GraphQlExceptionHandler.java` — `DataFetcherExceptionResolverAdapter` yang memetakan
  `StatusRuntimeException`, `ResourceNotFoundException`, `AccessDeniedException`,
  `AuthenticationException`, validasi, dan `UnsupportedOperationException` menjadi GraphQL error
  dengan `extensions.httpStatus` + `extensions.grpcCode`.
- `graphql/resolver/*.java` (14 resolver `@Controller`): `Auth`, `User`, `Role`, `Merchant`
  (6 sub-domain sekaligus), `Category`, `Product`, `Banner`, `Slider`, `Cart`, `Order`, `Review`,
  `ReviewDetail`, `Shipping`, `Transaction`, `Stats`.
- `controller/UploadController.java` — satu-satunya REST yang tersisa: `POST /api/upload`.
- `src/test/.../GraphQlEndpointTests.java` — smoke test endpoint + keamanan + error mapping.
- `src/test/.../GraphQlResolverTests.java` — test binding input object, serialisasi `*Page`, dan
  otorisasi resolver dengan stub gRPC di-mock.

### Yang dihapus
- 20 controller REST lama (semua kecuali `UploadController`).
- `exception/RestExceptionHandler.java` (digantikan `GraphQlExceptionHandler`).
- Seluruh `dto/requests/**` (61 file) — sudah tak terpakai; input GraphQL memakai record
  `*Input` di dalam masing-masing resolver. `dto/responses/**` tetap dipakai `ProtoMapper`.

### Deviasi dari rencana (disengaja)
1. **`order`**: field `orderRelation` dan `ordersByMerchant` **tidak** diekspos karena di REST
   lama keduanya melempar `UnsupportedOperationException`. Endpoint lain (findAll/findById/create/
   update/trash/restore/delete/all) tetap ada.
2. **`merchant_social_link`**: hanya `createMerchantSocial` + `updateMerchantSocial`
   (mengikuti gRPC; trash/restore/delete tidak tersedia).
3. **`review_detail` create/update**: di REST menerima banyak file Multipart; di GraphQL menerima
   **satu** `CreateReviewDetailInput` (URL tunggal hasil `/api/upload`). Untuk banyak gambar,
   pakai alias/beberapa mutation; `review_detail` juga tidak punya query by-id di REST.
4. **`slider`**: tidak ada query by-id (sama seperti REST).
5. **`shipping_address`**: tidak ada create/update (dibuat bersama order).
6. **Validasi**: mengandalkan validasi downstream (gRPC `INVALID_ARGUMENT` → error 400) plus
   anotasi di DTO. Gateway tidak lagi memakai `@Valid` pada request body lama.
7. **`healthchecker`**: dipindah dari `GET /api/auth/healthchecker` menjadi query
   `healthchecker: String!` (tetap kompatibel sebagai liveness sederhana).
8. **Grafik `_root`**: `type Query { _root: Boolean }` sengaja tak punya resolver; inspection
   hanya melaporkan `_root` sebagai unmapped (diharapkan).
9. **`springdoc`/OpenApiConfig** dipertahankan; kini hanya mendokumentasikan `/api/upload`.
10. **`null` argumen**: `@Argument` nullable dinormalisasi lewat `Gql.i/l/s` sebelum dikirim ke
    gRPC, sama seperti `nvl` di controller lama.

### Cara mencoba
```graphql
query { healthchecker }

mutation Login {
  login(input: { username: "admin@mail.com", password: "secret" }) {
    accessToken refreshToken
  }
}

# Token dari login → header Authorization: Bearer <accessToken>
query { products(page: 1, pageSize: 10) { data { id name price } pagination { totalItems } } }
```

### Yang belum dilakukan
- Belum diuji terhadap service gRPC hidup (butuh Docker Compose). Verifikasi saat ini: compile,
  context load, schema valid, public field sukses, field terproteksi mengembalikan GraphQL error.
- Koleksi `hurl`/klien REST lama perlu diperbarui (di luar scope kode gateway).
- Dokumentasi OpenAPI/Swagger kini tidak relevan untuk mayoritas endpoint (GraphQL self-documenting
  via GraphiQL).
