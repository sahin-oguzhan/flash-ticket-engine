import { useState } from 'react';
import { Play, Zap, ShieldAlert, Clock, Database, Layers } from 'lucide-react';

interface Stats {
    total: number;
    success: number;
    accepted: number;
    failed: number;
    avgLatency: number;
    minLatency: number;
    maxLatency: number;
    totalDuration?: number;
}

interface LogEntry {
    id: number;
    status: number;
    duration: number;
    correlationId?: string;
    error?: string;
}

type SimulationMode = 'sync' | 'async';

export default function App() {
    const [isRunning, setIsRunning] = useState<boolean>(false);
    const [requestCount, setRequestCount] = useState<number>(100);
    const [ticketId, setTicketId] = useState<number>(1);
    const [, setActiveTab] = useState<SimulationMode>('async');
    const [stats, setStats] = useState<Stats>({
        total: 0,
        success: 0,
        accepted: 0,
        failed: 0,
        avgLatency: 0,
        minLatency: Infinity,
        maxLatency: 0,
    });
    const [logs, setLogs] = useState<LogEntry[]>([]);

    const runSimulation = async (mode: SimulationMode) => {
        setIsRunning(true);
        setActiveTab(mode);
        setLogs([]);

        const startTime = performance.now();

        // Doğru rotalar: /api/v1/tickets/{ticketId}/...
        const endpoint =
            mode === 'async'
                ? `http://localhost:8080/api/v1/tickets/${ticketId}/buy-async`
                : `http://localhost:8080/api/v1/tickets/${ticketId}/buy-pessimistic`;

        let successCount = 0;
        let acceptedCount = 0;
        let failedCount = 0;
        const latencies: number[] = [];

        const requests = Array.from({ length: requestCount }, async (_, index) => {
            const reqStart = performance.now();
            const idempotencyKey = `sim-${Date.now()}-${index + 1}`;

            try {
                const response = await fetch(endpoint, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Idempotency-Key': idempotencyKey
                    },
                    body: JSON.stringify({
                        userId: `user-${index + 1}`,
                        quantity: 1
                    }),
                });

                const reqEnd = performance.now();
                const duration = Math.round(reqEnd - reqStart);
                latencies.push(duration);

                // 200 veya 201 Created -> Başarılı Senkron
                if (response.status === 200 || response.status === 201) {
                    successCount++;
                } else if (response.status === 202) {
                    acceptedCount++;
                } else {
                    failedCount++;
                }

                const logEntry: LogEntry = {
                    id: index + 1,
                    status: response.status,
                    duration,
                    correlationId: idempotencyKey,
                };

                setLogs((prev: LogEntry[]) => [logEntry, ...prev.slice(0, 99)]);
            } catch {
                failedCount++;
                latencies.push(Math.round(performance.now() - reqStart));
                setLogs((prev: LogEntry[]) => [
                    { id: index + 1, status: 500, duration: 0, error: 'Connection Refused' },
                    ...prev.slice(0, 99),
                ]);
            }
        });

        await Promise.all(requests);

        const totalDuration = Math.round(performance.now() - startTime);
        const avg = latencies.length
            ? Math.round(latencies.reduce((a, b) => a + b, 0) / latencies.length)
            : 0;

        setStats({
            total: requestCount,
            success: successCount,
            accepted: acceptedCount,
            failed: failedCount,
            avgLatency: avg,
            minLatency: latencies.length ? Math.min(...latencies) : 0,
            maxLatency: latencies.length ? Math.max(...latencies) : 0,
            totalDuration,
        });

        setIsRunning(false);
    };

    return (
        <div
            style={{
                backgroundColor: '#0f172a',
                minHeight: '100vh',
                color: '#f8fafc',
                fontFamily: 'system-ui, sans-serif',
                padding: '32px',
            }}
        >
            <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
                {/* Header */}
                <div
                    style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        marginBottom: '32px',
                        borderBottom: '1px solid #1e293b',
                        paddingBottom: '20px',
                    }}
                >
                    <div>
                        <h1
                            style={{
                                fontSize: '28px',
                                fontWeight: 'bold',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '12px',
                                margin: 0,
                            }}
                        >
                            <Zap color="#38bdf8" fill="#38bdf8" size={32} />
                            Flash Ticket Engine - Stress Playground
                        </h1>
                        <p style={{ color: '#94a3b8', margin: '6px 0 0 0', fontSize: '14px' }}>
                            Synchronous DB Lock Contention vs Asynchronous Redisson &amp; RabbitMQ Pipeline
                        </p>
                    </div>
                    <div style={{ display: 'flex', gap: '12px' }}>
            <span
                style={{
                    backgroundColor: '#1e293b',
                    padding: '6px 14px',
                    borderRadius: '20px',
                    fontSize: '13px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                }}
            >
              <Database size={16} color="#38bdf8" /> PostgreSQL 16
            </span>
                        <span
                            style={{
                                backgroundColor: '#1e293b',
                                padding: '6px 14px',
                                borderRadius: '20px',
                                fontSize: '13px',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '6px',
                            }}
                        >
              <Layers size={16} color="#f43f5e" /> Redis &amp; Redisson
            </span>
                    </div>
                </div>

                {/* Control Box */}
                <div
                    style={{
                        backgroundColor: '#1e293b',
                        padding: '24px',
                        borderRadius: '12px',
                        marginBottom: '32px',
                        display: 'flex',
                        flexWrap: 'wrap',
                        gap: '20px',
                        alignItems: 'center',
                    }}
                >
                    <div>
                        <label style={{ display: 'block', fontSize: '13px', color: '#94a3b8', marginBottom: '6px' }}>
                            Ticket ID
                        </label>
                        <input
                            type="number"
                            value={ticketId}
                            onChange={(e) => setTicketId(Number(e.target.value))}
                            style={{
                                backgroundColor: '#0f172a',
                                border: '1px solid #334155',
                                borderRadius: '8px',
                                padding: '10px 14px',
                                color: '#fff',
                                width: '100px',
                            }}
                        />
                    </div>

                    <div>
                        <label style={{ display: 'block', fontSize: '13px', color: '#94a3b8', marginBottom: '6px' }}>
                            Concurrent Requests
                        </label>
                        <input
                            type="number"
                            value={requestCount}
                            onChange={(e) => setRequestCount(Number(e.target.value))}
                            style={{
                                backgroundColor: '#0f172a',
                                border: '1px solid #334155',
                                borderRadius: '8px',
                                padding: '10px 14px',
                                color: '#fff',
                                width: '120px',
                            }}
                        />
                    </div>

                    <div style={{ display: 'flex', gap: '12px', marginTop: '22px' }}>
                        <button
                            onClick={() => runSimulation('sync')}
                            disabled={isRunning}
                            style={{
                                backgroundColor: isRunning ? '#334155' : '#e11d48',
                                color: '#fff',
                                padding: '12px 24px',
                                borderRadius: '8px',
                                border: 'none',
                                fontWeight: '600',
                                cursor: isRunning ? 'not-allowed' : 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '8px',
                            }}
                        >
                            <ShieldAlert size={18} />
                            Simulate Synchronous (Lock Contention)
                        </button>

                        <button
                            onClick={() => runSimulation('async')}
                            disabled={isRunning}
                            style={{
                                backgroundColor: isRunning ? '#334155' : '#0284c7',
                                color: '#fff',
                                padding: '12px 24px',
                                borderRadius: '8px',
                                border: 'none',
                                fontWeight: '600',
                                cursor: isRunning ? 'not-allowed' : 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '8px',
                            }}
                        >
                            <Play size={18} />
                            Simulate Asynchronous (Queue + Redisson)
                        </button>
                    </div>
                </div>

                {/* Metrics Grid */}
                <div
                    style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                        gap: '16px',
                        marginBottom: '32px',
                    }}
                >
                    <div style={{ backgroundColor: '#1e293b', padding: '20px', borderRadius: '12px' }}>
                        <span style={{ fontSize: '13px', color: '#94a3b8' }}>Total Simulated Requests</span>
                        <div style={{ fontSize: '26px', fontWeight: 'bold', marginTop: '6px' }}>{stats.total}</div>
                    </div>

                    <div style={{ backgroundColor: '#1e293b', padding: '20px', borderRadius: '12px' }}>
                        <span style={{ fontSize: '13px', color: '#94a3b8' }}>Accepted (202 Queued)</span>
                        <div style={{ fontSize: '26px', fontWeight: 'bold', marginTop: '6px', color: '#38bdf8' }}>
                            {stats.accepted}
                        </div>
                    </div>

                    <div style={{ backgroundColor: '#1e293b', padding: '20px', borderRadius: '12px' }}>
                        <span style={{ fontSize: '13px', color: '#94a3b8' }}>Created (201 Sync OK)</span>
                        <div style={{ fontSize: '26px', fontWeight: 'bold', marginTop: '6px', color: '#4ade80' }}>
                            {stats.success}
                        </div>
                    </div>

                    <div style={{ backgroundColor: '#1e293b', padding: '20px', borderRadius: '12px' }}>
                        <span style={{ fontSize: '13px', color: '#94a3b8' }}>Average Response Time</span>
                        <div
                            style={{
                                fontSize: '26px',
                                fontWeight: 'bold',
                                marginTop: '6px',
                                color: stats.avgLatency > 150 ? '#f43f5e' : '#facc15',
                            }}
                        >
                            {stats.avgLatency} ms
                        </div>
                    </div>

                    <div style={{ backgroundColor: '#1e293b', padding: '20px', borderRadius: '12px' }}>
                        <span style={{ fontSize: '13px', color: '#94a3b8' }}>Min / Max Latency</span>
                        <div style={{ fontSize: '20px', fontWeight: 'bold', marginTop: '8px' }}>
                            {stats.minLatency === Infinity ? 0 : stats.minLatency} / {stats.maxLatency} ms
                        </div>
                    </div>
                </div>

                {/* Live Stream Table */}
                <div style={{ backgroundColor: '#1e293b', borderRadius: '12px', padding: '20px' }}>
                    <h3
                        style={{
                            margin: '0 0 16px 0',
                            fontSize: '16px',
                            fontWeight: '600',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px',
                        }}
                    >
                        <Clock size={18} color="#94a3b8" /> Live Incoming Responses (Last 100)
                    </h3>
                    <div style={{ maxHeight: '350px', overflowY: 'auto' }}>
                        <table
                            style={{
                                width: '100%',
                                borderCollapse: 'collapse',
                                textAlign: 'left',
                                fontSize: '13px',
                            }}
                        >
                            <thead>
                            <tr style={{ color: '#94a3b8', borderBottom: '1px solid #334155' }}>
                                <th style={{ padding: '10px' }}>#</th>
                                <th style={{ padding: '10px' }}>Status Code</th>
                                <th style={{ padding: '10px' }}>Response Latency</th>
                                <th style={{ padding: '10px' }}>Idempotency / Tracking ID</th>
                            </tr>
                            </thead>
                            <tbody>
                            {logs.length === 0 ? (
                                <tr>
                                    <td colSpan={4} style={{ textAlign: 'center', padding: '24px', color: '#64748b' }}>
                                        Henüz simülasyon başlatılmadı. Yukarıdaki butonlardan birine tıklayın.
                                    </td>
                                </tr>
                            ) : (
                                logs.map((log: LogEntry) => (
                                    <tr key={log.id} style={{ borderBottom: '1px solid #1e293b' }}>
                                        <td style={{ padding: '8px 10px', color: '#64748b' }}>{log.id}</td>
                                        <td style={{ padding: '8px 10px' }}>
                        <span
                            style={{
                                backgroundColor:
                                    log.status === 202
                                        ? '#0369a1'
                                        : log.status === 201 || log.status === 200
                                            ? '#15803d'
                                            : '#be123c',
                                padding: '2px 8px',
                                borderRadius: '4px',
                                fontWeight: '600',
                                fontSize: '12px',
                            }}
                        >
                          {log.status} {log.status === 202 ? 'ACCEPTED' : log.status === 201 ? 'CREATED' : log.status === 200 ? 'OK' : 'FAIL'}
                        </span>
                                        </td>
                                        <td
                                            style={{
                                                padding: '8px 10px',
                                                color: log.duration > 150 ? '#f43f5e' : '#f8fafc',
                                            }}
                                        >
                                            {log.duration} ms
                                        </td>
                                        <td
                                            style={{
                                                padding: '8px 10px',
                                                fontFamily: 'monospace',
                                                color: '#94a3b8',
                                            }}
                                        >
                                            {log.correlationId || '-'}
                                        </td>
                                    </tr>
                                ))
                            )}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    );
}