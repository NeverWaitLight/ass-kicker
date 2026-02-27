#!/usr/bin/env python3
"""
压测脚本 - 测试 /api/send 直接发送接口的核心任务处理 TPS

依赖安装:
  pip install aiohttp

使用示例:
  python scripts/bench.py \
    --token <access_token> \
    --template-code welcome \
    --channel-id <channel_id> \
    --recipients user@example.com \
    --concurrency 50 \
    --duration 30

参数说明:
  --url           后端地址，默认 http://localhost:8080
  --token         用户授权 Bearer token（必填，/api/send 需登录）
  --template-code 模板编码（必填）
  --channel-id    通道ID（必填）
  --recipients    收件人，多个用逗号分隔（必填）
  --language      语言代码，默认 ZH_HANS
  --params        模板参数 JSON 字符串，默认 {}
  --concurrency   并发协程数，默认 50
  --duration      压测持续时间（秒），默认 30
"""

import argparse
import asyncio
import json
import sys
import time
from collections import deque

try:
    import aiohttp
except ImportError:
    print("缺少依赖，请先安装: pip install aiohttp")
    sys.exit(1)


def parse_args():
    parser = argparse.ArgumentParser(description="AssKicker 核心任务处理 TPS 压测")
    parser.add_argument("--url", default="http://localhost:8080", help="后端地址")
    parser.add_argument("--token", required=True, help="用户授权 Bearer token，/api/send 需登录")
    parser.add_argument("--template-code", required=True, help="模板编码")
    parser.add_argument("--channel-id", required=True, help="通道ID")
    parser.add_argument("--recipients", required=True, help="收件人，多个用逗号分隔")
    parser.add_argument("--language", default="ZH_HANS", help="语言代码，默认 ZH_HANS")
    parser.add_argument("--params", default="{}", help="模板参数 JSON 字符串")
    parser.add_argument("--concurrency", type=int, default=50, help="并发协程数，默认 50")
    parser.add_argument("--duration", type=int, default=30, help="压测持续时间（秒），默认 30")
    return parser.parse_args()


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
            now = time.monotonic()
            self.tps_samples.append((now, 1))

    def current_tps(self, window_sec: float = 1.0) -> float:
        now = time.monotonic()
        cutoff = now - window_sec
        count = sum(1 for t, _ in self.tps_samples if t >= cutoff)
        return count / window_sec

    def percentile(self, p: float) -> float:
        if not self.latencies:
            return 0.0
        sorted_lat = sorted(self.latencies)
        idx = int(len(sorted_lat) * p / 100)
        idx = min(idx, len(sorted_lat) - 1)
        return sorted_lat[idx]

    def avg_latency(self) -> float:
        if not self.latencies:
            return 0.0
        return sum(self.latencies) / len(self.latencies)

    def avg_tps(self, duration_sec: float) -> float:
        if duration_sec <= 0:
            return 0.0
        return self.success / duration_sec


async def worker(
    session: aiohttp.ClientSession,
    endpoint: str,
    payload: dict,
    headers: dict,
    stats: BenchStats,
    stop_event: asyncio.Event,
):
    while not stop_event.is_set():
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


async def run(args):
    endpoint = f"{args.url.rstrip('/')}/api/send"
    recipients = [r.strip() for r in args.recipients.split(",") if r.strip()]
    try:
        params = json.loads(args.params)
    except json.JSONDecodeError as e:
        print(f"--params JSON 格式错误: {e}")
        sys.exit(1)

    payload = {
        "templateCode": args.template_code,
        "language": args.language,
        "params": params,
        "channelId": args.channel_id,
        "recipients": recipients,
    }

    print(f"压测目标 (send): {endpoint}")
    print(f"并发数:   {args.concurrency}")
    print(f"持续时间: {args.duration}s")
    print(f"请求体:   {json.dumps(payload, ensure_ascii=False)}")
    print("-" * 70)

    stats = BenchStats()
    stop_event = asyncio.Event()
    headers = {"Authorization": f"Bearer {args.token}"}

    connector = aiohttp.TCPConnector(limit=args.concurrency + 10)
    timeout = aiohttp.ClientTimeout(total=60)

    async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
        workers = [
            asyncio.create_task(worker(session, endpoint, payload, headers, stats, stop_event))
            for _ in range(args.concurrency)
        ]
        reporter_task = asyncio.create_task(
            reporter(stats, args.duration, stop_event)
        )

        start = time.monotonic()
        await asyncio.sleep(args.duration)
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
    print(f"延迟统计 (ms):")
    print(f"  平均:  {stats.avg_latency():.1f}")
    print(f"  P50:   {stats.percentile(50):.1f}")
    print(f"  P95:   {stats.percentile(95):.1f}")
    print(f"  P99:   {stats.percentile(99):.1f}")
    print(f"  最大:  {max(stats.latencies, default=0):.1f}")
    print("=" * 70)


def main():
    args = parse_args()
    asyncio.run(run(args))


if __name__ == "__main__":
    main()
