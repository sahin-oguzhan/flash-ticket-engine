# ⚡ Flash Ticket Engine

Spring Boot ile geliştirilen; flaş bilet satışı sırasındaki aşırı trafik ve yoğun yarış koşullarında (race conditions) stok aşımını (over-selling) önleyen, yüksek eşzamanlı bilet rezervasyon motoru.

High-concurrency ticket reservation engine built with Spring Boot, designed to prevent over-selling under extreme flash-sale traffic spikes using multiple concurrency control strategies.

---

## 🇹🇷 Türkçe

### 📌 Genel Bakış
Yoğun talep gören flaş bilet satışlarında binlerce kullanıcı aynı anda sınırlı sayıdaki bileti almaya çalışır. Doğru eşzamanlılık (concurrency) yönetimi uygulanmadığında yarış koşulları nedeniyle kapasite aşımı (over-selling) gerçekleşir. Bu projede problemi çözmek için 5 farklı mimari yaklaşım uygulanmış ve performansları kıyaslanmıştır:

1. **Naive Flow:** Eşzamanlılık kontrolü içermeyen temel akış (over-selling durumunu gözlemlemek için).
2. **Pessimistic Lock (Karamsar Kilit):** PostgreSQL `SELECT ... FOR UPDATE` ile satır düzeyinde kilitleme.
3. **Optimistic Lock (İyimser Kilit):** JPA `@Version` ile versiyon kontrolüne dayalı atomik güncelleme.
4. **Redis Distributed Lock (Dağıtık Kilit):** Redisson `RLock` ile dağıtık düğümler arası kilit senkronizasyonu.
5. **RabbitMQ Asynchronous Queue (Asenkron Kuyruk):** Mesaj kuyrukları ve DLQ ile olay güdümlü (event-driven) yük sönümleme.

### 🛠 Teknoloji Yığını
- **Backend:** Java 21, Spring Boot 4.1.1, Spring Data JPA
- **Veritabanı:** PostgreSQL 16
- **Dağıtık Önbellek ve Kilit:** Redis 7, Redisson
- **Mesajlaşma:** RabbitMQ 3 (AMQP 0-9-1)
- **Konteynerleştirme:** Docker, Docker Compose
- **Yük Testi:** Grafana k6

### 📊 Yük Testi ve Benchmark Sonuçları (k6)
**1.000 biletlik stok** üzerinde, **100 eşzamanlı sanal kullanıcı (VU)** ile **30 saniye** boyunca koşturulan flaş satış yük testi sonuçları:

| Eşzamanlılık Stratejisi | İstek Modeli | Toplam İstek (30s) | Throughput (RPS) | Ort. Gecikme | p95 Gecikme | Stok Aşımı (Over-Selling) |
|---|---|---|---|---|---|---|
| **Pessimistic Lock (`SELECT FOR UPDATE`)** | Senkron | 12.876 | ~427 req/s | 182 ms | 650 ms | **0** (1.000 Satış) |
| **Redis Distributed Lock (`Redisson RLock`)** | Senkron | 3.582 | ~117 req/s | 795 ms | 2.250 ms | **0** (1.000 Satış) |
| **RabbitMQ Asynchronous Queue (DLQ)** | Olay Güdümlü | **43.659** | **~1.452 req/s** | **16.7 ms** | **56.6 ms** | **0** (1.000 Satış) |

#### 🔍 Çıkarımlar
- **Olay Güdümlü Dayanıklılık:** RabbitMQ, HTTP istek kabul katmanını veritabanı yazma darboğazından ayırarak **11 kat daha düşük p95 gecikmesi** sundu ve tek bir istek dahi düşmeden yoğun trafiği sönümledi.
- **Kilit Çekişmesi Dengesi:** Redis dağıtık kilidi birden çok sunucu arasında veri tutarlılığını garantiledi; ancak tek bir anahtar üzerindeki aşırı yarış, veritabanının kendi satır kilidine kıyasla ağ gecikmesi nedeniyle throughput düşüşüne yol açtı.

### 🚀 Projeyi Çalıştırma

```bash
# 1. Altyapıyı Başlat
docker compose up -d

# 2. Testleri Koştur
./mvnw clean test

# 3. Uygulamayı Başlat
./mvnw spring-boot:run

# 4. k6 Yük Testini Çalıştır
k6 run load-tests/benchmark.js
```

---

## 🇬🇧 English

### 📌 Overview
During high-demand flash sales, thousands of users attempt to purchase limited tickets simultaneously. Without proper concurrency controls, race conditions lead to overselling. This project implements, benchmarks, and compares 5 different architectural patterns to solve this problem:

1. **Naive Flow:** Unsynchronized baseline (guaranteed over-selling for verification).
2. **Pessimistic Locking:** Database row-level locks via PostgreSQL `SELECT ... FOR UPDATE`.
3. **Optimistic Locking:** Version-based atomic commits via JPA `@Version`.
4. **Redis Distributed Lock:** Node-agnostic distributed synchronization using Redisson `RLock`.
5. **RabbitMQ Asynchronous Queue:** Event-driven load leveling with message queues and Dead Letter Queue (DLQ).

### 🛠 Tech Stack
- **Backend:** Java 21, Spring Boot 4.1.1, Spring Data JPA
- **Database:** PostgreSQL 16
- **Distributed Cache & Lock:** Redis 7, Redisson
- **Messaging:** RabbitMQ 3 (AMQP 0-9-1)
- **Containerization:** Docker, Docker Compose
- **Load Testing:** Grafana k6

### 📊 Load Testing & Concurrency Benchmarks (k6)
Simulated flash-sale conditions with **100 Concurrent Virtual Users (VUs) for 30 seconds** against an inventory of **1,000 tickets**:

| Concurrency Strategy | Handling Model | Total Requests (30s) | Throughput (RPS) | Avg Latency | p95 Latency | Over-Selling |
|---|---|---|---|---|---|---|
| **Pessimistic Lock (`SELECT FOR UPDATE`)** | Synchronous | 12,876 | ~427 req/s | 182 ms | 650 ms | **0** (1,000 Sold) |
| **Redis Distributed Lock (`Redisson RLock`)** | Synchronous | 3,582 | ~117 req/s | 795 ms | 2,250 ms | **0** (1,000 Sold) |
| **RabbitMQ Asynchronous Queue (DLQ)** | Event-Driven | **43,659** | **~1,452 req/s** | **16.7 ms** | **56.6 ms** | **0** (1,000 Sold) |

#### 🔍 Key Takeaways
- **Event-Driven Resilience:** RabbitMQ decoupled the HTTP ingestion layer from the database persistence bottleneck, delivering an **11x lower p95 latency** and absorbing spikes with zero dropouts.
- **Lock Contention Tradeoff:** Redis distributed locks guaranteed consistency across distributed nodes, but high contention on a single key caused round-trip overhead compared to in-engine database row locks.

### 🚀 Running the Project

```bash
# 1. Start Infrastructure
docker compose up -d

# 2. Run Tests
./mvnw clean test

# 3. Start Application
./mvnw spring-boot:run

# 4. Run k6 Load Test
k6 run load-tests/benchmark.js
```