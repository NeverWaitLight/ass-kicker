#!/usr/bin/env python3
"""
压测脚本 - 测试 /api/send 直接发送接口的核心任务处理 TPS

依赖安装:
  pip install aiohttp

使用示例:
  python scripts/bench.py
"""

import asyncio
import json
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
CONCURRENCY = 50
DURATION = 30

TEMPLATE_CODES = ["captcha-sms", "captcha-email", "captcha-im", "captcha-push"]

CHANNEL_TYPE_FOR_TEMPLATE = {
    "captcha-sms":   "SMS",
    "captcha-email": "EMAIL",
    "captcha-im":    "IM",
    "captcha-push":  "PUSH",
}
# ─────────────────────────────────────────────────────────────────────────────


def random_phone() -> str:
    prefixes = ["130", "131", "132", "133", "134", "135", "136", "137", "138", "139",
                "150", "151", "152", "153", "155", "156", "157", "158", "159",
                "170", "176", "177", "178",
                "180", "181", "182", "183", "184", "185", "186", "187", "188", "189"]
    return random.choice(prefixes) + "".join(random.choices(string.digits, k=8))


def random_email() -> str:
    user = "".join(random.choices(string.ascii_lowercase + string.digits, k=random.randint(5, 10)))
    domains = ["example.com", "test.com", "bench.io", "mail.dev"]
    return f"{user}@{random.choice(domains)}"


def random_im_id() -> str:
    return "im_" + "".join(random.choices(string.ascii_lowercase + string.digits, k=12))


def random_push_token() -> str:
    return "push_" + "".join(random.choices(string.hexdigits.lower(), k=32))


RECIPIENT_GENERATORS = {
    "captcha-sms":   random_phone,
    "captcha-email": random_email,
    "captcha-im":    random_im_id,
    "captcha-push":  random_push_token,
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
    """返回 {ChannelType -> channelId} 映射，每种类型取第一个可用通道。"""
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
    """为 4 个模板各预建一个 payload 模板（recipient 在发送时动态生成）。"""
    payloads = []
    for code in TEMPLATE_CODES:
        ch_type = CHANNEL_TYPE_FOR_TEMPLATE[code]
        payloads.append({
            "templateCode": code,
            "language": LANGUAGE,
            "params": {"captcha": "123456"},
            "channelId": type_to_id[ch_type],
            "_recipient_gen": RECIPIENT_GENERATORS[code],
        })
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


async def reporter(stats: BenchStats, duration: int, stop_event: asyncio.Event):
    start = time.monotonic()
    while not stop_event.is_set():
        await asyncio.sleep(1)
        elapsed = time.monotonic() - start
        remaining = max(0, duration - elapsed)
        tps = stats.current_tps()
        print(
            f"\r进度 {elapsed:5.1f}s / {duration}s  |  "
            f"完成 {stats.total:6d}  成功 {stats.success:6d}  失败 {stats.failed:4d}  |  "
            f"实时TPS {tps:7.1f}  剩余 {remaining:4.0f}s",
            end="",
            flush=True,
        )


async def run():
    connector = aiohttp.TCPConnector(limit=CONCURRENCY + 10)
    timeout = aiohttp.ClientTimeout(total=60)

    async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
        print("正在登录...")
        token = await login(session)
        print(f"登录成功，正在获取通道列表...")

        type_to_id = await fetch_channels(session, token)
        print("通道映射:")
        for t, cid in type_to_id.items():
            print(f"  {t}: {cid}")

        payload_templates = build_payloads(type_to_id)
        endpoint = f"{BASE_URL}/api/send"
        headers = {"Authorization": f"Bearer {token}"}

        print()
        print(f"压测目标:  {endpoint}")
        print(f"模板列表:  {TEMPLATE_CODES}")
        print(f"语言:      {LANGUAGE}")
        print(f"并发数:    {CONCURRENCY}")
        print(f"持续时间:  {DURATION}s")
        print("-" * 70)

        stats = BenchStats()
        stop_event = asyncio.Event()

        workers = [
            asyncio.create_task(
                worker(session, endpoint, payload_templates, headers, stats, stop_event)
            )
            for _ in range(CONCURRENCY)
        ]
        reporter_task = asyncio.create_task(reporter(stats, DURATION, stop_event))

        start = time.monotonic()
        await asyncio.sleep(DURATION)
        actual_duration = time.monotonic() - start

        stop_event.set()
        await asyncio.gather(*workers, return_exceptions=True)
        reporter_task.cancel()

    print()
    print("=" * 70)
    print("压测结果")
    print("=" * 70)
    print(f"持续时间:   {actual_duration:.2f}s")
    print(f"总请求数:   {stats.total}")
    print(f"成功数:     {stats.success}")
    print(f"失败数:     {stats.failed}")
    if stats.total > 0:
        print(f"成功率:     {stats.success / stats.total * 100:.2f}%")
    print()
    print(f"平均 TPS:   {stats.avg_tps(actual_duration):.2f} 任务/秒")
    print()
    print("延迟统计 (ms):")
    print(f"  平均:  {stats.avg_latency():.1f}")
    print(f"  P50:   {stats.percentile(50):.1f}")
    print(f"  P95:   {stats.percentile(95):.1f}")
    print(f"  P99:   {stats.percentile(99):.1f}")
    print(f"  最大:  {max(stats.latencies, default=0):.1f}")
    print("=" * 70)


def main():
    asyncio.run(run())


if __name__ == "__main__":
    main()
