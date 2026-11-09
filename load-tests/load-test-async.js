import http from 'k6/http';
import {check, sleep} from 'k6';

export const options = {
    scenarios: {
        async_ticket_burst: {
            executor: "constant-vus",
            vus: 50,
            duration: "30s",
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<100'],
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {
    const ticketId = 48;
    const url = `http://localhost:8080/api/v1/tickets/${ticketId}/buy-async;`

    const isDuplicate = Math.random() < 0.2;
    const correlationId = isDuplicate
    ? "fixed-duplicate-key-user-99"
        : `order-${__VU}-${__ITER}-${Date.now()}`;

    const payload = JSON.stringify({
        userId: `user-${__VU}`,
        quantity: 1,
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Idempotency-Key': correlationId,
        },
    };

    const res = http.post(url, payload, params);

    check(res, {
        'status is 200 or 202': (r) => r.status === 200 || r.status === 202,
    });

    sleep(0.05);
}
