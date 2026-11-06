# SCEX_TEST

Project dùng Java 25 và Maven Wrapper.

## Cấu trúc

- `domain/entity`: model bất biến và các ràng buộc dữ liệu của watchlist.
- `domain/service`: quy tắc áp dụng thao tác và xử lý xung đột merge.
- `application/usecase`: điểm vào nghiệp vụ `MergeWatchlist`, nhận domain service qua constructor.
- `infrastructure/configuration`: cấu hình Spring để khởi tạo và kết nối các thành phần.
- `ScexTestApplication`: khởi động ứng dụng.

Application phụ thuộc domain. Infrastructure phụ thuộc application và domain. Domain và application không phụ thuộc Spring.

## Kiểm thử

```sh
./mvnw test
```
