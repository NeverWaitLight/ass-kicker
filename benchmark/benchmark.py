#!/usr/bin/env python3
"""
固定并发压测脚本

依赖安装:
  pip install aiohttp pymongo

使用示例:
  python scripts/benchmark.py
"""

import asyncio
import random
import string
import sys
import time

try:
    import aiohttp
except ImportError:
    print("缺少依赖，请先安装: pip install aiohttp")
    sys.exit(1)

try:
    from pymongo import MongoClient
except ImportError:
    print("缺少依赖，请先安装: pip install pymongo")
    sys.exit(1)

# ── 默认配置 ──────────────────────────────────────────────────────────────────
BASE_URL = "http://localhost:8080"
API_KEY = "ak_5f9fe09cb50f4327989106318e95b876"
LANGUAGE = "ZH_HANS"

# 与 backend application.yml 一致
MONGODB_URI = "mongodb://admin:123456@localhost:27017/asskicker?authSource=admin"
SENDRECORD_COLLECTION = "t_send_record"

TEST_CONCURRENCIES = [1000, 2000, 3000, 4000]
ROUND_DURATION = 20  # 每轮压测持续时间(秒)
PAUSE_BETWEEN_ROUNDS = 5  # 轮次间暂停时间(秒)
WARMUP_CONCURRENCY = 200  # 预热并发数
WARMUP_DURATION = 10  # 预热持续时间(秒)

TEMPLATE_CODES = ["captcha-sms", "captcha-email", "captcha-im", "captcha-push"]

CHANNEL_TYPE_FOR_TEMPLATE = {
    "captcha-sms": "SMS",
    "captcha-email": "EMAIL",
    "captcha-im": "IM",
    "captcha-push": "PUSH",
}
# ─────────────────────────────────────────────────────────────────────────────


def clear_send_record_table() -> int:
    """连接项目数据库，删除 t_send_record 表，返回被删除集合中原有的文档数。"""
    client = MongoClient(MONGODB_URI)
    try:
        db = client.get_default_database()
        coll = db[SENDRECORD_COLLECTION]
        count = coll.count_documents({})
        coll.drop()
        return count
    finally:
        client.close()


def random_phone() -> str:
    prefixes = [
        "130",
        "131",
        "132",
        "133",
        "134",
        "135",
        "136",
        "137",
        "138",
        "139",
        "150",
        "151",
        "152",
        "153",
        "155",
        "156",
        "157",
        "158",
        "159",
        "170",
        "176",
        "177",
        "178",
        "180",
        "181",
        "182",
        "183",
        "184",
        "185",
        "186",
        "187",
        "188",
        "189",
    ]
    return random.choice(prefixes) + "".join(random.choices(string.digits, k=8))


def random_email() -> str:
    user = "".join(
        random.choices(string.ascii_lowercase + string.digits, k=random.randint(5, 10))
    )
    domains = ["example.com", "test.com", "bench.io", "mail.dev"]
    return f"{user}@{random.choice(domains)}"


def random_im_id() -> str:
    return "im_" + "".join(random.choices(string.ascii_lowercase + string.digits, k=12))


def random_push_token() -> str:
    return "push_" + "".join(random.choices(string.hexdigits.lower(), k=32))


RECIPIENT_GENERATORS = {
    "captcha-sms": random_phone,
    "captcha-email": random_email,
    "captcha-im": random_im_id,
    "captcha-push": random_push_token,
}


class BenchStats:
    def __init__(self):
        self.total = 0
        self.success = 0
        self.failed = 0
        self.latencies: list[float] = []
        self._lock = asyncio.Lock()

    async def record(self, success: bool, latency_ms: float, finished_at: float, deadline: float):
        if finished_at > deadline:
            return
        async with self._lock:
            self.total += 1
            if success:
                self.success += 1
            else:
                self.failed += 1
            self.latencies.append(latency_ms)

    def percentile(self, p: float) -> float:
        if not self.latencies:
            return 0.0
        sorted_lat = sorted(self.latencies)
        idx = min(int(len(sorted_lat) * p / 100), len(sorted_lat) - 1)
        return sorted_lat[idx]

    def actual_tps(self, duration_sec: float) -> float:
        return self.success / duration_sec if duration_sec > 0 else 0.0


def build_payloads(type_to_id: dict[str, str]) -> list[dict]:
    payloads = []
    for code in TEMPLATE_CODES:
        ch_type = CHANNEL_TYPE_FOR_TEMPLATE[code]
        payloads.append(
            {
                "templateCode": code,
                "language": LANGUAGE,
                "params": {"captcha": "123456"},
                "channelId": type_to_id[ch_type],
                "_recipient_gen": RECIPIENT_GENERATORS[code],
            }
        )
    return payloads


async def worker(
    session: aiohttp.ClientSession,
    endpoint: str,
    payload_templates: list[dict],
    headers: dict,
    stats: BenchStats,
    stop_event: asyncio.Event,
    deadline: float,
):
    while not stop_event.is_set():
        tpl = random.choice(payload_templates)
        recipient = tpl["_recipient_gen"]()
        payload = {k: v for k, v in tpl.items() if not k.startswith("_")}
        payload["recipients"] = [recipient]

        t0 = time.monotonic()
        try:
            async with session.post(endpoint, json=payload, headers=headers) as resp:
                await resp.read()
                success = resp.status == 200
        except Exception:
            success = False
        finished_at = time.monotonic()
        latency_ms = (finished_at - t0) * 1000
        await stats.record(success, latency_ms, finished_at, deadline)


def format_round_result(concurrency: int, stats: BenchStats, duration: float) -> dict:
    actual_tps = stats.actual_tps(duration)
    return {
        "concurrency": concurrency,
        "actual_tps": actual_tps,
        "p50": stats.percentile(50),
        "p95": stats.percentile(95),
        "p99": stats.percentile(99),
    }


def print_result_table(results: list[dict]):
    print("┌────────┬────────────┬──────────┬──────────┬──────────┐")
    print("│  并发  │ 实际TPS    │  P50(ms) │  P95(ms) │  P99(ms) │")
    print("├────────┼────────────┼──────────┼──────────┼──────────┤")
    for result in results:
        print(
            f"│ {result['concurrency']:>6} │ {result['actual_tps']:>10.1f} │ "
            f"{result['p50']:>8.1f} │ {result['p95']:>8.1f} │ {result['p99']:>8.1f} │"
        )
    print("└────────┴────────────┴──────────┴──────────┴──────────┘")


async def run_round(
    session: aiohttp.ClientSession,
    endpoint: str,
    payload_templates: list[dict],
    headers: dict,
    concurrency: int,
    duration: int,
) -> dict:
    connector_limit = concurrency + 50
    session._connector._limit = connector_limit

    stats = BenchStats()
    stop_event = asyncio.Event()
    start = time.monotonic()
    deadline = start + duration

    workers = [
        asyncio.create_task(
            worker(session, endpoint, payload_templates, headers, stats, stop_event, deadline)
        )
        for _ in range(concurrency)
    ]

    await asyncio.sleep(duration)
    actual_duration = time.monotonic() - start

    stop_event.set()
    await asyncio.gather(*workers, return_exceptions=True)
    return format_round_result(concurrency, stats, actual_duration)


async def run():
    max_conn = max(TEST_CONCURRENCIES) + 50
    connector = aiohttp.TCPConnector(limit=max_conn)
    timeout = aiohttp.ClientTimeout(total=60)

    try:
        clear_send_record_table()
    except Exception as e:
        print(f"删除 sendrecord 表失败: {e}")
        sys.exit(1)

    async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
        type_to_id = {CHANNEL_TYPE_FOR_TEMPLATE[t]: "" for t in TEMPLATE_CODES}
        payload_templates = build_payloads(type_to_id)
        endpoint = f"{BASE_URL}/v1/send"
        headers = {"Authorization": f"Bearer {API_KEY}"}

        await run_round(
            session,
            endpoint,
            payload_templates,
            headers,
            WARMUP_CONCURRENCY,
            WARMUP_DURATION,
        )
        await asyncio.sleep(PAUSE_BETWEEN_ROUNDS)

        results: list[dict] = []
        for index, concurrency in enumerate(TEST_CONCURRENCIES):
            result = await run_round(
                session,
                endpoint,
                payload_templates,
                headers,
                concurrency,
                ROUND_DURATION,
            )
            results.append(result)
            if index < len(TEST_CONCURRENCIES) - 1:
                await asyncio.sleep(PAUSE_BETWEEN_ROUNDS)

        print_result_table(results)


def main():
    asyncio.run(run())


if __name__ == "__main__":
    main()
