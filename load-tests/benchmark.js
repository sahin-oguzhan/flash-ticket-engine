import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const successfulPurchases = new Counter('successful_purchases');
const stockExhaustedErrors = new Counter('stock_exhausted_errors');
const lockTimeoutErrors = new Counter('lock_timeout_errors');
const otherErrors = new Counter('other_errors');
const errorRate = new Rate('error_rate');
const purchaseLatency = new Trend('purchase_latency');


export const options = {
    scenarios: {
        flash_sale_spike: {
            executor: "constant-vus",
            vus: 100,
            duration: "30s",
        },
    },
    thresholds: {
        http_req_failed: ["rate<0.80"],
        http_req_duration: ["p(95)<2000"],
    },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080/api/v1/tickets";
const TICKET_ID = __ENV.TICKET_ID || "1";
const TEST_TYPE = __ENV.TEST_TYPE || "pessimistic";

export default function () {
    const url = `${BASE_URL}/${TICKET_ID}/buy-${TEST_TYPE}`;

    const payload = JSON.stringify({
        userId: `user-vu-${__VU}-${__ITER}`,
        quantity: 1,
    });

    const params = {
        headers: {
            "Content-Type": "application/json",
        },
    };

    const startTime = Date.now();
    const res = http.post(url, payload, params);
    purchaseLatency.add(Date.now() - startTime);

    if (res.status === 201 || res.status === 202) {
        successfulPurchases.add(1);
        errorRate.add(0);
        check(res, {
            "status is success (201 or 202)": (r) => r.status === 201 || r.status === 202,
        });
    } else if (res.status === 409) {
        stockExhaustedErrors.add(1);
        errorRate.add(0);
    } else if (res.status === 429) {
        lockTimeoutErrors.add(1);
        errorRate.add(1);
    } else {
        otherErrors.add(1);
        errorRate.add(1);
        check(res, {
            "unexpected error": () => false,
        });
    }

    sleep(0.05);
}