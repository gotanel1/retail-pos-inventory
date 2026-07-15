# Catalog API

Catalog endpoints require an authenticated session. Mutating requests also require the CSRF header described in [Authentication API](authentication.md).

`OWNER`, `MANAGER`, and `INVENTORY_STAFF` may mutate catalog data. `CASHIER` may only read products and categories.

## List products

```http
GET /api/v1/products?search=hammer&page=0&size=20&sort=name,asc HTTP/1.1
Accept: application/json
```

Search matches product name, SKU, or barcode. The response uses zero-based pagination:

```json
{
  "content": [
    {
      "id": "6d8bc4de-7d63-4ed8-b3e5-d244e351334c",
      "categoryId": "9810b9fd-3194-43f7-9a26-65beae83bb2b",
      "categoryName": "Tools",
      "sku": "HAMMER-01",
      "barcode": "885000000001",
      "name": "Claw hammer",
      "salePrice": 250.00,
      "lowStockThreshold": 3,
      "active": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

## Create a category and product

```http
POST /api/v1/categories HTTP/1.1
Content-Type: application/json
X-CSRF-TOKEN: generated-token

{ "name": "Tools" }
```

```http
POST /api/v1/products HTTP/1.1
Content-Type: application/json
X-CSRF-TOKEN: generated-token

{
  "categoryId": "9810b9fd-3194-43f7-9a26-65beae83bb2b",
  "sku": "HAMMER-01",
  "barcode": "885000000001",
  "name": "Claw hammer",
  "salePrice": 250.00,
  "lowStockThreshold": 3
}
```

## Preview and commit a CSV import

The UTF-8 CSV must contain these headers: `sku`, `barcode`, `name`, `category`, `salePrice`, and `lowStockThreshold`. The maximum upload is 2 MB and 1,000 data rows.

```http
POST /api/v1/product-imports/preview HTTP/1.1
Content-Type: multipart/form-data; boundary=generated-by-client
X-CSRF-TOKEN: generated-token

file=@products.csv
```

Previewing never creates categories or products. It returns row-level validation errors and an import ID that expires after one hour.

```http
POST /api/v1/product-imports/{importId}/commit HTTP/1.1
X-CSRF-TOKEN: generated-token
```

Commit reparses and revalidates the original content, locks the import record against duplicate commits, and creates all categories and products in one database transaction. If any row is no longer valid, no catalog data is created.

Validation and conflict responses use `application/problem+json`.
