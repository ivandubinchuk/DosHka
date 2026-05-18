"""
UDP Discovery Server - автовиявлення сервера в мережі
"""
import asyncio
import socket
import json
import threading
from .core.config import settings

DISCOVERY_PORT = 9999
DISCOVERY_MESSAGE = "DOSHKA_DISCOVER"
SERVER_PORT = 8000


class DiscoveryServer:
    """UDP сервер для автовиявлення в локальній мережі"""

    def __init__(self, http_port: int = SERVER_PORT):
        self.http_port = http_port
        self.running = False
        self.sock = None

    def get_local_ip(self) -> str:
        """Отримує локальну IP-адресу сервера"""
        try:
            # Створюємо тимчасове з'єднання щоб визначити IP
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except Exception:
            return "127.0.0.1"

    def start(self):
        """Запускає UDP discovery сервер у фоновому потоці"""
        if self.running:
            return

        self.running = True
        thread = threading.Thread(target=self._run_server, daemon=True)
        thread.start()
        print(f"🔍 Discovery server started on UDP port {DISCOVERY_PORT}")

    def _run_server(self):
        """Основний цикл UDP сервера"""
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        self.sock.bind(("", DISCOVERY_PORT))
        self.sock.settimeout(1.0)

        local_ip = self.get_local_ip()
        print(f"📡 Server discoverable at {local_ip}:{self.http_port}")

        while self.running:
            try:
                data, addr = self.sock.recvfrom(1024)
                message = data.decode("utf-8").strip()

                if message == DISCOVERY_MESSAGE:
                    # Відповідаємо клієнту з інформацією про сервер
                    response = json.dumps({
                        "service": "doshka",
                        "ip": local_ip,
                        "port": self.http_port,
                        "url": f"http://{local_ip}:{self.http_port}/"
                    })
                    self.sock.sendto(response.encode("utf-8"), addr)
                    print(f"✅ Discovery response sent to {addr[0]}")

            except socket.timeout:
                continue
            except Exception as e:
                if self.running:
                    print(f"Discovery error: {e}")

    def stop(self):
        """Зупиняє сервер"""
        self.running = False
        if self.sock:
            self.sock.close()


# Глобальний екземпляр
discovery_server = DiscoveryServer()
