#!/usr/bin/env python3
"""
渐进式压测脚本 - 自动递增并发数直到服务器扛不住

依赖安装:
  pip install aiohttp

使用示例:
  python scripts/bench.py
"""

import asyncio
import random
import string
import sys
import time
from collections import deque

try:
    import aiohttp
except ImportError:
    print("缺少依赖，请先安装: pip install aiohttp")
    sys.exit(1)

# ── 默认配置 ──────────────────────────────────────────────────────────────────
BASE_URL = "http://localhost:8080"
USERNAME = "admin"
PASSWORD = "123456"
LANGUAGE = "ZH_HANS"

INITIAL_CONCURRENCY = 1000  # 起始并发数
ROUND_DURATION = 30  # 每轮压测持续时间(秒)
PAUSE_BETWEEN_ROUNDS = 10  # 轮次间暂停时间(秒)
CONCURRENCY_STEP_RATIO = 1.25  # 每轮并发递增比例,下一轮并发=上一轮TPS*125%
TPS_THRESHOLD_RATIO = 0.8  # TPS低于并发数80%时停止压测

TEMPLATE_CODES = ["captcha-sms", "captcha-email", "captcha-im", "captcha-push"]

CHANNEL_TYPE_FOR_TEMPLATE = {
    "captcha-sms": "SMS",
    "captcha-email": "EMAIL",
    "captcha-im": "IM",
    "captcha-push": "PUSH",
}
# ─────────────────────────────────────────────────────────────────────────────


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


async def login(session: aiohttp.ClientSession) -> str:
    url = f"{BASE_URL}/api/auth/login"
    payload = {"username": USERNAME, "password": PASSWORD}
    async with session.post(url, json=payload) as resp:
        if resp.status != 200:
            body = await resp.text()
            print(f"登录失败 (HTTP {resp.status}): {body}")
            sys.exit(1)
        data = await resp.json()
        token = data.get("accessToken") or data.get("access_token")
        if not token:
            print(f"登录响应中未找到 accessToken: {data}")
            sys.exit(1)
        return token


async def fetch_channels(session: aiohttp.ClientSession, token: str) -> dict[str, str]:
    url = f"{BASE_URL}/api/channels"
    headers = {"Authorization": f"Bearer {token}"}
    async with session.get(url, headers=headers) as resp:
        if resp.status != 200:
            body = await resp.text()
            print(f"获取通道列表失败 (HTTP {resp.status}): {body}")
            sys.exit(1)
        channels = await resp.json()

    type_to_id: dict[str, str] = {}
    for ch in channels:
        ch_type = ch.get("type", "").upper()
        if ch_type and ch_type not in type_to_id:
            type_to_id[ch_type] = ch.get("id") or ch.get("_id") or ""

    missing = [
        CHANNEL_TYPE_FOR_TEMPLATE[t]
        for t in TEMPLATE_CODES
        if CHANNEL_TYPE_FOR_TEMPLATE[t] not in type_to_id
    ]
    if missing:
        print(f"以下通道类型在系统中未找到: {missing}")
        print("请先在管理界面创建对应类型的通道")
        sys.exit(1)

    return type_to_id


class BenchStats:
    def __init__(self):
        self.total = 0
        self.success = 0
        self.failed = 0
        self.latencies: list[float] = []
        self.tps_samples: deque[tuple[float, int]] = deque()
        self._lock = asyncio.Lock()

    async def record(self, success: bool, latency_ms: float):
        async with self._lock:
            self.total += 1
            if success:
                self.success += 1
            else:
                self.failed += 1
            self.latencies.append(latency_ms)
            self.tps_samples.append((time.monotonic(), 1))

    def current_tps(self, window_sec: float = 1.0) -> float:
        now = time.monotonic()
        cutoff = now - window_sec
        count = sum(1 for t, _ in self.tps_samples if t >= cutoff)
        return count / window_sec

    def percentile(self, p: float) -> float:
        if not self.latencies:
            return 0.0
        sorted_lat = sorted(self.latencies)
        idx = min(int(len(sorted_lat) * p / 100), len(sorted_lat) - 1)
        return sorted_lat[idx]

    def avg_latency(self) -> float:
        return sum(self.latencies) / len(self.latencies) if self.latencies else 0.0

    def avg_tps(self, duration_sec: float) -> float:
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
        latency_ms = (time.monotonic() - t0) * 1000
        await stats.record(success, latency_ms)


async def reporter(
    stats: BenchStats,
    concurrency: int,
    round_num: int,
    duration: int,
    stop_event: asyncio.Event,
):
    start = time.monotonic()
    while not stop_event.is_set():
        await asyncio.sleep(1)
        elapsed = time.monotonic() - start
        remaining = max(0, duration - elapsed)
        tps = stats.current_tps()
        print(
            f"\r  [第{round_num}轮 | 并发{concurrency}]  "
            f"{elapsed:5.1f}s / {duration}s  |  "
            f"完成 {stats.total:6d}  成功 {stats.success:6d}  失败 {stats.failed:4d}  |  "
            f"实时TPS {tps:7.1f}  剩余 {remaining:4.0f}s",
            end="",
            flush=True,
        )


def format_round_result(
    round_num: int, concurrency: int, stats: BenchStats, duration: float
) -> dict:
    avg_tps = stats.avg_tps(duration)
    return {
        "round": round_num,
        "concurrency": concurrency,
        "duration": duration,
        "total": stats.total,
        "success": stats.success,
        "failed": stats.failed,
        "success_rate": stats.success / stats.total * 100 if stats.total > 0 else 0,
        "avg_tps": avg_tps,
        "avg_latency": stats.avg_latency(),
        "p50": stats.percentile(50),
        "p95": stats.percentile(95),
        "p99": stats.percentile(99),
        "max_latency": max(stats.latencies, default=0),
    }


def print_round_summary(result: dict):
    print()
    print(
        f"  -- 第{result['round']}轮小结 --  "
        f"并发 {result['concurrency']}  "
        f"TPS {result['avg_tps']:.1f}  "
        f"成功率 {result['success_rate']:.1f}%  "
        f"P95 {result['p95']:.1f}ms"
    )


def print_final_report(results: list[dict], final: dict, stop_reason: str):
    print()
    print("=" * 78)
    print("  渐进式压测报告")
    print("=" * 78)
    print()
    print(f"  停止原因:  {stop_reason}")
    print()

    print(
        "  ┌────────┬────────┬──────────┬──────────┬──────────┬──────────┬──────────┐"
    )
    print(
        "  │  轮次  │  并发  │   TPS    │  成功率  │  P50(ms) │  P95(ms) │  P99(ms) │"
    )
    print(
        "  ├────────┼────────┼──────────┼──────────┼──────────┼──────────┼──────────┤"
    )
    for r in results:
        print(
            f"  │ {r['round']:^6} │ {r['concurrency']:^6} │ {r['avg_tps']:>8.1f} │ "
            f"{r['success_rate']:>7.1f}% │ {r['p50']:>8.1f} │ {r['p95']:>8.1f} │ {r['p99']:>8.1f} │"
        )
    print(
        "  └────────┴────────┴──────────┴──────────┴──────────┴──────────┴──────────┘"
    )

    print()
    print("-" * 78)
    print("  最终轮次详情")
    print("-" * 78)
    print(f"  轮次:       第{final['round']}轮")
    print(f"  并发数:     {final['concurrency']}")
    print(f"  持续时间:   {final['duration']:.2f}s")
    print(f"  总请求数:   {final['total']}")
    print(f"  成功数:     {final['success']}")
    print(f"  失败数:     {final['failed']}")
    print(f"  成功率:     {final['success_rate']:.2f}%")
    print(f"  平均 TPS:   {final['avg_tps']:.2f}")
    print()
    print(f"  延迟统计 (ms):")
    print(f"    平均:  {final['avg_latency']:.1f}")
    print(f"    P50:   {final['p50']:.1f}")
    print(f"    P95:   {final['p95']:.1f}")
    print(f"    P99:   {final['p99']:.1f}")
    print(f"    最大:  {final['max_latency']:.1f}")
    print("=" * 78)


async def run_round(
    session: aiohttp.ClientSession,
    endpoint: str,
    payload_templates: list[dict],
    headers: dict,
    concurrency: int,
    duration: int,
    round_num: int,
) -> dict:
    connector_limit = concurrency + 50
    session._connector._limit = connector_limit

    stats = BenchStats()
    stop_event = asyncio.Event()

    workers = [
        asyncio.create_task(
            worker(session, endpoint, payload_templates, headers, stats, stop_event)
        )
        for _ in range(concurrency)
    ]
    reporter_task = asyncio.create_task(
        reporter(stats, concurrency, round_num, duration, stop_event)
    )

    start = time.monotonic()
    await asyncio.sleep(duration)
    actual_duration = time.monotonic() - start

    stop_event.set()
    await asyncio.gather(*workers, return_exceptions=True)
    reporter_task.cancel()

    return format_round_result(round_num, concurrency, stats, actual_duration)


async def run():
    max_conn = 2000
    connector = aiohttp.TCPConnector(limit=max_conn)
    timeout = aiohttp.ClientTimeout(total=60)

    async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
        print("正在登录...")
        token = await login(session)
        print("登录成功，正在获取通道列表...")

        type_to_id = await fetch_channels(session, token)
        print("通道映射:")
        for t, cid in type_to_id.items():
            print(f"  {t}: {cid}")

        payload_templates = build_payloads(type_to_id)
        endpoint = f"{BASE_URL}/api/send"
        headers = {"Authorization": f"Bearer {token}"}

        print()
        print(f"压测目标:      {endpoint}")
        print(f"模板列表:      {TEMPLATE_CODES}")
        print(f"语言:          {LANGUAGE}")
        print(f"起始并发:      {INITIAL_CONCURRENCY}")
        print(f"每轮持续:      {ROUND_DURATION}s")
        print(f"轮间暂停:      {PAUSE_BETWEEN_ROUNDS}s")
        print(f"递增策略:      下一轮并发 = 上一轮TPS x {CONCURRENCY_STEP_RATIO:.0%}")
        print(f"TPS 停止阈值:  并发数 x {TPS_THRESHOLD_RATIO:.0%}")
        print("=" * 78)

        concurrency = INITIAL_CONCURRENCY
        round_num = 0
        results: list[dict] = []
        stop_reason = ""

        while True:
            round_num += 1
            print()
            print(
                f">>> 第{round_num}轮开始  并发数 {concurrency}  持续 {ROUND_DURATION}s"
            )

            result = await run_round(
                session,
                endpoint,
                payload_templates,
                headers,
                concurrency,
                ROUND_DURATION,
                round_num,
            )
            results.append(result)
            print_round_summary(result)

            # 检查是否达到停止条件：TPS < 并发数 * 80%
            tps_threshold = concurrency * TPS_THRESHOLD_RATIO
            if result["avg_tps"] < tps_threshold:
                stop_reason = (
                    f"第{round_num}轮 TPS({result['avg_tps']:.1f}) "
                    f"< 阈值({tps_threshold:.1f} = 并发{concurrency} x {TPS_THRESHOLD_RATIO:.0%})"
                )
                print()
                print(f">>> 触发停止条件: {stop_reason}")
                break

            # 下一轮并发数 = 本轮平均TPS * 125%
            next_concurrency = int(result["avg_tps"] * CONCURRENCY_STEP_RATIO)
            print(
                f"\n>>> 服务器扛住了(TPS {result['avg_tps']:.1f}) "
                f"暂停{PAUSE_BETWEEN_ROUNDS}s后提升并发 {concurrency} -> {next_concurrency}"
            )
            await asyncio.sleep(PAUSE_BETWEEN_ROUNDS)
            concurrency = next_concurrency

        print_final_report(results, results[-1], stop_reason)


def main():
    asyncio.run(run())


if __name__ == "__main__":
    main()
