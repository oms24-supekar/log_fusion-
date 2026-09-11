import json
import urllib.request
from datetime import datetime, timedelta

API_URL = "http://localhost:8080/api/logs/process"

LOGS = [
    ("ubuntu-server-01", "linux", "Sep 11 18:01:14 ubuntu-server sshd[12345]: Accepted password for om from 192.168.1.24 port 53218 ssh2"),
    ("ubuntu-server-01", "linux", "Sep 11 18:02:01 ubuntu-server sshd[12368]: Failed password for invalid user admin from 45.33.12.88 port 43120 ssh2"),
    ("ubuntu-server-01", "linux", "Sep 11 18:02:22 ubuntu-server sudo: om : TTY=pts/0 ; PWD=/home/om ; USER=root ; COMMAND=/usr/bin/apt update"),
    ("ubuntu-server-01", "linux", "Sep 11 18:05:11 ubuntu-server nginx[8432]: 192.168.1.22 - - \"GET /api/logs HTTP/1.1\" 200 4213"),
    ("ubuntu-server-01", "linux", "Sep 11 18:06:20 ubuntu-server kernel: Out of memory: Kill process 4921 (java) score 812 or sacrifice child"),
    ("windows-dc-01", "windows", "2026-09-11 18:10:01 Windows-Security EventID=4624 AccountName=OmSupekar LogonType=2 SourceAddress=192.168.1.15 Status=Success"),
    ("windows-dc-01", "windows", "2026-09-11 18:10:22 Windows-Security EventID=4625 AccountName=Administrator LogonType=3 SourceAddress=45.86.201.11 Status=Failed"),
    ("windows-dc-01", "windows", "2026-09-11 18:11:03 Windows-System EventID=7036 ServiceName=WindowsUpdate State=Running"),
    ("windows-dc-01", "windows", "2026-09-11 18:13:10 Windows-Defender EventID=1117 ThreatName=\"Trojan:Win32/Wacatac\" Action=Quarantined"),
    ("windows-dc-01", "windows", "2026-09-11 18:14:21 Windows-Security EventID=4720 AccountName=testuser CreatedBy=Administrator"),
    ("macbook-pro-01", "mac", "Sep 11 18:20:04 MacBook-Pro loginwindow[104]: USER_PROCESS: 501 console"),
    ("macbook-pro-01", "mac", "Sep 11 18:20:55 MacBook-Pro sudo[881]: om : TTY=ttys001 ; USER=root ; COMMAND=/usr/bin/softwareupdate -l"),
    ("macbook-pro-01", "mac", "Sep 11 18:21:20 MacBook-Pro sshd[923]: Failed password for admin from 103.55.12.98 port 44321 ssh2"),
    ("firewall-01", "network", "CEF:0|Fortinet|FortiGate|7.2|0001|Traffic Allowed|3|src=192.168.1.10 dst=8.8.8.8 spt=52114 dpt=53 proto=UDP act=allow"),
    ("firewall-01", "network", "CEF:0|Fortinet|FortiGate|7.2|0002|Traffic Blocked|7|src=45.33.10.81 dst=192.168.1.100 spt=44211 dpt=22 proto=TCP act=deny"),
    ("firewall-01", "network", "CEF:0|Palo Alto Networks|PAN-OS|11.0|THREAT|Malware Detected|9|src=172.16.1.12 dst=91.198.174.192 request=/payload.exe act=block"),
    ("firewall-01", "network", "192.168.1.11 - - [11/Sep/2026:18:40:01 +0530] \"GET / HTTP/1.1\" 200 612 \"-\" \"Mozilla/5.0\""),
    ("firewall-01", "network", "45.155.205.19 - - [11/Sep/2026:18:40:28 +0530] \"GET /.env HTTP/1.1\" 404 153 \"-\" \"curl/8.4.0\""),
    ("logfusion-api", "application", '{"timestamp":"2026-09-11T18:45:01+05:30","level":"INFO","service":"logfusion-api","message":"Log ingestion request received","source":"linux-server-01","format":"SYSLOG"}'),
    ("logfusion-api", "application", '{"timestamp":"2026-09-11T18:45:31+05:30","level":"INFO","service":"logfusion-normalizer","message":"Log normalized successfully","parser":"JsonLogParser","processingTimeMs":14}'),
    ("auth-server-01", "application", "timestamp=2026-09-11T18:50:01+05:30 level=INFO source=firewall-01 action=ALLOW src_ip=192.168.1.20 dst_ip=8.8.4.4 protocol=UDP dst_port=53"),
    ("auth-server-01", "application", "timestamp=2026-09-11T18:50:07+05:30 level=WARN source=firewall-01 action=DENY src_ip=185.44.23.12 dst_ip=192.168.1.20 protocol=TCP dst_port=22"),
    ("auth-server-01", "application", "timestamp=2026-09-11T18:50:20+05:30 level=INFO source=auth-server user=om action=LOGIN status=SUCCESS ip=192.168.1.11"),
]


def send_log(source_name, source_type, content, timestamp):
    payload = {
        "rawContent": content,
        "sourceName": source_name,
        "sourceType": source_type,
        "receivedAt": timestamp.isoformat(timespec="seconds")
    }
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req, timeout=20) as response:
        body = response.read().decode("utf-8")
        print(f"[{timestamp}] {source_name} -> {response.status}: {body[:120]}")


def main():
    now = datetime.now()
    days_back = 90
    for index, (source_name, source_type, content) in enumerate(LOGS):
        offset_days = (index % 14) + 5
        total_days = min(days_back, offset_days + (index // 3) * 7)
        timestamp = now - timedelta(days=total_days, hours=(index % 6), minutes=(index % 20))
        send_log(source_name, source_type, content, timestamp)


if __name__ == "__main__":
    main()
