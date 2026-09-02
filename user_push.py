#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""统计用户 0x0204 推送条数、当天重连次数。在 im-lc-server 目录执行:  ./user_push.py"""
from __future__ import print_function

import gzip
import os
import re
import sys
from collections import Counter
from datetime import date, datetime

USER = "364b00baa6a247c1b5837c41e8a88815"
CMD = "0x0204"
LOG_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "logs")
TODAY = date.today().isoformat()

CLIENT = {1: "IPHONE", 2: "IPAD", 257: "ANDROID", 513: "PC", 769: "WEB"}

LINE_RE = re.compile(
    r"^\[(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})\]\s+\w+\s+\[[^\]]*\]\s+\S+\s+-\s+(.*)$"
)
PUSH_RE = re.compile(
    r"^push signal cmd:\[([^\]]+)\] to user:\[([^\]]*)\]-\[([^\]]*)\], "
    r"clientTypes--(.*?)\. channel count:(null|\d+)"
)
BIND_RE = re.compile(
    r"^bind userId:(\S*) companyId:(\S*) clientType:(-?\d+) to channel:(\d+), localIP:(\S*)"
)
UNBIND_RE = re.compile(
    r"^user channel closed\. userId:(\S*) companyId:(\S*) clientType:(-?\d+) "
    r"channelId:(\d+) removeSuccess:(\w+)"
)
ERR_RE = re.compile(r"^pushData error,complexId:\[([^\]]*)\],cmd:\[([^\]]*)\]")


def ct_name(v):
    try:
        return CLIENT.get(int(v), "T%s" % v)
    except Exception:
        return str(v)


def norm_cmd(s):
    s = (s or "").strip()
    if s.lower().startswith("0x"):
        return "0x%04X" % int(s, 16)
    if s.isdigit():
        return "0x%04X" % int(s)
    return s.upper()


def open_log(path):
    if path.endswith(".gz"):
        if sys.version_info[0] >= 3:
            return gzip.open(path, "rt", encoding="utf-8", errors="replace")
        return gzip.open(path, "r")
    if sys.version_info[0] >= 3:
        return open(path, "r", encoding="utf-8", errors="replace")
    return open(path, "r")


def list_logs(d):
    if os.path.isfile(d):
        return [d]
    if not os.path.isdir(d):
        return []
    out = []
    for name in os.listdir(d):
        if name.endswith(".log") or name.endswith(".log.gz"):
            out.append(os.path.join(d, name))
    out.sort(key=lambda p: os.path.getmtime(p))
    return out


def main():
    user = USER
    cmd = norm_cmd(CMD)
    day = TODAY
    logdir = LOG_DIR
    argv = sys.argv[1:]
    i = 0
    while i < len(argv):
        a = argv[i]
        if a in ("-h", "--help"):
            print("用法: ./user_push.py [userId] [--cmd 0x0204] [--date YYYY-MM-DD] [--log logs目录]")
            return 0
        if a == "--cmd" and i + 1 < len(argv):
            cmd = norm_cmd(argv[i + 1]); i += 2; continue
        if a == "--date" and i + 1 < len(argv):
            day = argv[i + 1]; i += 2; continue
        if a == "--log" and i + 1 < len(argv):
            logdir = argv[i + 1]; i += 2; continue
        if not a.startswith("-"):
            user = a
        i += 1

    paths = list_logs(logdir)
    if not paths:
        sys.stderr.write("找不到日志: %s\n" % logdir)
        return 1

    want_day = datetime.strptime(day, "%Y-%m-%d").date()
    pushes = []
    binds = []
    unbinds = []
    errs = 0
    nline = 0
    first = last = None

    for path in paths:
        try:
            fh = open_log(path)
        except IOError as e:
            sys.stderr.write("读不了 %s: %s\n" % (path, e))
            continue
        try:
            for line in fh:
                nline += 1
                if user not in line:
                    continue
                m = LINE_RE.match(line.rstrip("\n"))
                if not m:
                    continue
                ts, msg = m.group(1), m.group(2)
                try:
                    if datetime.strptime(ts[:10], "%Y-%m-%d").date() != want_day:
                        continue
                except ValueError:
                    continue
                if first is None:
                    first = ts
                last = ts

                pm = PUSH_RE.match(msg)
                if pm and pm.group(2) == user:
                    if norm_cmd(pm.group(1)) != cmd:
                        continue
                    cnt = 0 if pm.group(5) == "null" else int(pm.group(5))
                    pushes.append((ts, cnt, pm.group(3), pm.group(4)))
                    continue
                em = ERR_RE.match(msg)
                if em and user in em.group(1):
                    errs += 1
                    continue
                bm = BIND_RE.match(msg)
                if bm and bm.group(1) == user:
                    binds.append((ts, bm.group(2), bm.group(3), bm.group(4), bm.group(5)))
                    continue
                um = UNBIND_RE.match(msg)
                if um and um.group(1) == user:
                    unbinds.append((ts, um.group(3), um.group(4), um.group(5)))
                    continue
        finally:
            fh.close()

    ok = [p for p in pushes if p[1] > 0]
    miss = [p for p in pushes if p[1] <= 0]
    reconnects = len(binds) - 1 if len(binds) > 0 else 0
    hour_ok = Counter(p[0][:13] for p in ok)
    hour_miss = Counter(p[0][:13] for p in miss)

    print("=" * 68)
    print("用户推送 / 重连分析报告")
    print("=" * 68)
    print("用户     : %s" % user)
    print("信令     : %s  (NEW_MSGSIG)" % cmd)
    print("日期     : %s" % day)
    print("日志     : %s  (%d 个文件, %d 行)" % (logdir, len(paths), nline))
    print("跨度     : %s  ~  %s" % (first or "-", last or "-"))
    print("")
    print("---- 推送 ----")
    print("口径: channel count>=1 算本机投递成功(已 write), =0 算未投递。不是客户端 ACK。")
    print("请求总数     : %d" % len(pushes))
    print("投递成功     : %d" % len(ok))
    print("未投递       : %d" % len(miss))
    print("write 异常   : %d" % errs)
    if pushes:
        print("成功率       : %.1f%%" % (100.0 * len(ok) / len(pushes)))
    print("")
    hours = sorted(set(hour_ok) | set(hour_miss))
    if hours:
        print("小时          成功  未投递")
        for h in hours:
            print("%s  %4d  %4d" % (h, hour_ok[h], hour_miss[h]))
        print("")
    if ok:
        print("首次成功 : %s" % ok[0][0])
        print("末次成功 : %s" % ok[-1][0])
        print("最近 10 条成功:")
        for p in ok[-10:]:
            print("  %s  count=%d  company=%s  types=%s" % (p[0], p[1], p[2] or "-", p[3]))
        print("")
    if miss:
        print("未投递 %d 条 (前 10):" % len(miss))
        for p in miss[:10]:
            print("  %s  count=%d  company=%s" % (p[0], p[1], p[2] or "-"))
        print("")

    print("---- 今天重连 ----")
    print("口径: 当天第 1 次 bind 算登录, 之后每次 bind 算重连。")
    print("绑定成功     : %d" % len(binds))
    print("重连次数     : %d" % reconnects)
    print("解绑/断开    : %d" % len(unbinds))
    print("")
    if binds:
        print("绑定时间线:")
        for i, b in enumerate(binds):
            tag = "登录" if i == 0 else ("重连#%d" % i)
            print("  %s  %-6s  %s  ch=%s  company=%s  ip=%s" % (
                b[0], tag, ct_name(b[2]), b[3], b[1] or "-", b[4]))
        print("")
    if unbinds:
        print("解绑时间线:")
        for u in unbinds:
            print("  %s  %s  ch=%s  removeSuccess=%s" % (u[0], ct_name(u[1]), u[2], u[3]))
        print("")

    print("---- 结论 ----")
    print("%s 当天给用户 %s 成功推送 %d 条 (%s), 重连 %d 次。" % (
        day, user, len(ok), cmd, reconnects))
    if miss:
        print("另有 %d 条本机没有通道, 没有发出去。" % len(miss))
    if not pushes and not binds:
        print("当天日志没有这个用户。确认日期、日志目录, 以及请求是否打到这台机器。")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        sys.exit(130)
