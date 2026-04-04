# GET /admin/categories/product

## Endpoint Documentation

**Endpoint:** `GET /api/admin/categories/product`  
**Authorization:** Required (Admin only - `AuthAdmin` middleware)  
**Description:** Pobiera wszystkie produkty pogrupowane po kategoriach z pełnymi danymi administracyjnymi.

---

## Response Model

### Response Structure
```typescript
{
  data: CategoryProductDto[],
  total: number
}
```

### CategoryProductDto
```typescript
{
  id?: string;                              // ID kategorii (MongoDB ObjectId)
  name: string | Record<string, string>;    // Nazwa kategorii (przetłumaczona lub obiekt tłumaczeń)
  slug: string;                             // Slug kategorii (URL-friendly)
  status: boolean;                          // Status kategorii (aktywna/nieaktywna)
  category_icon?: AttachmentDto | null;     // Ikona kategorii
  products: ProductDto[];                   // Lista produktów w kategorii
}
```

### AttachmentDto (category_icon)
```typescript
{
  id: string;                  // ID załącznika
  name: string;                // Nazwa pliku
  original_url: string;        // URL oryginalnego pliku
  url?: string;                // URL zoptymalizowanego pliku
  path?: string;               // Ścieżka do pliku
  mime_type: string | null;    // Typ MIME (np. "image/png")
  size: number;                // Rozmiar pliku w bajtach
}
```

### ProductDto (pełne dane dla admina)
```typescript
{
  // Podstawowe informacje
  id: string;                                    // ID produktu (MongoDB ObjectId)
  name: string | Record<string, string>;         // Nazwa produktu (przetłumaczona lub obiekt tłumaczeń)
  short_description?: string | Record<string, string>;  // Krótki opis
  slug?: string;                                 // Slug produktu (URL-friendly)
  sku?: string;                                  // SKU produktu
  pos_id?: string;                               // ID w systemie POS
  type: string;                                  // Typ produktu (np. "simple", "bundle")
  
  // Ceny i rabaty (w groszach lub PLN - sprawdź implementację centsToPln)
  price: number;                                 // Cena regularna
  sale_price?: number;                           // Cena promocyjna
  discount?: number | null;                      // Wartość rabatu
  
  // Stany i statusy
  status?: boolean;                              // Status produktu (aktywny/nieaktywny)
  stock_status?: ProductStockStatus;             // Status magazynowy ("IN_STOCK" | "OUT_OF_STOCK" | "PREORDER")
  
  // Dodatkowe informacje
  unit?: string | null;                          // Jednostka miary (np. "szt", "kg", "l")
  quantity?: number;                             // Ilość
  sort?: number;                                 // Pozycja sortowania
  product_type?: string;                         // Typ produktu (dodatkowy)
  printer?: PrinterEnum | null;                  // Drukarka przypisana do produktu
  availability?: any;                            // Dostępność produktu
  
  // Relacje
  tax_id?: string;                               // ID stawki VAT
  tax?: TaxDto;                                  // Obiekt stawki VAT
  product_thumbnail_id?: string | null;          // ID głównego zdjęcia
  product_thumbnail?: AttachmentDto | null;      // Główne zdjęcie produktu
  product_galleries: AttachmentDto[];            // Galeria zdjęć produktu
  
  // Kategorie i atrybuty
  categories: CategoryDto[];                     // Lista kategorii produktu
  allergens: AllergenDto[];                      // Lista alergenów
  addons_group?: AddonGroupDto[];                // Grupy dodatków
  
  // Bundle
  bundle_items?: BundleItemDto[];                // Produkty w zestawie (jeśli type = "bundle")
  
  // Produkty powiązane
  related_products?: string[];                   // ID powiązanych produktów
  cross_sell_products?: string[];                // ID produktów do cross-sellingu
}
```

### ProductStockStatus (enum)
```typescript
enum ProductStockStatus {
  IN_STOCK = "IN_STOCK",           // Dostępny w magazynie
  OUT_OF_STOCK = "OUT_OF_STOCK",   // Niedostępny
  PREORDER = "PREORDER"            // Przedsprzedaż
}
```

### PrinterEnum (enum)
```typescript
enum PrinterEnum {
  BAR = "BAR",
  KITCHEN = "KITCHEN",
  NONE = "NONE"
}
```

---

## Example Response

```json
{
  "data": [
    {
      "id": "507f1f77bcf86cd799439011",
      "name": "Sushi",
      "slug": "sushi",
      "status": true,
      "category_icon": {
        "id": "507f191e810c19729de860ea",
        "name": "sushi-icon.png",
        "original_url": "https://cdn.example.com/sushi-icon.png",
        "mime_type": "image/png",
        "size": 15234
      },
      "products": [
        {
          "id": "507f1f77bcf86cd799439012",
          "name": "California Roll",
          "short_description": "Tradycyjne sushi z awokado",
          "slug": "california-roll",
          "sku": "SUSHI-001",
          "pos_id": "123",
          "type": "simple",
          "price": 2500,
          "sale_price": 2000,
          "discount": 500,
          "status": true,
          "stock_status": "IN_STOCK",
          "unit": "szt",
          "quantity": 8,
          "sort": 1,
          "printer": "KITCHEN",
          "product_thumbnail": {
            "id": "507f191e810c19729de860eb",
            "name": "california-roll.jpg",
            "original_url": "https://cdn.example.com/california-roll.jpg",
            "mime_type": "image/jpeg",
            "size": 45678
          },
          "product_galleries": [],
          "categories": [
            {
              "id": "507f1f77bcf86cd799439011",
              "name": "Sushi",
              "slug": "sushi",
              "status": true
            }
          ],
          "allergens": [
            {
              "id": "507f191e810c19729de860ec",
              "name": "Ryby",
              "slug": "ryby"
            }
          ],
          "addons_group": [
            {
              "id": "507f191e810c19729de860ed",
              "name": "Sosy",
              "addons": [...]
            }
          ],
          "availability": null,
          "related_products": ["507f1f77bcf86cd799439013"],
          "cross_sell_products": []
        }
      ]
    },
    {
      "id": "507f1f77bcf86cd799439014",
      "name": "Zupy",
      "slug": "zupy",
      "status": true,
      "category_icon": null,
      "products": [...]
    }
  ],
  "total": 2
}
```

---

## Query Parameters

Endpoint nie przyjmuje parametrów query. Zwraca wszystkie produkty ze wszystkich aktywnych kategorii.

---

## Filtry w Repository

Metoda `repository.getProductAll()` używa agregacji `productAggregateByCategory` z następującymi filtrami:
- `stock_status: "IN_STOCK"` - tylko produkty dostępne w magazynie
- `status: true` - tylko aktywne produkty
- Sortowanie: `sort: 1` (pozycja kategorii), `product_sort: 1` (pozycja produktu w kategorii)

---

## Error Responses

### 401 Unauthorized
```json
{
  "message": "Unauthorized",
  "statusCode": 401
}
```

### 500 Internal Server Error
```json
{
  "message": "Internal server error",
  "statusCode": 500
}
```

---

## Notes

1. **Ceny:** Wartości `price`, `sale_price`, `discount` mogą być w groszach - zweryfikuj z funkcją `centsToPln()` czy następuje konwersja
2. **Tłumaczenia:** Pola `name`, `short_description` mogą być stringiem (przetłumaczone na locale) lub obiektem `{ pl: "...", en: "..." }`
3. **Relacje:** Produkty zawierają pełne obiekty powiązanych encji (kategorie, alergeny, dodatki, zdjęcia) zamiast samych ID
4. **Bundle items:** Dla produktów typu "bundle" pole `bundle_items` zawiera listę produktów wchodzących w skład zestawu
5. **Availability:** Pole `availability` zawiera informacje o dostępności czasowej produktu (godziny, dni tygodnia)

---

## Usage Example

### JavaScript/TypeScript
```typescript
const response = await fetch('/api/admin/categories/product', {
  headers: {
    'Authorization': `Bearer ${adminToken}`,
    'Content-Type': 'application/json'
  }
});

const { data, total } = await response.json();

console.log(`Loaded ${total} categories`);
data.forEach(category => {
  console.log(`${category.name}: ${category.products.length} products`);
});
```

### cURL
```bash
curl -X GET "https://api.example.com/api/admin/categories/product" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json"
```
